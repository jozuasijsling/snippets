package jozua.sijsling.snippets

import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.OnRebindCallback
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.*


// [snippet comment]
// I have reused this group of classes and functions in various projects.
// It lets me create specific recycler adapters with a builder pattern, that are tweaked
// for use with data binding. I use this to create custom list / grid views, specific per
// content type. Using those custom views in my layouts makes their xml more descriptive.
// It lacks documentation because the builder pattern is meant to speak for itself.


fun <I, B : ViewDataBinding> bindingRecyclerAdapter(function: AdapterBuilder<I, B>.() -> Unit):
        DataBoundRecyclerAdapter<I, B> = AdapterBuilder<I, B>().apply(function).build()

fun <I, B : ViewDataBinding> pagedBindingRecyclerAdapter(function: AdapterBuilder<I, B>.() -> Unit):
        DataBoundPagedRecyclerAdapter<I, B> = AdapterBuilder<I, B>().apply(function).buildAsPagedAdapter()

class AdapterBuilder<I, B : ViewDataBinding> internal constructor() {
    private lateinit var binder: (B, I) -> Unit
    private lateinit var areItemsTheSame: (I, I) -> Boolean
    private lateinit var areContentsTheSame: (I, I) -> Boolean
    private var layouts: (I, Int) -> Int = { _, _ -> 0 }

    fun binder(function: (binding: B, item: I) -> Unit) {
        binder = function
    }

    fun layouts(function: (item: I) -> Int) = layouts { item, _ -> function(item) }
    fun layouts(function: (item: I, pos: Int) -> Int) {
        layouts = function
    }

    fun areItemsTheSame(function: (oldItem: I, newItem: I) -> Boolean) {
        areItemsTheSame = function
    }

    fun areContentsTheSame(function: (oldItem: I, newItem: I) -> Boolean) {
        areContentsTheSame = function
    }

    internal fun build() = DataBoundRecyclerAdapter(
        binder, layouts, object : DiffUtil.ItemCallback<I>() {
            override fun areItemsTheSame(oldItem: I, newItem: I): Boolean {
                return this@AdapterBuilder.areItemsTheSame(oldItem, newItem)
            }

            override fun areContentsTheSame(oldItem: I, newItem: I): Boolean {
                return this@AdapterBuilder.areContentsTheSame(oldItem, newItem)
            }
        })

    internal fun buildAsPagedAdapter() = DataBoundPagedRecyclerAdapter(
        binder, layouts, object : DiffUtil.ItemCallback<I>() {
            override fun areItemsTheSame(oldItem: I, newItem: I): Boolean {
                return this@AdapterBuilder.areItemsTheSame(oldItem, newItem)
            }

            override fun areContentsTheSame(oldItem: I, newItem: I): Boolean {
                return this@AdapterBuilder.areContentsTheSame(oldItem, newItem)
            }
        })
}

/**
 * A simple adapter designed for use with item data binding.
 *
 * @param <I> Item type
 * @param <B> Binding type
 */
class DataBoundRecyclerAdapter<I, B : ViewDataBinding>(
    private val bindItem: (binding: B, item: I) -> Unit,
    private val itemViewType: (item: I, pos: Int) -> Int,
    diffCallback: DiffUtil.ItemCallback<I>
) : RecyclerView.Adapter<DataBoundViewHolder<B>>() {

    var items: List<I>? get() = differ.currentList.toList(); set(value) = differ.submitList(value)

    private var recyclerView: RecyclerView? = null
    private val differ = AsyncListDiffer<I>(
        AdapterListUpdateCallback(this),
        differConfigOf(diffCallback)
    )

    override fun getItemViewType(pos: Int) = itemViewType.invoke(differ.currentList[pos], pos)
    override fun getItemCount(): Int = differ.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBoundViewHolder<B> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<B>(inflater, viewType, parent, false)

        // When an item view model requires an update;
        // dispatch a item change notification with a special payload
        binding.addOnRebindCallback(AwaitRecyclerViewRebindCallback<B>())

        val viewHolder = DataBoundViewHolder(binding)
        binding.lifecycleOwner = viewHolder
        return viewHolder
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    override fun onViewAttachedToWindow(holder: DataBoundViewHolder<B>) {
        holder.lifecycleRegistry.markState(Lifecycle.State.STARTED)
        holder.binding.lifecycleOwner = holder
    }

    override fun onViewDetachedFromWindow(holder: DataBoundViewHolder<B>) {
        holder.lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        holder.binding.lifecycleOwner = null
    }

    override fun onBindViewHolder(holder: DataBoundViewHolder<B>, position: Int) {
        bindItem(holder.binding, differ.currentList[position])
        holder.binding.executePendingBindings()
    }

    override fun onBindViewHolder(
        holder: DataBoundViewHolder<B>,
        position: Int,
        payloads: List<Any>
    ) {
        if (isInvalidationPayload(payloads)) {
            holder.binding.executePendingBindings()
        } else {
            onBindViewHolder(holder, position)
        }
    }

    private inner class AwaitRecyclerViewRebindCallback<T : ViewDataBinding> : OnRebindCallback<T>() {
        override fun onPreBind(binding: T) = recyclerView?.isComputingLayout ?: false

        override fun onCanceled(binding: T) {
            recyclerView?.takeUnless(RecyclerView::isComputingLayout)?.let { view ->
                val position = view.getChildAdapterPosition(binding.root)
                if (position != RecyclerView.NO_POSITION) {
                    notifyItemChanged(position, DATA_INVALIDATION_PAYLOAD)
                }
            }
        }
    }

    companion object {

        private val DATA_INVALIDATION_PAYLOAD = Any()

        private fun isInvalidationPayload(payloads: List<Any>): Boolean {
            if (payloads.isEmpty()) {
                return false
            }
            return payloads.all { it === DATA_INVALIDATION_PAYLOAD }
        }

        private fun <T> differConfigOf(diffCallback: DiffUtil.ItemCallback<T>): AsyncDifferConfig<T> =
            AsyncDifferConfig.Builder(diffCallback)
                .setBackgroundThreadExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                .build()
    }
}

/**
 * A simple adapter designed for use with item data binding.
 *
 * @param <I> Item type
 * @param <B> Binding type
 */
class DataBoundPagedRecyclerAdapter<I, B : ViewDataBinding>(
    private val bindItem: (binding: B, item: I) -> Unit,
    private val itemViewType: (item: I, pos: Int) -> Int,
    diffCallback: DiffUtil.ItemCallback<I>
) : PagedListAdapter<I, DataBoundViewHolder<B>>(differConfigOf(diffCallback)) {

    var pagedItems get() = currentList; set(value) = submitList(value)

    private var recyclerView: RecyclerView? = null

    override fun getItemViewType(pos: Int) = itemViewType(currentList!![pos]!!, pos)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBoundViewHolder<B> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<B>(inflater, viewType, parent, false)

        // When an item view model requires an update;
        // dispatch a item change notification with a special payload
        binding.addOnRebindCallback(AwaitRecyclerViewRebindCallback<B>())

        val viewHolder = DataBoundViewHolder(binding)
        binding.lifecycleOwner = viewHolder
        return viewHolder
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    override fun onViewAttachedToWindow(holder: DataBoundViewHolder<B>) {
        holder.lifecycleRegistry.markState(Lifecycle.State.STARTED)
        holder.binding.lifecycleOwner = holder
    }

    override fun onViewDetachedFromWindow(holder: DataBoundViewHolder<B>) {
        holder.lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        holder.binding.lifecycleOwner = null
    }

    override fun onBindViewHolder(holder: DataBoundViewHolder<B>, position: Int) {
        bindItem(holder.binding, getItem(position)!!)
        holder.binding.executePendingBindings()
    }

    override fun onBindViewHolder(
        holder: DataBoundViewHolder<B>,
        position: Int,
        payloads: List<Any>
    ) {
        if (isInvalidationPayload(payloads)) {
            holder.binding.executePendingBindings()
        } else {
            onBindViewHolder(holder, position)
        }
    }

    private inner class AwaitRecyclerViewRebindCallback<T : ViewDataBinding> : OnRebindCallback<T>() {
        override fun onPreBind(binding: T) = recyclerView?.isComputingLayout ?: false

        override fun onCanceled(binding: T) {
            recyclerView?.takeUnless(RecyclerView::isComputingLayout)?.let { view ->
                val position = view.getChildAdapterPosition(binding.root)
                if (position != RecyclerView.NO_POSITION) {
                    notifyItemChanged(position, DATA_INVALIDATION_PAYLOAD)
                }
            }
        }
    }

    companion object {

        private val DATA_INVALIDATION_PAYLOAD = Any()

        private fun isInvalidationPayload(payloads: List<Any>): Boolean {
            if (payloads.isEmpty()) {
                return false
            }
            return payloads.all { it === DATA_INVALIDATION_PAYLOAD }
        }

        private fun <T> differConfigOf(diffCallback: DiffUtil.ItemCallback<T>): AsyncDifferConfig<T> =
            AsyncDifferConfig.Builder(diffCallback)
                .setBackgroundThreadExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                .build()
    }
}

class DataBoundViewHolder<T : ViewDataBinding>(val binding: T) : RecyclerView.ViewHolder(binding.root),
    LifecycleOwner {
    val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle() = lifecycleRegistry
}
