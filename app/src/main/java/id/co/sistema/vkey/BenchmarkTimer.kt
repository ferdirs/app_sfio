package id.co.sistema.vkey

import android.util.Log
import timber.log.Timber

class BenchmarkTimer(private val tag: String, private val name: String) {

    private var isEnded = false
    private lateinit var time: String
    private var startTime = 0L
    private var endTime = 0L
    fun startLog() {
        Log.d("timer", "$tag: $name: Begin timer")
        startTime = System.currentTimeMillis()
    }

    fun endLog() {
        endTime = System.currentTimeMillis()
        calculateTime()
    }

    private fun calculateTime() {
        if(!isEnded) {
            time = (endTime-startTime).toString() + " ms"
            Timber.d("$tag: $name: $time")
            isEnded = true
        }
    }

    fun getBenchmarkTime(): String =
        if(startTime!=0L || endTime!=0L) time else "Logger has not start or end yet!"

}