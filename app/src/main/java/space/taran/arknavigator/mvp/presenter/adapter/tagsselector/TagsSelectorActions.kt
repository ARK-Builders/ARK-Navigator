package space.taran.arknavigator.mvp.presenter.adapter.tagsselector

import space.taran.arknavigator.utils.Tag

sealed class TagsSelectorAction(val tag: Tag?)

class Include(tag: Tag): TagsSelectorAction(tag)
class Exclude(tag: Tag): TagsSelectorAction(tag)
class UncheckIncluded(tag: Tag): TagsSelectorAction(tag)
class UncheckExcluded(tag: Tag): TagsSelectorAction(tag)
class UncheckAndExclude(tag: Tag): TagsSelectorAction(tag)
class Clear(val included: Set<Tag>, val excluded: Set<Tag>): TagsSelectorAction(null)