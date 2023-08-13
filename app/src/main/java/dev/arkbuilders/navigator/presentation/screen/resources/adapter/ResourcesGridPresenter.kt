package dev.arkbuilders.navigator.presentation.screen.resources.adapter

import android.util.Log
import dev.arkbuilders.navigator.data.preferences.PreferenceKey
import dev.arkbuilders.navigator.data.preferences.Preferences
import dev.arkbuilders.navigator.data.utils.LogTags.RESOURCES_SCREEN
import dev.arkbuilders.navigator.data.utils.Sorting
import dev.arkbuilders.navigator.presentation.navigation.AppRouter
import dev.arkbuilders.navigator.presentation.navigation.Screens
import dev.arkbuilders.navigator.presentation.screen.resources.ResourcesPresenter
import dev.arkbuilders.navigator.presentation.screen.resources.ResourcesView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import space.taran.arkfilepicker.folders.RootAndFav
import space.taran.arklib.ResourceId
import space.taran.arklib.domain.index.Resource
import space.taran.arklib.domain.index.ResourceIndex
import space.taran.arklib.domain.meta.MetadataProcessor
import space.taran.arklib.domain.preview.PreviewProcessor
import space.taran.arklib.domain.score.ScoreStorage
import space.taran.arklib.domain.tags.TagStorage
import java.nio.file.Files
import javax.inject.Inject
import kotlin.io.path.notExists
import kotlin.system.measureTimeMillis

data class ResourceItem(
    val resource: Resource,
    var isSelected: Boolean = false,
    var isPinned: Boolean = false
) {
    fun id(): ResourceId = resource.id
}

class ResourcesGridPresenter(
    val rootAndFav: RootAndFav,
    val viewState: ResourcesView,
    val scope: CoroutineScope,
    val resourcesPresenter: ResourcesPresenter
) {
    @Inject
    lateinit var preferences: Preferences

    var resources = listOf<ResourceItem>()
        private set
    var selection = listOf<ResourceItem>()
        private set
    val selectedResources: List<ResourceId>
        get() = resources.filter { it.isSelected }.map { it.id() }

    private lateinit var index: ResourceIndex
    private lateinit var storage: TagStorage
    private lateinit var router: AppRouter
    private lateinit var metadataProcessor: MetadataProcessor
    private lateinit var previewProcessor: PreviewProcessor
    private lateinit var scoreStorage: ScoreStorage

    var sorting = Sorting.DEFAULT
        private set
    var ascending: Boolean = true
        private set
    var selectingEnabled: Boolean = false

    private var shortFileNames = true

    private var sortByScores = false

    fun getCount() = selection.size

    fun bindView(view: FileItemViewHolder) = runBlocking {
        val item = selection[view.position()]
        Log.d(RESOURCES_SCREEN, "binding view for resource ${item.id()}")

        val path = index.getPath(item.id())!!
        val score = scoreStorage.getScore(item.id())

        view.reset(selectingEnabled, item.isSelected)
        view.setText(path.fileName.toString(), shortFileNames)
        view.displayScore(sortByScores, score)
        Log.d(
            RESOURCES_SCREEN,
            "binding score $score for resource ${item.id()}"
        )

        if (Files.isDirectory(path)) {
            throw IllegalArgumentException("Resource can't be a directory")
        }

        if (path.notExists()) {
            scope.launch {
                resourcesPresenter.onRemovedResourceDetected()
            }
        }

        val id = item.id()
        val metadata = metadataProcessor.retrieve(id).getOrThrow()
        val preview = previewProcessor.retrieve(id).getOrThrow()

        view.setThumbnail(
            path,
            item.id(),
            metadata,
            preview,
            scope
        )
    }

    fun onItemClick(pos: Int) = scope.launch {
        val allPaths = index.allPaths()
        val containsNotExistingResource = selection.any { item ->
            allPaths[item.id()]!!.notExists()
        }

        if (containsNotExistingResource) {
            val clickedResource = selection[pos]
            resourcesPresenter.onRemovedResourceDetected()
            // selection has been updated
            if (!selection.contains(clickedResource))
                return@launch
        }

        router.navigateToFragmentUsingAdd(
            Screens.GalleryScreen(
                rootAndFav,
                selection.map { it.id() },
                pos
            )
        )
    }

    fun onSelectingChanged(enabled: Boolean) {
        selectingEnabled = enabled
        if (!selectingEnabled)
            resources.forEach { it.isSelected = false }
        viewState.onSelectingChanged(enabled)
        viewState.setSelectingEnabled(enabled)
        viewState.setSelectingCount(
            resources.filter { it.isSelected }.size,
            resources.size
        )
    }

    fun onItemSelectChanged(itemView: FileItemViewHolder) {
        val item = selection[itemView.position()]
        item.isSelected = !item.isSelected
        itemView.setSelected(item.isSelected)
        viewState.setSelectingCount(
            resources.filter { it.isSelected }.size,
            resources.size
        )
    }

    suspend fun init(
        index: ResourceIndex,
        storage: TagStorage,
        router: AppRouter,
        metadataProcessor: MetadataProcessor,
        previewProcessor: PreviewProcessor,
        scoreStorage: ScoreStorage
    ) {
        this.index = index
        this.storage = storage
        this.router = router
        this.metadataProcessor = metadataProcessor
        this.previewProcessor = previewProcessor
        this.scoreStorage = scoreStorage

        sorting = Sorting.values()[preferences.get(PreferenceKey.Sorting)]
        ascending = preferences.get(PreferenceKey.IsSortingAscending)
        shortFileNames = preferences.get(PreferenceKey.ShortFileNames)
        sortByScores = preferences.get(PreferenceKey.SortByScores)

        preferences.flow(PreferenceKey.Sorting).onEach { intSorting ->
            val newSorting = Sorting.values()[intSorting]
            if (sorting != newSorting)
                updateSorting(newSorting)
        }.launchIn(scope + Dispatchers.IO)

        preferences.flow(PreferenceKey.IsSortingAscending).onEach {
            if (ascending != it)
                updateAscending(it)
        }.launchIn(scope + Dispatchers.IO)

        preferences.flow(PreferenceKey.ShortFileNames).onEach {
            if (shortFileNames != it) {
                shortFileNames = it
                viewState.updateResourcesAdapter()
            }
        }.launchIn(scope)

        preferences.flow(PreferenceKey.SortByScores).onEach {
            if (sortByScores != it) {
                sortByScores = it
                sortAllResources()
                sortSelectionAndUpdateAdapter()
            }
        }.launchIn(scope + Dispatchers.IO)
    }

    suspend fun updateSelection(
        selection: Set<ResourceId>
    ) = withContext(Dispatchers.Main) {
        this@ResourcesGridPresenter.selection = resources
            .filter { selection.contains(it.id()) }
        setProgressVisibility(false)
        viewState.updateResourcesAdapter()
    }

    suspend fun resetResources(
        resources: Set<Resource>
    ) = withContext(Dispatchers.Default) {
        this@ResourcesGridPresenter.resources = mapNewResources(resources)
        sortAllResources()
        withContext(Dispatchers.Main) {
            setProgressVisibility(false)
        }
    }

    suspend fun shuffleResources() = withContext(Dispatchers.Default) {
        selection = selection.shuffled()
        Log.d(
            RESOURCES_SCREEN,
            "reordering resources randomly"
        )
        withContext(Dispatchers.Main) {
            viewState.updateResourcesAdapter()
        }
    }

    suspend fun unShuffleResources() = withContext(Dispatchers.Default) {
        sortAllResources()
        sortSelectionAndUpdateAdapter()
        Log.d(
            RESOURCES_SCREEN,
            "reordering resources back from random order"
        )
    }

    suspend fun increaseScore() = changeScore(1)

    suspend fun decreaseScore() = changeScore(-1)

    suspend fun resetScores() = withContext(Dispatchers.IO) {
        scoreStorage.resetScores(selectedResources)
        withContext(Dispatchers.Main) {
            sortAllResources()
            sortSelectionAndUpdateAdapter()
            onSelectingChanged(false)
        }
    }

    fun allowScoring() =
        selectedResources.isNotEmpty() && sortByScores

    fun allowResettingScores() = allowScoring() &&
        selectedResources.all {
            scoreStorage.getScore(it) > 0 || scoreStorage.getScore(it) < 0
        }

    fun onScoresChangedExternally() {
        sortAllResources()
        sortSelectionAndUpdateAdapter()
    }

    fun onSelectedChangedExternally(selected: List<ResourceId>) =
        scope.launch(Dispatchers.Default) {
            resources.forEach { item ->
                item.isSelected = item.id() in selected
            }
            withContext(Dispatchers.Main) {
                viewState.updateResourcesAdapter()
                viewState.setSelectingCount(
                    resources.filter { it.isSelected }.size,
                    resources.size
                )
            }
        }

    fun onSelectedItemLongClick(item: FileItemViewHolder) {
        router.navigateToFragmentUsingAdd(
            Screens.GalleryScreenWithSelected(
                rootAndFav,
                selection.map { it.id() },
                item.position(),
                selectedResources
            )
        )
    }

    private suspend fun changeScore(inc: Int) = withContext(Dispatchers.Default) {
        with(selectedResources) {
            if (isNotEmpty()) {
                this.forEach {
                    val score = scoreStorage.getScore(it)
                    scoreStorage.setScore(it, score + inc)
                }
            }
        }
        withContext(Dispatchers.IO) {
            scoreStorage.persist()
        }
        withContext(Dispatchers.Main) {
            sortAllResources()
            sortSelectionAndUpdateAdapter()
        }
    }

    private fun updateSorting(sorting: Sorting) {
        scope.launch(Dispatchers.Default) {
            setProgressVisibility(true, "Sorting")
            this@ResourcesGridPresenter.sorting = sorting
            sortAllResources()
            sortSelectionAndUpdateAdapter()
        }
    }

    private fun updateAscending(ascending: Boolean) {
        scope.launch(Dispatchers.Default) {
            setProgressVisibility(true, "Sorting")
            this@ResourcesGridPresenter.ascending = ascending
            sortAllResources()
            sortSelectionAndUpdateAdapter()
        }
    }

    private fun mapNewResources(
        newResources: Set<Resource>
    ): List<ResourceItem> {
        if (!selectingEnabled)
            return newResources.map { resource ->
                ResourceItem(resource)
            }

        return newResources.map { resource ->
            val selected = resources
                .find { item -> item.id() == resource.id }
                ?.isSelected ?: false

            ResourceItem(resource, selected)
        }
    }

    private fun sortAllResources() {
        val sortTime = measureTimeMillis {
            val bySorting = resourceComparator(sorting)
            val byScores = scoreComparator()

            if (bySorting == null && byScores == null) {
                return
            }

            val comparator = if (bySorting != null && byScores != null) {
                byScores.then(bySorting)
            } else if (byScores == null) {
                bySorting!!
            } else {
                byScores
            }

            resources = resources.sortedWith(comparator)

            if (sorting != Sorting.DEFAULT && !ascending) {
                resources = resources.reversed()
            }
        }
        Log.d(
            RESOURCES_SCREEN,
            "sorting by $sorting of ${resources.size} " +
                "resources took $sortTime milliseconds"
        )
    }

    private fun sortSelectionAndUpdateAdapter() {
        scope.launch(Dispatchers.Main) {
            selection = resources.filter { selection.contains(it) }
            setProgressVisibility(false)
            viewState.updateResourcesAdapter()
        }
    }

    private suspend fun setProgressVisibility(
        isVisible: Boolean,
        withText: String = ""
    ) =
        withContext(Dispatchers.Main) {
            viewState.setProgressVisibility(isVisible, withText)
        }

    private fun resourceComparator(sorting: Sorting): Comparator<ResourceItem>? =
        when (sorting) {
            Sorting.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) {
                it.resource.name
            }

            Sorting.SIZE -> compareBy { it.resource.size() }
            Sorting.TYPE -> compareBy { it.resource.extension }
            Sorting.LAST_MODIFIED -> compareBy { it.resource.modified }
            Sorting.DEFAULT -> null
        }

    private fun scoreComparator(): Comparator<ResourceItem>? =
        if (sortByScores) {
            Comparator<ResourceItem> { a, b ->
                val aScore = scoreStorage.getScore(a.id())
                val bScore = scoreStorage.getScore(b.id())
                aScore.compareTo(bScore)
            }
        } else {
            null
        }
}
