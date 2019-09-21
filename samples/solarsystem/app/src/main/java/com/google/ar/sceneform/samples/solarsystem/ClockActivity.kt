package com.google.ar.sceneform.samples.solarsystem

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.PersistableBundle
import android.support.v4.app.FragmentActivity
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_clock.*

/**
 * author: yangbang
 * date: 2019-09-21  05:52
 * fileName: ClockActivity
 */
class ClockActivity : AppCompatActivity(), Handler.Callback {
    override fun handleMessage(msg: Message?): Boolean {
        Log.i("Tag", "${msg?.obj}")

        return true
    }

    companion object {
        var handler = Handler()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_clock)
        clock.timeCall = { times ->

            Log.i("Tag", "h: ${times[0]},m: ${times[1]},s: ${times[0]}")
        }
        print("onCreate")


    }


}