package space.taran.arknavigator.mvp.presenter.dialog

import kotlinx.coroutines.launch
import moxy.MvpPresenter
import moxy.presenterScope
import space.taran.arknavigator.mvp.model.repo.index.ResourceId
import space.taran.arknavigator.mvp.model.repo.index.ResourcesIndex
import space.taran.arknavigator.mvp.model.repo.tags.TagsStorage
import space.taran.arknavigator.mvp.view.dialog.EditTagsDialogView
import space.taran.arknavigator.utils.*

class EditTagsDialogPresenter(
    private val resourceId: ResourceId,
    private val storage: TagsStorage,
    private val index: ResourcesIndex,
    private val onTagsChangedListener: (resource: ResourceId) -> Unit
): MvpPresenter<EditTagsDialogView>() {

    private var filter = ""

    override fun onFirstViewAttach() {
        viewState.init()
        viewState.setResourceTags(listResourceTags())
        viewState.setQuickTags(listQuickTags())
    }

    fun onInputChanged(input: String) {
        filter = input.split(',').last().trim()
        viewState.setQuickTags(listQuickTags())
    }

    fun onInputDone(input: String) = presenterScope.launch {
        val inputTags = Converters.tagsFromString(input)
        if (inputTags.isEmpty()) return@launch

        val newTags = listResourceTags() + inputTags
        storage.setTags(resourceId, newTags)

        viewState.clearInput()
        updateTags()
    }

    fun onResourceTagClick(tag: Tag) = presenterScope.launch {
        val newTags = listResourceTags() - tag
        storage.setTags(resourceId, newTags)

        updateTags()
    }

    fun onQuickTagClick(tag: Tag) = presenterScope.launch  {
        val newTags = listResourceTags() + tag
        storage.setTags(resourceId, newTags)

        viewState.clearInput()
        updateTags()
    }

    private fun updateTags() {
        viewState.setResourceTags(listResourceTags())
        viewState.setQuickTags(listQuickTags())
        onTagsChangedListener(resourceId)
    }

    private fun listQuickTags(): List<Tag> {
        val allTags = storage.groupTagsByResources(index.listAllIds())
            .values
            .flatten()
        val popularity = Popularity.calculate(allTags)
        val result = (popularity.keys - listResourceTags())
            .filter {
                tag -> tag.startsWith(filter, true)
            }

        return result.sortedByDescending { popularity[it] }
    }

    private fun listResourceTags() = storage.getTags(resourceId)
}