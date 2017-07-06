package com.benjaminearley.droidbot

import android.app.Activity
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
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
        if (event == null) return super.dispatchGenericMotionEvent(event)

        if (event.source != InputDevice.SOURCE_UNKNOWN &&
            InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE) {
            joysticksSubject.onNext(Joysticks(Pair(
                JoystickPosition(event.getAxisValue(MotionEvent.AXIS_X), event.getAxisValue(MotionEvent.AXIS_Y)),
                JoystickPosition(event.getAxisValue(MotionEvent.AXIS_Z), event.getAxisValue(MotionEvent.AXIS_RZ))
            )))
        }

        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return super.dispatchKeyEvent(event)
    }
}

data class Joysticks(val joysticks: Pair<JoystickPosition, JoystickPosition>)
data class JoystickPosition(val x: Float, val y: Float)
