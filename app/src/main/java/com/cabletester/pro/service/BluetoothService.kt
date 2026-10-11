package com.cabletester.pro.service

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method
import java.util.UUID
import java.util.concurrent.Executors

class BluetoothService(
    private val context: Context
) {

    companion object {

        private const val TAG = "BluetoothService"

        /*
         * Standardni Bluetooth Serial Port Profile UUID.
         */
        private val SPP_UUID: UUID =
            UUID.fromString(
                "00001101-0000-1000-8000-00805F9B34FB"
            )

        private const val RESPONSE_TIMEOUT_MS = 6000L

        /*
         * HC-05 običajno uporablja RFCOMM kanal 1.
         */
        private const val RFCOMM_CHANNEL = 1
    }

    private val bluetoothAdapter =
        context
            .getSystemService(BluetoothManager::class.java)
            ?.adapter

    private val executor =
        Executors.newSingleThreadExecutor()

    private var socket: BluetoothSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    private var connectedDevice: BluetoothDevice? = null

    private var disconnectedListener:
            (() -> Unit)? = null

    // ----------------------------------------------------
    // OSNOVNO
    // ----------------------------------------------------

    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    fun isBluetoothEnabled(): Boolean {
        return try {
            bluetoothAdapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }
    }

    fun isConnected(): Boolean {
        return try {
            socket?.isConnected == true
        } catch (_: Exception) {
            false
        }
    }

    fun getConnectedDeviceName(): String {

        return try {
            connectedDevice?.name
                ?: "Bluetooth naprava"
        } catch (_: SecurityException) {
            "Bluetooth naprava"
        }
    }

    // ----------------------------------------------------
    // BLUETOOTH PERMISSION
    // ----------------------------------------------------

    private fun hasBluetoothConnectPermission(): Boolean {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.S
        ) {
            return true
        }

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ----------------------------------------------------
    // SEZNANJENE NAPRAVE
    // ----------------------------------------------------

    fun getPairedDevices(): List<BluetoothDevice> {

        if (!hasBluetoothConnectPermission()) {
            Log.e(
                TAG,
                "BLUETOOTH_CONNECT permission ni odobren."
            )

            return emptyList()
        }

        val adapter =
            bluetoothAdapter
                ?: return emptyList()

        return try {

            adapter.bondedDevices
                ?.toList()
                ?: emptyList()

        } catch (e: SecurityException) {

            Log.e(
                TAG,
                "Napaka pri bondedDevices",
                e
            )

            emptyList()
        }
    }

    // ----------------------------------------------------
    // POIŠČI HC-05
    // ----------------------------------------------------

    fun findHC05(): BluetoothDevice? {

        return getPairedDevices()
            .firstOrNull { device ->

                try {

                    val name =
                        device.name
                            ?.uppercase()
                            ?: ""

                    name.contains("HC-05") ||
                            name.contains("HC05")

                } catch (_: SecurityException) {

                    false
                }
            }
    }

    // ----------------------------------------------------
    // DISCONNECT LISTENER
    // ----------------------------------------------------

    fun setOnDisconnectedListener(
        listener: (() -> Unit)?
    ) {
        disconnectedListener = listener
    }

    // ----------------------------------------------------
    // CONNECT
    // ----------------------------------------------------

    fun connect(
        device: BluetoothDevice,
        onConnected: () -> Unit,
        onError: (String) -> Unit
    ) {

        executor.execute {

            var connectedSocket:
                    BluetoothSocket? = null

            try {

                Log.d(
                    TAG,
                    "Začenjam povezavo..."
                )

                if (!isBluetoothSupported()) {

                    throw IOException(
                        "Bluetooth ni podprt."
                    )
                }

                if (!isBluetoothEnabled()) {

                    throw IOException(
                        "Bluetooth ni vklopljen."
                    )
                }

                if (!hasBluetoothConnectPermission()) {

                    throw SecurityException(
                        "BLUETOOTH_CONNECT dovoljenje ni odobreno."
                    )
                }


                /*
                 * Zapremo morebitno staro povezavo.
                 */
                closeSocketOnly()

                /*
                 * Če telefon izvaja discovery,
                 * lahko RFCOMM povezava pade.
                 */
                try {

                    bluetoothAdapter
                        ?.cancelDiscovery()

                } catch (e: Exception) {

                    Log.w(
                        TAG,
                        "cancelDiscovery ni uspel",
                        e
                    )
                }

                /*
                 * ------------------------------------------------
                 * POSKUS 1
                 * Standardni SPP socket
                 * ------------------------------------------------
                 */

                Log.d(
                    TAG,
                    "Poskus 1: standardni SPP"
                )

                connectedSocket =
                    tryStandardSocket(device)

                if (connectedSocket != null) {

                    Log.d(
                        TAG,
                        "STANDARDNI SPP: POVEZANO"
                    )

                } else {

                    /*
                     * ------------------------------------------------
                     * POSKUS 2
                     * Insecure SPP
                     * ------------------------------------------------
                     */

                    Log.d(
                        TAG,
                        "Poskus 2: insecure SPP"
                    )

                    connectedSocket =
                        tryInsecureSocket(device)

                    if (connectedSocket != null) {

                        Log.d(
                            TAG,
                            "INSECURE SPP: POVEZANO"
                        )

                    } else {

                        /*
                         * ------------------------------------------------
                         * POSKUS 3
                         * RFCOMM CHANNEL 1
                         *
                         * To je posebej za starejše HC-05 module.
                         * ------------------------------------------------
                         */

                        Log.d(
                            TAG,
                            "Poskus 3: RFCOMM channel $RFCOMM_CHANNEL"
                        )

                        connectedSocket =
                            tryRfcommChannel(
                                device,
                                RFCOMM_CHANNEL
                            )

                        if (connectedSocket != null) {

                            Log.d(
                                TAG,
                                "RFCOMM CHANNEL 1: POVEZANO"
                            )

                        } else {

                            throw IOException(
                                "HC-05 se ni uspel povezati.\n" +
                                        "Preizkušeni so bili:\n" +
                                        "• SPP\n" +
                                        "• insecure SPP\n" +
                                        "• RFCOMM kanal 1"
                            )
                        }
                    }
                }

                /*
                 * Shranimo uspešno povezavo.
                 */
                socket =
                    connectedSocket

                input =
                    connectedSocket.inputStream

                output =
                    connectedSocket.outputStream

                connectedDevice =
                    device

                Log.d(
                    TAG,
                    "Bluetooth povezava je uspešna."
                )

                /*
                 * Callback.
                 */
                onConnected()

            } catch (e: SecurityException) {

                try {
                    connectedSocket?.close()
                } catch (_: Exception) {
                }

                closeSocketOnly()

                Log.e(
                    TAG,
                    "Bluetooth permission napaka",
                    e
                )

                onError(
                    e.message
                        ?: "Bluetooth dovoljenje ni odobreno."
                )

            } catch (e: Exception) {

                try {
                    connectedSocket?.close()
                } catch (_: Exception) {
                }

                closeSocketOnly()

                Log.e(
                    TAG,
                    "Bluetooth povezava ni uspela",
                    e
                )

                onError(
                    e.message
                        ?: "Bluetooth povezava ni uspela."
                )
            }
        }
    }

    // ----------------------------------------------------
    // STANDARDNI SPP
    // ----------------------------------------------------

    private fun tryStandardSocket(
        device: BluetoothDevice
    ): BluetoothSocket? {

        var testSocket:
                BluetoothSocket? = null

        return try {

            testSocket =
                device.createRfcommSocketToServiceRecord(
                    SPP_UUID
                )

            testSocket.connect()

            testSocket

        } catch (e: Exception) {

            Log.w(
                TAG,
                "Standardni SPP ni uspel: ${e.message}"
            )

            try {
                testSocket?.close()
            } catch (_: Exception) {
            }

            null
        }
    }

    // ----------------------------------------------------
    // INSECURE SPP
    // ----------------------------------------------------

    private fun tryInsecureSocket(
        device: BluetoothDevice
    ): BluetoothSocket? {

        var testSocket:
                BluetoothSocket? = null

        return try {

            testSocket =
                device.createInsecureRfcommSocketToServiceRecord(
                    SPP_UUID
                )

            testSocket.connect()

            testSocket

        } catch (e: Exception) {

            Log.w(
                TAG,
                "Insecure SPP ni uspel: ${e.message}"
            )

            try {
                testSocket?.close()
            } catch (_: Exception) {
            }

            null
        }
    }

    // ----------------------------------------------------
    // RFCOMM CHANNEL
    // ----------------------------------------------------

    private fun tryRfcommChannel(
        device: BluetoothDevice,
        channel: Int
    ): BluetoothSocket? {

        var testSocket:
                BluetoothSocket? = null

        return try {

            /*
             * Starejši HC-05 moduli pogosto uporabljajo
             * RFCOMM kanal 1.
             *
             * createRfcommSocket(int) ni del običajnega
             * javnega API-ja, zato ga uporabljamo samo kot
             * fallback za združljivost s HC-05.
             */
            val method: Method =
                device.javaClass.getMethod(
                    "createRfcommSocket",
                    Int::class.javaPrimitiveType
                )

            testSocket =
                method.invoke(
                    device,
                    channel
                ) as BluetoothSocket

            testSocket.connect()

            testSocket

        } catch (e: Exception) {

            Log.w(
                TAG,
                "RFCOMM kanal $channel ni uspel: ${e.message}"
            )

            try {
                testSocket?.close()
            } catch (_: Exception) {
            }

            null
        }
    }

    // ----------------------------------------------------
    // SEND TEST
    // ----------------------------------------------------

    fun sendTest(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        executor.execute {

            try {

                val currentSocket =
                    socket
                        ?: throw IOException(
                            "Bluetooth ni povezan."
                        )

                if (!currentSocket.isConnected) {

                    throw IOException(
                        "Bluetooth socket ni povezan."
                    )
                }

                val inputStream =
                    input
                        ?: throw IOException(
                            "Bluetooth vhodni tok ni na voljo."
                        )

                val outputStream =
                    output
                        ?: throw IOException(
                            "Bluetooth izhodni tok ni na voljo."
                        )

                Log.d(
                    TAG,
                    "Čistim vhodni buffer..."
                )

                clearInputBuffer(
                    inputStream
                )

                /*
                 * Pošljemo TEST.
                 */
                Log.d(
                    TAG,
                    "Pošiljam TEST"
                )

                outputStream.write(
                    "TEST\n".toByteArray(
                        Charsets.UTF_8
                    )
                )

                outputStream.flush()

                /*
                 * Preberemo odgovor.
                 */
                var response = ""

                val deadline =
                    System.currentTimeMillis() + RESPONSE_TIMEOUT_MS

                while (System.currentTimeMillis() < deadline) {
                    val line = readLineWithTimeout(
                        inputStream,
                        deadline - System.currentTimeMillis()
                    )

                    Log.d(TAG, "Bluetooth vrstica: $line")

                    if (
                        line == "BT_OK" ||
                        line == "TEST_OK" ||
                        line == "Cable Tester Ready"
                    ) {
                        continue
                    }

                    if (isValidArduinoResponse(line)) {
                        response = line
                        break
                    }
                }

                if (response.isBlank()) {
                    throw IOException(
                        "Arduino ni vrnil veljavnega rezultata testa."
                    )
                }

                onResult(response)
                return@execute

                if (response.isBlank()) {

                    throw IOException(
                        "Arduino ni vrnil odgovora."
                    )
                }

                if (
                    !isValidArduinoResponse(
                        response
                    )
                ) {

                    throw IOException(
                        "Neveljaven odgovor Arduina: $response"
                    )
                }

                onResult(response)

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "TEST napaka",
                    e
                )

                onError(
                    e.message
                        ?: "Bluetooth TEST ni uspel."
                )

                closeSocketOnly()

                try {
                    disconnectedListener?.invoke()
                } catch (_: Exception) {
                }
            }
        }
    }

    // ----------------------------------------------------
    // READ
    // ----------------------------------------------------

    private fun readLineWithTimeout(
        inputStream: InputStream,
        timeoutMs: Long
    ): String {

        val startTime =
            System.currentTimeMillis()

        val buffer =
            StringBuilder()

        while (
            System.currentTimeMillis() -
            startTime <
            timeoutMs
        ) {

            while (
                inputStream.available() > 0
            ) {

                val value =
                    inputStream.read()

                /*
                 * Android lahko pri zaprtem RFCOMM socketu
                 * vrne -1.
                 */
                if (value == -1) {

                    throw IOException(
                        "Bluetooth povezava je bila zaprta."
                    )
                }

                val character =
                    value.toChar()

                if (character == '\n') {

                    return buffer
                        .toString()
                        .trim()
                }

                if (character != '\r') {

                    buffer.append(
                        character
                    )
                }

                /*
                 * Arduino lahko vrne:
                 * NO_CONNECTIONS
                 */
                if (
                    buffer.toString()
                        .contains(
                            "NO_CONNECTIONS",
                            ignoreCase = true
                        )
                ) {

                    return buffer
                        .toString()
                        .trim()
                }
            }

            Thread.sleep(20)
        }

        if (buffer.isNotEmpty()) {

            return buffer
                .toString()
                .trim()
        }

        throw IOException(
            "Čakanje na odgovor Arduina je poteklo."
        )
    }

    // ----------------------------------------------------
    // CLEAR INPUT
    // ----------------------------------------------------

    private fun clearInputBuffer(
        inputStream: InputStream
    ) {

        val startTime =
            System.currentTimeMillis()

        while (
            System.currentTimeMillis() -
            startTime <
            500
        ) {

            if (
                inputStream.available() > 0
            ) {

                val value =
                    inputStream.read()

                if (value == -1) {
                    break
                }

            } else {

                Thread.sleep(20)
            }
        }
    }

    // ----------------------------------------------------
    // VALIDATE ARDUINO RESPONSE
    // ----------------------------------------------------

    private fun isValidArduinoResponse(
        response: String
    ): Boolean {

        val text =
            response.trim()

        if (
            text.equals(
                "NO_CONNECTIONS",
                ignoreCase = true
            )
        ) {
            return true
        }

        val channel =
            """(?:[AB](?:[1-9]|_SHIELD))"""

        val pairRegex =
            Regex(
                """^$channel-$channel$"""
            )

        val parts =
            text
                .split(",")
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }

        if (parts.isEmpty()) {
            return false
        }

        return parts.all {
            pairRegex.matches(it)
        }
    }

    // ----------------------------------------------------
    // DISCONNECT
    // ----------------------------------------------------

    fun disconnect() {

        executor.execute {

            closeSocketOnly()

            try {
                disconnectedListener?.invoke()
            } catch (_: Exception) {
            }
        }
    }

    // ----------------------------------------------------
    // CLOSE SOCKET
    // ----------------------------------------------------

    private fun closeSocketOnly() {

        try {
            input?.close()
        } catch (_: Exception) {
        }

        try {
            output?.close()
        } catch (_: Exception) {
        }

        try {
            socket?.close()
        } catch (_: Exception) {
        }

        input = null
        output = null
        socket = null
        connectedDevice = null
    }

    // ----------------------------------------------------
    // SHUTDOWN
    // ----------------------------------------------------

    fun shutdown() {

        try {
            closeSocketOnly()
        } catch (_: Exception) {
        }

        try {
            executor.shutdownNow()
        } catch (_: Exception) {
        }

        disconnectedListener = null
    }
}