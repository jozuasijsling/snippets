package jozua.sijsling.snippets


// [snippet comment]
// This class was a first attempt to having components survive activity recreation.
// I wrote it months before Google introduced architecture components.


/**
 * Object held by reference count that can optionally be kept even when no reference remain.
 * This is useful for instances started from an [android.app.Activity] that need to survive
 * recreation.
 */
class ReclaimableReference<T> {

    private var count = 0
    private var instance: T? = null

    @Synchronized
    fun claim(factory: () -> T): T {
        count++
        if (instance == null) {
            instance = factory()
        }
        return instance!!
    }

    @JvmOverloads
    @Synchronized
    fun release(remove: Boolean, onRemove: (T) -> Unit = {}) {
        if (--count == 0 && remove) {
            instance?.also { onRemove.invoke(it) }
            instance = null
        }
    }

    @Synchronized
    fun get(): T? = instance
}

