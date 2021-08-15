package space.taran.arknavigator.utils

import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object CoroutineRunner {

    fun <R>runAndBlock(f: suspend () -> R): R {
        Log.w(CONCURRENT, "running coroutine, blocking main thread")

        var result: R? = null
        runBlocking {
            launch {
                Log.d(CONCURRENT, "\tperforming the job")
                result = f()
                Log.d(CONCURRENT, "\tthe job performed")
            }
        }
        Log.d(CONCURRENT, "\tcontinuing main routine")

        return result!!
    }

    fun <R>runInBackground(f: suspend () -> R): R {
        Log.i(CONCURRENT, "running coroutine, without blocking main thread")

        var result: R? = null
        runBlocking {
            GlobalScope.launch {
                Log.d(CONCURRENT, "\tperforming the job")
                result = f()
                Log.d(CONCURRENT, "\tthe job performed")
            }
        }
        Log.d(CONCURRENT, "\tcontinuing main routine")

        return result!!
    }
}