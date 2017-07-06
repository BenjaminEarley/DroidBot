package com.benjaminearley.droidbot

import android.os.SystemClock
import android.util.Log
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManagerService
import java.io.IOException
import kotlin.experimental.and
import kotlin.experimental.or

class AdafruitPCA9685(I2C_DEVICE_NAME: String) {

    private val mDevice: I2cDevice

    init {
        mDevice = PeripheralManagerService().openI2cDevice(I2C_DEVICE_NAME, PCA9685_ADDRESS)
        setAllPwm(0, 0)
        mDevice.writeRegByte(MODE2, OUTDRV.toByte())
        mDevice.writeRegByte(MODE1, ALLCALL.toByte())
        SystemClock.sleep(5) //wake up (reset sleep)
        var mode1 = mDevice.readRegByte(MODE1)
        mode1 = mode1 and SLEEP.inv().toByte()
        mDevice.writeRegByte(MODE1, mode1)
        SystemClock.sleep(5)
    }

    fun spinLeft() {
        setPwm(1, 0, SERVO_MIN)
    }

    fun spinRight() {
        setPwm(1, 0, SERVO_MAX)
    }

    fun softwareReset() {
        writeBuffer(byteArrayOf(0x06)) // SWRST
    }

    fun close() {
        try {
            mDevice.close()
        } catch (e: IOException) {
            Log.w(TAG, "Unable to close I2C device", e)
        }
    }

    fun setPwmFreq(freqHz: Float) {
        var preScaleVal = 25000000f    //25HZ
        preScaleVal /= 4096f
        preScaleVal /= freqHz
        preScaleVal -= 1.0f
        Log.i(TAG, "Setting PWM frequency to $freqHz Hz")
        Log.i(TAG, "Estimated pre-scale: $preScaleVal")
        val preScale = Math.floor(preScaleVal + 0.5).toByte()
        Log.i(TAG, "Final pre-scale: $preScale")
        val oldMode = mDevice.readRegByte(MODE1)
        val newMode = (oldMode and 0x7F) or 0x10 //sleep
        mDevice.writeRegByte(MODE1, newMode) //go to sleep
        mDevice.writeRegByte(PRESCALE, preScale)
        mDevice.writeRegByte(MODE1, oldMode)
        SystemClock.sleep(5)
        mDevice.writeRegByte(MODE1, oldMode or 0x80.toByte())
    }

    private fun setPwm(channel: Byte, on: Short, off: Short) {
        mDevice.writeRegByte(LED0_ON_L + 4 * channel, (on and 0xFF).toByte())
        mDevice.writeRegByte(LED0_ON_H + 4 * channel, (on.toInt() ushr 8).toByte())
        mDevice.writeRegByte(LED0_OFF_L + 4 * channel, (off and 0xFF).toByte())
        mDevice.writeRegByte(LED0_OFF_H + 4 * channel, (off.toInt() ushr 8).toByte())
    }

    private fun setAllPwm(on: Short, off: Short) {
        mDevice.writeRegByte(ALL_LED_ON_L, (on and 0xFF).toByte())
        mDevice.writeRegByte(ALL_LED_ON_H, (on.toInt() ushr 8).toByte())
        mDevice.writeRegByte(ALL_LED_OFF_L, (off and 0xFF).toByte())
        mDevice.writeRegByte(ALL_LED_OFF_H, (off.toInt() ushr 8).toByte())
    }

    @Throws(IOException::class)
    private fun writeBuffer(buffer: ByteArray) {
        val count = mDevice.write(buffer, buffer.size)
        Log.d(TAG, "Wrote $count bytes over I2C.")
    }

    companion object {
        val TAG = "PCA9685"

        // Registers/etc:
        val PCA9685_ADDRESS = 0x40
        val MODE1 = 0x00
        val MODE2 = 0x01
        val PRESCALE = 0xFE
        val LED0_ON_L = 0x06
        val LED0_ON_H = 0x07
        val LED0_OFF_L = 0x08
        val LED0_OFF_H = 0x09
        val ALL_LED_ON_L = 0xFA
        val ALL_LED_ON_H = 0xFB
        val ALL_LED_OFF_L = 0xFC
        val ALL_LED_OFF_H = 0xFD

        // Bits:
        val ALLCALL = 0x01
        val SLEEP = 0x10
        val OUTDRV = 0x04

        val SERVO_MIN: Short = 205  // Min pulse length out of 4096
        val SERVO_MIDPOINT: Short = 307  // Mid point
        val SERVO_MAX: Short = 410  // Max pulse length out of 4096
    }
}
