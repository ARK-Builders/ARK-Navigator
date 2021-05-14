package space.taran.arkbrowser.utils

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object CoroutineRunner {

    fun runAndBlock(f: suspend () -> Unit) {
        Log.w(CONCURRENT, "running coroutine, blocking main thread")
        Log.d(CONCURRENT, "\t[0]")
        runBlocking {
            Log.d(CONCURRENT, "\t[1]")
            launch {
                Log.d(CONCURRENT, "\t[2]")
                f()
                Log.d(CONCURRENT, "\t[3]")
            }
            Log.d(CONCURRENT, "\t[4]")
        }
        Log.d(CONCURRENT, "\t[5]")
    }

    fun runInBackground(f: suspend () -> Unit) {
        Log.i(CONCURRENT, "running coroutine, without blocking main thread")
        Log.d(CONCURRENT, "\t[0]")
        runBlocking {
            Log.d(CONCURRENT, "\t[1]")
            GlobalScope.launch {
                Log.d(CONCURRENT, "\t[2]")
                f()
                Log.d(CONCURRENT, "\t[3]")
            }
            Log.d(CONCURRENT, "\t[4]")
        }
        Log.d(CONCURRENT, "\t[5]")
    }
}