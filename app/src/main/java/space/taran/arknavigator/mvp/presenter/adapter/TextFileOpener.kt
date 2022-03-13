package space.taran.arknavigator.mvp.presenter.adapter

import moxy.viewstate.strategy.alias.AddToEndSingle

interface TextFileOpener {
    @AddToEndSingle
    fun openTextFile(path: String)
}
