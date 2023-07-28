package dev.arkbuilders.navigator.ui.adapter.previewpager

interface PreviewPlainTextItemView {
    var pos: Int

    fun reset()
    fun setContent(text: String)
}
