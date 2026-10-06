
package com.cabletester.pro.service

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException
import java.util.concurrent.Executors

class USBService(
    private val context: Context
) {
    @Volatile
    private var port: UsbSerialPort? = null

    @Volatile
    private var connectedDeviceId: Int? = null

    @Volatile
    private var onDisconnectedListener: (() -> Unit)? = null

    private val executor = Executors.newSingleThreadExecutor()

    private val usbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager

    companion object {
        private const val ACTION_USB_PERMISSION =
            "com.cabletester.pro.USB_PERMISSION"

        private const val BAUD_RATE = 9600
        private const val RESPONSE_TIMEOUT_MS = 6000L

        private val channel = """(?:[AB](?:[1-9]|_SHIELD))"""
        private val connectionRegex =
            Regex("""^$channel-$channel$""")
    }
    private val usbDetachReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context?,
            intent: Intent?
        ) {
            if (intent?.action != UsbManager.ACTION_USB_DEVICE_DETACHED) {
                return
            }

            val device: UsbDevice? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(
                        UsbManager.EXTRA_DEVICE,
                        UsbDevice::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }

            if (device?.deviceId == connectedDeviceId) {
                disconnect()
                onDisconnectedListener?.invoke()
            }
        }
    }
    init {
        val filter = IntentFilter(
            UsbManager.ACTION_USB_DEVICE_DETACHED
        )

        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(
                usbDetachReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(usbDetachReceiver, filter)
        }
    }



    fun setOnDisconnectedListener(listener: (() -> Unit)?) {
        onDisconnectedListener = listener
    }

    fun getConnectedUsbDevices(): List<UsbDevice> {
        return usbManager.deviceList.values.toList()
    }

    fun findArduino(): UsbDevice? {
        return UsbSerialProber
            .getDefaultProber()
            .findAllDrivers(usbManager)
            .firstOrNull()
            ?.device
    }

    fun requestUsbPermission(
        device: UsbDevice,
        onResult: (Boolean) -> Unit
    ) {
        if (usbManager.hasPermission(device)) {
            onResult(true)
            return
        }

        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION)
                .setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_MUTABLE
        )

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(
                ctx: Context?,
                intent: Intent?
            ) {
                if (intent?.action != ACTION_USB_PERMISSION) return

                try {
                    val granted = intent.getBooleanExtra(
                        UsbManager.EXTRA_PERMISSION_GRANTED,
                        false
                    )

                    context.unregisterReceiver(this)
                    onResult(granted)
                } catch (_: Exception) {
                    onResult(false)
                }
            }
        }

        val filter = IntentFilter(ACTION_USB_PERMISSION)

        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(
                receiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }

        usbManager.requestPermission(device, permissionIntent)
    }

    fun connect(
        device: UsbDevice,
        onConnected: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val driver = UsbSerialProber
                .getDefaultProber()
                .probeDevice(device)
                ?: throw IOException("USB naprava ni podprta.")

            if (!usbManager.hasPermission(device)) {
                onError(
                    "USB dovoljenje ni odobreno. Najprej zahtevaj dovoljenje."
                )
                return
            }

            val connection = usbManager.openDevice(device)
                ?: throw IOException("USB naprave ni mogoče odpreti.")

            val serialPort = driver.ports.firstOrNull()
                ?: throw IOException("USB serijska vrata niso na voljo.")

            serialPort.open(connection)

            serialPort.setParameters(
                BAUD_RATE,
                8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )

            try {
                port?.close()
            } catch (_: Exception) {
            }

            port = serialPort
            connectedDeviceId = device.deviceId

            onConnected()

        } catch (e: Exception) {
            onError(e.message ?: "USB povezava ni uspela.")
        }
    }

    fun sendTest(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val serialPort = port

        if (serialPort == null) {
            onError("Arduino ni povezan.")
            return
        }

        executor.execute {
            try {
                // Arduino se lahko po odprtju USB povezave ponovno zažene.
                Thread.sleep(1800)

                var validResult: String? = null

                for (attempt in 0..1) {
                    clearInputBuffer(serialPort)

                    serialPort.write(
                        "TEST\n".toByteArray(Charsets.US_ASCII),
                        2000
                    )

                    val response = readResponse(serialPort)
                    validResult = findValidResponse(response)

                    if (validResult != null) {
                        break
                    }

                    if (attempt == 0) {
                        Thread.sleep(300)
                    }
                }

                if (validResult != null) {
                    onResult(validResult)
                } else {
                    onError(
                        "Arduino ni vrnil veljavnega odgovora. " +
                                "Preveri povezavo in poskusi znova."
                    )
                }

            } catch (e: Exception) {
                onError(e.message ?: "Napaka pri USB testu.")
            }
        }
    }

    private fun clearInputBuffer(serialPort: UsbSerialPort) {
        val buffer = ByteArray(256)
        val start = System.currentTimeMillis()
        var lastData = start

        while (
            System.currentTimeMillis() - start < 700 &&
            System.currentTimeMillis() - lastData < 200
        ) {
            val count = serialPort.read(buffer, 100)

            if (count > 0) {
                lastData = System.currentTimeMillis()
            }
        }
    }

    private fun readResponse(serialPort: UsbSerialPort): String {
        val buffer = ByteArray(256)
        val response = StringBuilder()
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < RESPONSE_TIMEOUT_MS) {
            val count = serialPort.read(buffer, 300)

            if (count > 0) {
                response.append(
                    String(buffer, 0, count, Charsets.US_ASCII)
                )

                if (response.contains("\n")) {
                    break
                }
            }
        }

        return response.toString()
    }

    private fun findValidResponse(raw: String): String? {
        val lines = raw
            .split("\n", "\r")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        for (line in lines) {
            if (line == "NO_CONNECTIONS") {
                return line
            }

            val connections = line.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (
                connections.isNotEmpty() &&
                connections.all { connectionRegex.matches(it) }
            ) {
                return connections.distinct().joinToString(",")
            }
        }

        return null
    }

    fun disconnect() {
        try {
            port?.close()
        } catch (_: Exception) {
        }

        port = null
        connectedDeviceId = null
    }
}