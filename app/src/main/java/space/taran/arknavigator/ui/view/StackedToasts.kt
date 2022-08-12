package space.taran.arknavigator.ui.view

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.binding.AbstractBindingItem
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.taran.arknavigator.R
import space.taran.arknavigator.databinding.ItemToastBinding
import space.taran.arknavigator.utils.tickerFlow
import space.taran.arknavigator.utils.tryUnlock
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class StackedToasts(
    val rv: RecyclerView,
    val lifecycleScope: CoroutineScope
) {
    private val adapter = ItemAdapter<ToastItem>()
    private val toasts = mutableListOf<ToastItem>()
    private val toastsFlow = MutableSharedFlow<Path>()
    private val adapterUpdateManageMutex = Mutex()
    private val itemOverflowMutex = Mutex()
    private var toastsJob = collectToasts()
    private var adapterUpdateJob = collectAdapterUpdate()

    init {
        val fastAdapter = FastAdapter.with(adapter)
        rv.layoutManager = LinearLayoutManager(rv.context).apply {
            reverseLayout = false
            stackFromEnd = true
        }
        rv.adapter = fastAdapter
    }

    fun toast(failedPath: Path) = lifecycleScope.launch {
        toastsFlow.emit(failedPath)
    }

    fun clearToasts() {
        toastsJob.cancel()
        toasts.clear()
        FastAdapterDiffUtil[adapter] = toasts
        toastsJob = collectToasts()
    }

    private fun collectToasts() = toastsFlow.onEach { path ->
        adapterUpdateManageMutex.withLock {
            val item = ToastItem(rv.context, path)
            if (toasts.size == MAX_ITEMS)
                itemOverflowMutex.withLock {}
            if (adapterUpdateJob.isCancelled)
                adapterUpdateJob = collectAdapterUpdate()
            toasts.add(item)
            if (toasts.size == MAX_ITEMS)
                itemOverflowMutex.lock()
            launchItemTimer(item)
        }
    }.launchIn(lifecycleScope)

    private fun launchItemTimer(item: ToastItem) = lifecycleScope.launch {
        delay(SHOW_TIME)
        toasts.remove(item)
        itemOverflowMutex.tryUnlock()
    }

    private fun collectAdapterUpdate() = tickerFlow(TICK).onEach {
        if (toasts.isEmpty()) {
            adapterUpdateManageMutex.withLock {
                if (toasts.isEmpty())
                    currentCoroutineContext().cancel()
            }
        }
        FastAdapterDiffUtil[adapter] = toasts
    }.launchIn(lifecycleScope)

    companion object {
        private const val TICK = 450L
        private const val MAX_ITEMS = 3
        private const val SHOW_TIME = 3_000L
    }
}

private class ToastItem(
    val context: Context,
    val path: Path
) : AbstractBindingItem<ItemToastBinding>() {
    override var identifier: Long
        get() = path.hashCode().toLong()
        set(value) {}
    override val type = R.id.fastadapter_item

    override fun createBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?
    ) = ItemToastBinding.inflate(inflater, parent, false)

    override fun bindView(binding: ItemToastBinding, payloads: List<Any>) {
        binding.root.isEnabled = false
        binding.tvMessage.text = context.getString(
            R.string.toast_could_not_detect_kind_for,
            path.absolutePathString()
        )
    }
}
