package com.benjaminearley.droidbot

import android.os.Bundle
import com.benjaminearley.droidbot.Buttons.BACK
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import java.util.concurrent.TimeUnit

class DroidbotActivity : ControllerActivity() {

    private lateinit var pwmHat: AdafruitPCA9685
    private lateinit var disposables: CompositeDisposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pwmHat = AdafruitPCA9685(I2C_DEVICE_NAME)
        pwmHat.setPwmFreq(Mg360Servo.FREQUENCY_IN_HERTZ)

        disposables = CompositeDisposable()
        
        val sampler = Observable.interval(Mg360Servo.FREQUENCY_IN_MS, TimeUnit.MILLISECONDS)

        val joystickInput = getJoysticks()
            .map { (lStick, rStick) -> Pair(JoystickPosition(lStick.x, -lStick.y), -rStick.x) }

        sampler
            .withLatestFrom(joystickInput,
                BiFunction<Long, Pair<JoystickPosition, Float>, Pair<JoystickPosition, Float>> { _, joysticks -> joysticks })
            .subscribe { (lateral, yaw) ->
                getSpeeds(lateral, yaw).forEachIndexed { i, speed ->
                    pwmHat.setPwm(i.toByte(), 0, unitToPwm(speed))
                }
            }.let(disposables::add)

        getButtons()
            .subscribe({ Buttons ->
                when (Buttons) {
                    BACK -> {
                        pwmHat.softwareReset()
                        pwmHat.setPwmFreq(Mg360Servo.FREQUENCY_IN_HERTZ)
                    }
                    else -> Unit
                }
            }).let(disposables::add)
    }

    private fun unitToPwm(x: Float): Short {
        val pwm = x.clipToUnit

        return when {
            pwm > 0 -> ((Mg360Servo.MAX - Mg360Servo.MIDPOINT) * pwm + Mg360Servo.MIDPOINT).toShort()
            pwm < 0 -> (Mg360Servo.MIDPOINT + (Mg360Servo.MIDPOINT - Mg360Servo.MIN) * pwm).toShort()
            else -> Mg360Servo.MIDPOINT
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pwmHat.close()
        disposables.clear()
    }

    companion object {
        const private val TAG = "DROIDBOT"
        const private val I2C_DEVICE_NAME = "I2C1"
    }
}

object Mg360Servo {
    const val MIN: Short = 220  // Min pulse length out of 4096
    const val MIDPOINT: Short = 310  // Mid point
    const val MAX: Short = 400  // Max pulse length out of 4096
    const val FREQUENCY_IN_HERTZ: Float = 50F
    const val FREQUENCY_IN_MS: Long = (1000 / FREQUENCY_IN_HERTZ).toLong()
}

val Float.clipToUnit: Float get() = if (this < -1.0f) -1.0f else if (this > 1.0f) 1.0f else this
