package space.taran.arknavigator.mvp.presenter.adapter.tagsselector

sealed class TagsSelectorAction {
    data class Include(val item: TagItem) : TagsSelectorAction()
    data class Exclude(val item: TagItem) : TagsSelectorAction()

    data class UncheckIncluded(val item: TagItem) : TagsSelectorAction()
    data class UncheckExcluded(val item: TagItem) : TagsSelectorAction()
    data class UncheckAndExclude(val item: TagItem) : TagsSelectorAction()

    data class Clear(val included: Set<TagItem>, val excluded: Set<TagItem>) :
        TagsSelectorAction()
}
