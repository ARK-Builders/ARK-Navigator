package space.taran.arknavigator.mvp.presenter.dialog

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.mvp.model.repo.RootAndFav
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndexRepo
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorageRepo
import space.taran.arknavigator.mvp.view.dialog.EditTagsDialogView
import space.taran.arknavigator.utils.Popularity
import space.taran.arknavigator.utils.Tag
import javax.inject.Inject

sealed class EditTagsAction {
    data class AddTag(val tag: Tag) : EditTagsAction()
    data class RemoveTag(val tag: Tag) : EditTagsAction()
}

class EditTagsDialogPresenter(
    private val rootAndFav: RootAndFav,
    private val resourceId: ResourceId,
    private val _index: ResourcesIndex?,
    private val _storage: TagsStorage?
) : MvpPresenter<EditTagsDialogView>() {

    private lateinit var index: ResourcesIndex
    private lateinit var storage: TagsStorage

    @Inject
    lateinit var indexRepo: ResourcesIndexRepo

    @Inject
    lateinit var tagsStorageRepo: TagsStorageRepo

    private var input = ""
        set(value) {
            field = value
            viewState.setInput(field)
        }
    private val actionsHistory = ArrayDeque<EditTagsAction>()
    private val resourceTags = mutableSetOf<Tag>()
    private val quickTags = mutableSetOf<Tag>()

    private var wasTextRemovedRecently = false
    private var wasTagRemovedRecently = false
    private var textRemovedRecentlyJob: Job? = null

    override fun onFirstViewAttach() {
        if (_index != null && _storage != null) {
            index = _index
            storage = _storage
            init()
        } else {
            presenterScope.launch {
                index = indexRepo.provide(rootAndFav)
                storage = tagsStorageRepo.provide(rootAndFav)
                init()
            }
        }
    }

    private fun init() {
        viewState.init()
        resourceTags += listResourceTags()
        quickTags += listQuickTags()
        viewState.setQuickTags(filterQuickTags())
        viewState.setResourceTags(resourceTags)
    }

    fun onInputChanged(newInput: String) {
        if (newInput.isNotEmpty() && input.length > newInput.length) {
            wasTextRemovedRecently = true
            textRemovedRecentlyTimer()
        }

        if (TAG_SEPARATORS.any { newInput.endsWith(it) }) {
            if (newInput.length > 1) {
                val tag = newInput.substring(0, newInput.lastIndex - 1)
                addTag(tag)
            }
            input = ""
            return
        }

        input = newInput
        viewState.setQuickTags(filterQuickTags())
    }

    fun onAddBtnClick() {
        if (input.isEmpty()) return
        addTag(input)
        input = ""
    }

    fun onResourceTagClick(tag: Tag) = removeTag(tag)

    fun onQuickTagClick(tag: Tag) {
        input = ""
        addTag(tag)
    }

    fun onBackspacePressed() {
        if (!wasTextRemovedRecently && !wasTagRemovedRecently) {
            val lastTag = resourceTags.lastOrNull() ?: return
            removeTag(lastTag)
            wasTagRemovedRecently = true
            tagWasRemovedRecentlyTimer()
        }
    }

    fun onBackClick(): Boolean {
        if (actionsHistory.isEmpty()) return false

        when (val lastAction = actionsHistory.last()) {
            is EditTagsAction.AddTag -> resourceTags -= lastAction.tag
            is EditTagsAction.RemoveTag -> resourceTags += lastAction.tag
        }
        actionsHistory.removeLast()
        updateTags()

        return true
    }

    fun onInputDone() = presenterScope.launch {
        if (input.isNotEmpty()) {
            resourceTags += input
        }
        storage.setTags(resourceId, resourceTags)
        viewState.dismissDialog()
    }

    private fun addTag(tag: Tag) {
        resourceTags += tag
        actionsHistory.addLast(EditTagsAction.AddTag(tag))

        updateTags()
    }

    private fun removeTag(tag: Tag) {
        resourceTags -= tag
        actionsHistory.addLast(EditTagsAction.RemoveTag(tag))

        updateTags()
    }

    private fun updateTags() {
        viewState.setResourceTags(resourceTags)
        viewState.setQuickTags(filterQuickTags())
    }

    private fun textRemovedRecentlyTimer() {
        textRemovedRecentlyJob?.cancel()
        textRemovedRecentlyJob = presenterScope.launch {
            delay(BACKSPACE_GAP_BETWEEN_TEXT_AND_TAG)
            ensureActive()
            wasTextRemovedRecently = false
        }
    }

    private fun tagWasRemovedRecentlyTimer() = presenterScope.launch {
        delay(BACKSPACE_GAP_BETWEEN_TAGS)
        wasTagRemovedRecently = false
    }

    private fun filterQuickTags(): Set<Tag> =
        (quickTags - resourceTags)
            .filter { tag ->
                tag.startsWith(input, true)
            }.toSet()

    private fun listQuickTags(): Set<Tag> {
        val allTags = storage.groupTagsByResources(index.listAllIds())
            .values
            .flatten()
        val popularity = Popularity.calculate(allTags)

        return allTags
            .sortedByDescending { popularity[it] }
            .toSet()
    }

    private fun listResourceTags() = storage.getTags(resourceId).toMutableSet()

    companion object {
        private val TAG_SEPARATORS = listOf(",")
        private const val BACKSPACE_GAP_BETWEEN_TEXT_AND_TAG = 1000L // ms
        private const val BACKSPACE_GAP_BETWEEN_TAGS = 500L // ms
    }
}
