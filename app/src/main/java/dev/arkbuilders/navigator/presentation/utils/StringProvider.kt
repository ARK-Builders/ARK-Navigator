package dev.arkbuilders.navigator.presentation.utils

import android.content.Context
import androidx.annotation.StringRes
import dev.arkbuilders.arklib.data.meta.Kind
import dev.arkbuilders.navigator.R

class StringProvider(private val context: Context) {
    fun getString(@StringRes stringResId: Int): String {
        return context.getString(stringResId)
    }

    fun kindToString(kind: Kind) = when (kind) {
        Kind.IMAGE -> context.getString(R.string.kind_image)
        Kind.VIDEO -> context.getString(R.string.kind_video)
        Kind.DOCUMENT -> context.getString(R.string.kind_document)
        Kind.LINK -> context.getString(R.string.kind_link)
        Kind.ARCHIVE -> context.getString(R.string.kind_archive)
        Kind.PLAINTEXT -> context.getString(R.string.kind_plain_text)
        Kind.UNKNOWN -> context.getString(R.string.kind_unknown)
    }
}
