package space.taran.arknavigator.utils

import space.taran.arknavigator.mvp.model.repo.index.ResourceMeta

data class PartialResult<S, F>(val succeeded: S, val failed: F)

data class MetaResult(
    val meta: ResourceMeta? = null,
    val exception: Exception? = null
) {
    inline fun onSuccess(action: (meta: ResourceMeta) -> Unit): MetaResult {
        meta?.let(action)
        return this
    }

    inline fun onFailure(action: (e: Exception) -> Unit): MetaResult {
        exception?.let(action)
        return this
    }

    companion object {
        fun success(meta: ResourceMeta) = MetaResult(meta)
        fun failure(exception: Exception) = MetaResult(exception = exception)
    }
}
