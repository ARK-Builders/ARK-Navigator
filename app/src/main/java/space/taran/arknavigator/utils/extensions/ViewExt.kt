package space.taran.arknavigator.utils.extensions

import android.view.View
import android.widget.TextView
import kotlinx.coroutines.*
import space.taran.arknavigator.R
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

fun View.changeEnabledStatus(isEnabledStatus: Boolean) {
    isEnabled = isEnabledStatus
    isClickable = isEnabledStatus
    isFocusable = isEnabledStatus
}

fun View.makeGone() {
    visibility = View.GONE
}

fun View.makeVisible() {
    visibility = View.VISIBLE
}

fun View.makeVisibleAndSetOnClickListener(action: () -> Unit) {
    setOnClickListener { action() }
    visibility = View.VISIBLE
}

fun TextView?.textOrGone(string: String?) {
    if (string.isNullOrEmpty()) this?.makeGone()
    else {
        this?.text = string
        this?.makeVisible()
    }
}

val View.autoDisposeScope: CoroutineScope
    get() {
        val exist = getTag(R.id.view_tag) as? CoroutineScope
        if (exist != null) {
            return exist
        }
        val newScope = CoroutineScope(
            SupervisorJob() +
                Dispatchers.Main +
                autoDisposeInterceptor()
        )
        setTag(R.id.view_tag, newScope)
        return newScope
    }

fun ViewAutoDisposeInterceptor(view: View): ContinuationInterceptor =
    ViewAutoDisposeInterceptorImpl(view)

/**
 * Create a ContinuationInterceptor that follows attach/detach lifecycle of [View].
 */
fun View.autoDisposeInterceptor(): ContinuationInterceptor =
    ViewAutoDisposeInterceptor(this)

private class ViewAutoDisposeInterceptorImpl(
    private val view: View
) : ContinuationInterceptor {
    override val key: CoroutineContext.Key<*>
        get() = ContinuationInterceptor

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
        val job = continuation.context[Job]
        if (job != null) {
            view.autoDispose(job)
        }
        return continuation
    }
}

fun View.autoDispose(job: Job) {
    val listener = ViewListener(this, job)
    this.addOnAttachStateChangeListener(listener)
}

private class ViewListener(
    private val view: View,
    private val job: Job
) : View.OnAttachStateChangeListener,
    CompletionHandler {
    override fun onViewDetachedFromWindow(v: View) {
        view.removeOnAttachStateChangeListener(this)
        job.cancel()
    }

    override fun onViewAttachedToWindow(v: View) {
        // do nothing
    }

    override fun invoke(cause: Throwable?) {
        view.removeOnAttachStateChangeListener(this)
        job.cancel()
    }
}
