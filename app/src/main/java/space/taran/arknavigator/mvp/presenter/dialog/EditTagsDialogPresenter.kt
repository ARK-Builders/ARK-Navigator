package space.taran.arknavigator.mvp.presenter.dialog

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
import space.taran.arknavigator.utils.Converters
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

    private var filter = ""
    private val actionsHistory = ArrayDeque<EditTagsAction>()
    private val resourceTags = mutableSetOf<Tag>()
    private val quickTags = mutableSetOf<Tag>()

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

    fun onInputChanged(input: String) {
        filter = input.split(',').last().trim()
        viewState.setQuickTags(filterQuickTags())
    }

    fun onResourceTagClick(tag: Tag) {
        resourceTags -= tag
        actionsHistory.addLast(EditTagsAction.RemoveTag(tag))

        updateTags()
    }

    fun onQuickTagClick(tag: Tag) {
        resourceTags += tag
        actionsHistory.addLast(EditTagsAction.AddTag(tag))

        viewState.clearInput()
        updateTags()
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

    fun onInputDone(input: String) = presenterScope.launch {
        val inputTags = Converters.tagsFromString(input)
        val newTags = resourceTags + inputTags

        storage.setTags(resourceId, newTags)
        viewState.notifyTagsChanged()
        viewState.dismissDialog()
    }

    private fun updateTags() {
        viewState.setResourceTags(resourceTags)
        viewState.setQuickTags(filterQuickTags())
    }

    private fun filterQuickTags(): Set<Tag> =
        (quickTags - resourceTags)
            .filter { tag ->
                tag.startsWith(filter, true)
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
}
