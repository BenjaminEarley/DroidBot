package com.benjaminearley.droidbot

import android.os.Bundle
import android.os.SystemClock

class MainActivity : ControllerActivity() {

    lateinit var PwnBoard: AdafruitPCA9685

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PwnBoard = AdafruitPCA9685(I2C_DEVICE_NAME)
        PwnBoard.setPwmFreq(50F)
        SystemClock.sleep(2000)
        while (true) {
            PwnBoard.spinLeft()
            SystemClock.sleep(2000)
            PwnBoard.spinRight()
            SystemClock.sleep(2000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PwnBoard.close()
    }

    companion object {
        val TAG = "DROIDBOT"
        val I2C_DEVICE_NAME = "I2C1"
    }

}
