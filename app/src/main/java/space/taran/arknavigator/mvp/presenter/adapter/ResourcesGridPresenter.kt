package space.taran.arknavigator.mvp.presenter.adapter

import android.util.Log
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
import space.taran.arknavigator.mvp.model.repo.preferences.PreferenceKey
import space.taran.arknavigator.mvp.model.repo.preferences.Preferences
import space.taran.arklib.domain.preview.PreviewStorage
import space.taran.arknavigator.mvp.model.repo.scores.ScoreStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.presenter.ResourcesPresenter
import space.taran.arknavigator.mvp.view.ResourcesView
import space.taran.arknavigator.mvp.view.item.FileItemView
import space.taran.arknavigator.navigation.AppRouter
import space.taran.arknavigator.navigation.Screens
import space.taran.arknavigator.utils.LogTags
import space.taran.arknavigator.utils.LogTags.RESOURCES_SCREEN
import space.taran.arknavigator.utils.Sorting
import space.taran.arknavigator.utils.reifySorting
import space.taran.arknavigator.utils.unequalCompareBy
import java.nio.file.Files
import javax.inject.Inject
import kotlin.io.path.notExists
import kotlin.system.measureTimeMillis

data class ResourceItem(
    val resource: Resource,
    var isSelected: Boolean = false,
    var isPinned: Boolean = false
)

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
    val selectedResources: List<Resource>
        get() = resources.filter { it.isSelected }.map { it.resource }

    private lateinit var index: ResourceIndex
    private lateinit var storage: TagsStorage
    private lateinit var router: AppRouter
    private lateinit var previewStorage: PreviewStorage
    private lateinit var scoreStorage: ScoreStorage

    var sorting = Sorting.DEFAULT
        private set
    var ascending: Boolean = true
        private set
    var selectingEnabled: Boolean = false

    private var shortFileNames = true

    private var sortByScores = false

    fun getCount() = selection.size

    fun bindView(view: FileItemView) = runBlocking {
        val item = selection[view.position()]
        Log.d(RESOURCES_SCREEN, "binding view for resource ${item.resource.name}")

        val path = index.getPath(item.resource.id)!!
        val score = scoreStorage.getScore(item.resource.id)

        view.reset(selectingEnabled, item.isSelected)
        view.setText(path.fileName.toString(), shortFileNames)
        view.displayScore(sortByScores, score)
        Log.d(
            RESOURCES_SCREEN,
            "binding score $score for resource ${item.resource.id}"
        )

        if (Files.isDirectory(path)) {
            throw AssertionError("Resource can't be a directory")
        }

        if (path.notExists())
            scope.launch { resourcesPresenter.onRemovedResourceDetected() }

        val preview = previewStorage
            .locate(path, item.resource)
            .onFailure {
                Log.w(LogTags.GALLERY_SCREEN, "missing thumbnail for ${item.resource.id}")
            }

        view.setIconOrPreview(
            path,
            item.resource,
            preview.getOrNull()
        )
    }

    fun onItemClick(pos: Int) = scope.launch {
        val containsNotExistingResource = selection.any { item ->
            index.getPath(item.resource.id)!!.notExists()
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
                selection.map { it.resource.id },
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

    fun onItemSelectChanged(itemView: FileItemView) {
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
        storage: TagsStorage,
        router: AppRouter,
        previewStorage: PreviewStorage,
        scoreStorage: ScoreStorage
    ) {
        this.index = index
        this.storage = storage
        this.router = router
        this.previewStorage = previewStorage
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
                viewState.updateAdapter()
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
    ) = withContext(Dispatchers.Default) {
        this@ResourcesGridPresenter.selection = resources
            .filter { selection.contains(it.resource.id) }
        withContext(Dispatchers.Main) {
            setProgressVisibility(false)
            viewState.updateAdapter()
        }
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
            viewState.updateAdapter()
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
            scoreStorage.getScore(it.id) > 0 || scoreStorage.getScore(it.id) < 0
        }

    fun onScoresChangedExternally() {
        sortAllResources()
        sortSelectionAndUpdateAdapter()
    }

    fun onSelectedChangedExternally(selected: List<ResourceId>) =
        scope.launch(Dispatchers.Default) {
            resources.forEach { item ->
                item.isSelected = item.resource.id in selected
            }
            withContext(Dispatchers.Main) {
                viewState.updateAdapter()
                viewState.setSelectingCount(
                    resources.filter { it.isSelected }.size,
                    resources.size
                )
            }
        }

    fun onSelectedItemLongClick(item: FileItemView) {
        router.navigateToFragmentUsingAdd(
            Screens.GalleryScreenWithSelected(
                rootAndFav,
                selection.map { it.resource.id },
                item.position(),
                selectedResources.map { it.id }
            )
        )
    }

    private suspend fun changeScore(inc: Int) = withContext(Dispatchers.Default) {
        with(selectedResources) {
            if (isNotEmpty()) {
                this.forEach {
                    val score = scoreStorage.getScore(it.id)
                    scoreStorage.setScore(it.id, score + inc)
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

    private fun compareResourcesByScores() = Comparator<ResourceItem> { a, b ->
        val aScore = scoreStorage.getScore(a.resource.id)
        val bScore = scoreStorage.getScore(b.resource.id)
        aScore.compareTo(bScore)
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
                .find { item -> item.resource == resource }
                ?.isSelected ?: false

            ResourceItem(resource, selected)
        }
    }

    private fun sortAllResources() {
        val sortTime = measureTimeMillis {
            val comparator = reifySorting(sorting)
            if (comparator != null) {
                resources = resources.map { it to it }
                    .toMap()
                    .toSortedMap(
                        unequalCompareBy(
                            if (sortByScores)
                                compareResourcesByScores()
                                    .then(comparator)
                            else comparator
                        )
                    )
                    .values
                    .toList()
            } else resources = resources.sortedWith(
                unequalCompareBy(
                    compareResourcesByScores()
                )
            )
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

    private fun sortSelection() {
        val selection = this.selection.toSet()
        this.selection = resources.filter { selection.contains(it) }
    }

    private fun sortSelectionAndUpdateAdapter() {
        sortSelection()
        scope.launch(Dispatchers.Main) {
            setProgressVisibility(false)
            viewState.updateAdapter()
        }
    }

    private suspend fun setProgressVisibility(
        isVisible: Boolean,
        withText: String = ""
    ) =
        withContext(Dispatchers.Main) {
            viewState.setProgressVisibility(isVisible, withText)
        }
}
