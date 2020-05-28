package jozua.sijsling.snippets.ext

import android.content.Context
import android.content.ContextWrapper


// [snippet comments]
// Contexts can be wrapped and often are, this code lets me iteratively unwrap it while giving me
// access to Kotlin's Iterable extension APIs.

fun Context.ancestry(includeSelf: Boolean = true): Iterable<Context> = Iterable {
    object : Iterator<Context> {
        var current: Context? =
            if (includeSelf) this@ancestry else (this@ancestry as? ContextWrapper)?.baseContext

        override fun hasNext() = current != null
        override fun next(): Context {
            val temp = current
            current = (current as? ContextWrapper)?.baseContext
            return temp!!
        }
    }
}
