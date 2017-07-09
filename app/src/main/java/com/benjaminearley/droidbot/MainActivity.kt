package com.benjaminearley.droidbot

import android.os.Bundle
import android.util.Log
import java.util.concurrent.TimeUnit

class MainActivity : ControllerActivity() {

    lateinit var PwnBoard: AdafruitPCA9685

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PwnBoard = AdafruitPCA9685(I2C_DEVICE_NAME)
        PwnBoard.setPwmFreq(50F)

        getJoysticks()
            .sample(20, TimeUnit.MILLISECONDS)
            .map { (LStick, RStick) ->
                val lStick = Vector2(LStick.x, -LStick.y)
                val lateral = if (lStick dot lStick >= deadZone2) lStick else Vector2(0.0f, 0.0f)
                val yaw = if (Math.abs(RStick.x) >= deadZone) -RStick.x else 0.0f
                Pair(lateral, yaw)
            }
            .subscribe({ (lateral, yaw) ->
                Log.e(TAG, "$lateral $yaw")
                getSpeeds(lateral, yaw).forEachIndexed { i, speed ->
                    PwnBoard.setPwm(i.toByte(), 0, unitToPwm(speed))
                }
            })
    }

    fun unitToPwm(x: Float): Short {
        val pwm = x.clipToUnit

        return if (pwm > 0) {
            ((Mg360Servo.MAX - Mg360Servo.MIDPOINT) * pwm + Mg360Servo.MIDPOINT).toShort()
        } else if (pwm < 0) {
            (Mg360Servo.MIDPOINT + (Mg360Servo.MIDPOINT - Mg360Servo.MIN) * pwm).toShort()
        } else {
            Mg360Servo.MIDPOINT
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PwnBoard.close()
    }

    companion object {
        const private val TAG = "DROIDBOT"
        const private val I2C_DEVICE_NAME = "I2C1"

        private val deadZone = 0.18f
        private val deadZone2 = deadZone * deadZone

    }
}

object Mg360Servo {
    const val MIN: Short = 205  // Min pulse length out of 4096
    const val MIDPOINT: Short = 307  // Mid point
    const val MAX: Short = 410  // Max pulse length out of 4096
}
