package com.benjaminearley.droidbot

import android.app.Activity
import android.os.Bundle
import android.view.InputDevice
import android.view.MotionEvent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

open class ControllerActivity : Activity() {

    private lateinit var joysticksSubject: PublishSubject<Joysticks>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        joysticksSubject = PublishSubject.create()
    }

    fun getJoysticks(): Observable<Joysticks> = joysticksSubject

    override fun dispatchGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event != null &&
            event.source != InputDevice.SOURCE_UNKNOWN &&
            InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE) {

            val historySize = event.historySize

            for (i in 0..historySize - 1) {
                processJoystickInput(event, i)
            }

            processJoystickInput(event)
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun processJoystickInput(event: MotionEvent,
                                     historyPos: Int? = null) {

        val mInputDevice = event.device


        joysticksSubject.onNext(
            Joysticks(
                JoystickPosition(
                    getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_X, historyPos),
                    getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_Y, historyPos)),
                JoystickPosition(
                    getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_RX, historyPos),
                    getCenteredAxis(event, mInputDevice, MotionEvent.AXIS_RY, historyPos))))
    }

    private fun getCenteredAxis(event: MotionEvent,
                                device: InputDevice, axis: Int, historyPos: Int?): Float {
        val range = device.getMotionRange(axis, event.source)

        if (range != null) {
            val flat = range.flat

            val value = historyPos?.let {
                event.getHistoricalAxisValue(axis, it)
            } ?: run {
                event.getAxisValue(axis)
            }

            if (Math.abs(value) > flat) {
                return value
            }
        }

        return 0f
    }
}

data class Joysticks(val leftStick: JoystickPosition, val rightStick: JoystickPosition)
data class JoystickPosition(val x: Float, val y: Float)
