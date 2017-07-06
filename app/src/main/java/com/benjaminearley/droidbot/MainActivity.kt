package com.benjaminearley.droidbot

import android.os.Bundle
import java.util.concurrent.TimeUnit

class MainActivity : ControllerActivity() {

    lateinit var PwnBoard: AdafruitPCA9685

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PwnBoard = AdafruitPCA9685(I2C_DEVICE_NAME)
        PwnBoard.setPwmFreq(50F)

        getJoysticks()
            .sample(20, TimeUnit.MILLISECONDS)
            .subscribe({ (LStick, RStick) ->
                PwnBoard.setPwm(0, 0, stickToPwm(LStick.x))
            })
    }

    fun stickToPwm(x: Float): Short {
        val x = x.clipToUnit

        return if (x > 0) {
            ((AdafruitPCA9685.SERVO_MAX - AdafruitPCA9685.SERVO_MIDPOINT) * x + AdafruitPCA9685.SERVO_MIDPOINT).toShort()
        } else if (x < 0) {
            ((AdafruitPCA9685.SERVO_MIDPOINT - AdafruitPCA9685.SERVO_MIN) * x + AdafruitPCA9685.SERVO_MIN).toShort()
        } else {
            AdafruitPCA9685.SERVO_MIDPOINT.toShort()
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
