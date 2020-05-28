package jozua.sijsling.snippets.ext

import android.os.AsyncTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher

val Dispatchers.Background by lazy { AsyncTask.THREAD_POOL_EXECUTOR.asCoroutineDispatcher() }
