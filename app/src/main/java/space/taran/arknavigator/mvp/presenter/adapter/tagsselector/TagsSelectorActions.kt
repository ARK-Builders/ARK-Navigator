package space.taran.arknavigator.mvp.presenter.adapter.tagsselector

sealed class TagsSelectorAction(val item: TagItem?)

class Include(item: TagItem) : TagsSelectorAction(item)
class Exclude(item: TagItem?) : TagsSelectorAction(item)

class UncheckIncluded(item: TagItem?) : TagsSelectorAction(item)
class UncheckExcluded(item: TagItem?) : TagsSelectorAction(item)
class UncheckAndExclude(item: TagItem?) : TagsSelectorAction(item)

class Clear(val included: Set<TagItem>, val excluded: Set<TagItem>) :
    TagsSelectorAction(null)
