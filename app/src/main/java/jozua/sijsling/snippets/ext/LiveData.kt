package jozua.sijsling.snippets.ext

import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

// [snippet comments]
// Simple helper functions. These functions should have been available on the LiveData APIs

fun <T, R> LiveData<T>.map(transform: (T) -> R): LiveData<R> =
  Transformations.map(this, transform)

fun <T> MutableLiveData<T>.assign(value: T) {
  if (Looper.getMainLooper().thread === Thread.currentThread()) {
    this.value = value
  } else {
    this.postValue(value)
  }
}
