package com.benjaminearley.droidbot

import android.os.SystemClock
import android.util.Log
import com.google.android.things.pio.I2cDevice
import com.google.android.things.pio.PeripheralManagerService
import java.io.IOException
import kotlin.experimental.and
import kotlin.experimental.or

class AdafruitPCA9685(I2C_DEVICE_NAME: String) {

    private val device: I2cDevice

    init {
        device = PeripheralManagerService().openI2cDevice(I2C_DEVICE_NAME, PCA9685_ADDRESS)
        setAllPwm(0, 0)
        device.writeRegByte(MODE2, OUTDRV.toByte())
        device.writeRegByte(MODE1, ALLCALL.toByte())
        SystemClock.sleep(5) //wake up (reset sleep)
        var mode1 = device.readRegByte(MODE1)
        mode1 = mode1 and SLEEP.inv().toByte()
        device.writeRegByte(MODE1, mode1)
        SystemClock.sleep(5)
    }

    fun softwareReset() {
        writeBuffer(byteArrayOf(0x06)) // SWRST
        SystemClock.sleep(5)
    }

    fun close() {
        try {
            device.close()
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
        val oldMode = device.readRegByte(MODE1)
        val newMode = (oldMode and 0x7F) or 0x10 //sleep
        device.writeRegByte(MODE1, newMode) //go to sleep
        device.writeRegByte(PRESCALE, preScale)
        device.writeRegByte(MODE1, oldMode)
        SystemClock.sleep(5)
        device.writeRegByte(MODE1, oldMode or 0x80.toByte())
    }

    fun setPwm(channel: Byte, on: Short, off: Short) {
        device.writeRegByte(LED0_ON_L + 4 * channel, (on and 0xFF).toByte())
        device.writeRegByte(LED0_ON_H + 4 * channel, (on.toInt() ushr 8).toByte())
        device.writeRegByte(LED0_OFF_L + 4 * channel, (off and 0xFF).toByte())
        device.writeRegByte(LED0_OFF_H + 4 * channel, (off.toInt() ushr 8).toByte())
    }

    private fun setAllPwm(on: Short, off: Short) {
        device.writeRegByte(ALL_LED_ON_L, (on and 0xFF).toByte())
        device.writeRegByte(ALL_LED_ON_H, (on.toInt() ushr 8).toByte())
        device.writeRegByte(ALL_LED_OFF_L, (off and 0xFF).toByte())
        device.writeRegByte(ALL_LED_OFF_H, (off.toInt() ushr 8).toByte())
    }

    @Throws(IOException::class)
    private fun writeBuffer(buffer: ByteArray) {
        val count = device.write(buffer, buffer.size)
        Log.d(TAG, "Wrote $count bytes over I2C.")
    }

    companion object {
        const private val TAG = "PCA9685"

        // Registers/etc:
        const private val PCA9685_ADDRESS = 0x40
        const private val MODE1 = 0x00
        const private val MODE2 = 0x01
        const private val PRESCALE = 0xFE
        const private val LED0_ON_L = 0x06
        const private val LED0_ON_H = 0x07
        const private val LED0_OFF_L = 0x08
        const private val LED0_OFF_H = 0x09
        const private val ALL_LED_ON_L = 0xFA
        const private val ALL_LED_ON_H = 0xFB
        const private val ALL_LED_OFF_L = 0xFC
        const private val ALL_LED_OFF_H = 0xFD

        // Bits:
        const private val ALLCALL = 0x01
        const private val SLEEP = 0x10
        const private val OUTDRV = 0x04
    }
}
