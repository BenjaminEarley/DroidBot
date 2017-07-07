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
            // sample to Pwn Frequency
            .sample(20, TimeUnit.MILLISECONDS)
            // convert dead zone
            .map { (LStick, RStick) ->
                val lStick = Vector2(LStick.x, -LStick.y)
                val lateral = if (lStick dot lStick >= deadZone2) lStick else Vector2(0.0f, 0.0f)
                val yaw = if (Math.abs(RStick.x) >= deadZone) -RStick.x else 0.0f
                Pair(lateral, yaw)
            }
            .subscribe({ (lateral, yaw) ->
                getSpeeds(lateral, yaw).forEachIndexed { i, speed ->
                    PwnBoard.setPwm(i.toByte(), 0, unitToPwm(speed))
                }
            })
    }

    fun unitToPwm(x: Float): Short {
        val pwm = x.clipToUnit

        return if (pwm > 0) {
            ((Servo.MAX - Servo.MIDPOINT) * pwm + Servo.MIDPOINT).toShort()
        } else if (pwm < 0) {
            (Servo.MIDPOINT + (Servo.MIDPOINT - Servo.MIN) * pwm).toShort()
        } else {
            Servo.MIDPOINT
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PwnBoard.close()
    }

    companion object {
        const private val TAG = "DROIDBOT"
        const private val I2C_DEVICE_NAME = "I2C1"

        private val deadZone = 0.2f
        private val deadZone2 = deadZone * deadZone
    }
}
