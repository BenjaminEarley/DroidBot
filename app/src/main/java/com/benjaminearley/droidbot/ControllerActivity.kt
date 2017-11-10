package com.benjaminearley.droidbot

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

@SuppressLint("Registered")
open class ControllerActivity : Activity() {

    private lateinit var joysticksSubject: PublishSubject<Joysticks>
    private lateinit var buttonsSubject: PublishSubject<Buttons>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        joysticksSubject = PublishSubject.create()
        buttonsSubject = PublishSubject.create()
    }

    fun getJoysticks(): Observable<Joysticks> = joysticksSubject
        .map { (l, r) ->
            Joysticks(l.removeDeadZone.clipToUnit, r.removeDeadZone.clipToUnit)
        }
        .distinctUntilChanged()

    fun getButtons(): Observable<Buttons> = buttonsSubject

    override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event != null &&
            event.source != InputDevice.SOURCE_UNKNOWN &&
            (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE) {

            val historySize = event.historySize

            for (i in 0 until historySize) {
                processJoystickInput(event, i)
            }

            processJoystickInput(event)
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun processJoystickInput(event: MotionEvent, historyPos: Int? = null) =
        joysticksSubject.onNext(
            Joysticks(
                JoystickPosition(
                    getAxisValue(event, MotionEvent.AXIS_X, historyPos),
                    getAxisValue(event, MotionEvent.AXIS_Y, historyPos)),
                JoystickPosition(
                    getAxisValue(event, MotionEvent.AXIS_RX, historyPos),
                    getAxisValue(event, MotionEvent.AXIS_RY, historyPos))))


    private fun getAxisValue(event: MotionEvent, axis: Int, historyPos: Int?): Float =
        historyPos?.let {
            event.getHistoricalAxisValue(axis, it)
        } ?: run {
            event.getAxisValue(axis)
        }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.repeatCount == 0 && event.action == KeyEvent.ACTION_DOWN) {
            return when (event.keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    buttonsSubject.onNext(Buttons.BACK)
                    true
                }
                else -> false

            }
        }
        return super.dispatchKeyEvent(event)
    }
}

typealias JoystickPosition = Vector2

data class Joysticks(val leftStick: JoystickPosition, val rightStick: JoystickPosition)

val JoystickPosition.removeDeadZone: JoystickPosition
    get() {
        val squaredLength = this dot this

        return if (squaredLength >= deadZoneRadiusSquared) {
            (this.scale(-deadZoneRadius / squaredLength.sqrt) + this).scale(deadZoneRadiusScale)
        } else {
            JoystickPosition(0.0f, 0.0f)
        }
    }

private val deadZoneRadius = 0.2f
private val deadZoneRadiusSquared = deadZoneRadius * deadZoneRadius
private val deadZoneRadiusScale = 1.0f / (1.0f - deadZoneRadius)

enum class Buttons {
    X, Y, A, B, LB, RB, BACK, START
}

val Float.sqrt: Float get() = Math.sqrt(toDouble()).toFloat()
