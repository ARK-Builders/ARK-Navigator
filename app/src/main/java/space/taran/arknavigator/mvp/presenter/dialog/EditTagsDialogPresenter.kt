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
import space.taran.arknavigator.utils.*
import javax.inject.Inject

class EditTagsDialogPresenter(
    private val rootAndFav: RootAndFav,
    private val resourceId: ResourceId,
    private val _index: ResourcesIndex?,
    private val _storage: TagsStorage?
) : MvpPresenter<EditTagsDialogView>() {

    private var filter = ""
    private lateinit var index: ResourcesIndex
    private lateinit var storage: TagsStorage

    @Inject
    lateinit var indexRepo: ResourcesIndexRepo

    @Inject
    lateinit var tagsStorageRepo: TagsStorageRepo

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
        viewState.setQuickTags(listQuickTags())
        viewState.setResourceTags(listResourceTags())
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

    fun onQuickTagClick(tag: Tag) = presenterScope.launch {
        val newTags = listResourceTags() + tag
        storage.setTags(resourceId, newTags)

        viewState.clearInput()
        updateTags()
    }

    private fun updateTags() {
        viewState.setResourceTags(listResourceTags())
        viewState.setQuickTags(listQuickTags())
        viewState.notifyTagsChanged()
    }

    private fun listQuickTags(): List<Tag> {
        val allTags = storage.groupTagsByResources(index.listAllIds())
            .values
            .flatten()
        val popularity = Popularity.calculate(allTags)
        val result = (popularity.keys - listResourceTags())
            .filter { tag ->
                tag.startsWith(filter, true)
            }

        return result.sortedByDescending { popularity[it] }
    }

    private fun listResourceTags() = storage.getTags(resourceId)
}