package space.taran.arkbrowser.utils

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object CoroutineRunner {

    fun <R>runAndBlock(f: suspend () -> R): R {
        Log.w(CONCURRENT, "running coroutine, blocking main thread")

        var result: R? = null
        runBlocking {
            Log.d(CONCURRENT, "\t[1]")
            launch {
                Log.d(CONCURRENT, "\t[2]")
                result = f()
                Log.d(CONCURRENT, "\t[3]")
            }
            Log.d(CONCURRENT, "\t[4]")
        }
        Log.d(CONCURRENT, "\t[5]")

        return result!!
    }

    fun <R>runInBackground(f: suspend () -> R): R {
        Log.i(CONCURRENT, "running coroutine, without blocking main thread")

        var result: R? = null
        runBlocking {
            Log.d(CONCURRENT, "\t[1]")
            GlobalScope.launch {
                Log.d(CONCURRENT, "\t[2]")
                result = f()
                Log.d(CONCURRENT, "\t[3]")
            }
            Log.d(CONCURRENT, "\t[4]")
        }
        Log.d(CONCURRENT, "\t[5]")

        return result!!
    }
}