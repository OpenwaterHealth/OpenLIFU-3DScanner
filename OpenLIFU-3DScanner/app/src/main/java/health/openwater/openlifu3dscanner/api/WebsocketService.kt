package health.openwater.openlifu3dscanner.api

import android.util.Log
import health.openwater.openlifu3dscanner.api.model.ReconstructionProgress
import health.openwater.openlifu3dscanner.di.ApiModule
import io.socket.client.IO
import io.socket.client.Manager
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.URI

class WebsocketService(
    private val authService: AuthService,
    private val scope: CoroutineScope
) {
    private val sockets = mutableMapOf<Long, Socket>()

    fun connect(photoscanId: Long, progressFlow: MutableStateFlow<ReconstructionProgress?>) {
        Log.d(TAG, "Connecting $photoscanId")

        val token = runBlocking {
            mapOf("token" to "Bearer ${authService.getToken()}")
        }
        val opts = Manager.Options().apply {
            transports = arrayOf("websocket")
            reconnection = true
            auth = token
        }

        val manager = Manager(URI(ApiModule.API_URL), opts)

        val socket = manager.socket("/progress", IO.Options().apply {
            auth = token
        })

        sockets[photoscanId] = socket

        socket.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Connected: $photoscanId")

            val roomData = JSONObject().apply {
                put("photoscan_id", photoscanId)  // your room name
            }
            socket.emit("subscribe", roomData)
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.d(TAG, "Connect error $photoscanId: ${args[0]}")
        }

        socket.on(Socket.EVENT_DISCONNECT) { args ->
            Log.d(TAG, "Disconnected: $photoscanId")
        }

        socket.on("progress") { args ->
            val data = args[0]
            Log.d(TAG, "Progress: ${data}")
            try {
                val json = JSONObject(data.toString())
                val id = json.getLong("photoscan_id")
                val progress = json.getInt("progress")
                val message = json.getString("message")
                val status = json.getString("status")

                if (id == photoscanId) {
                    scope.launch {
                        progressFlow.emit(ReconstructionProgress(progress, message, status))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        socket.connect()
    }

    fun disconnect(photoscanId: Long) {
        Log.d(TAG, "Disconnecting $photoscanId")
        sockets.remove(photoscanId)?.disconnect()
    }

    companion object {
        private val TAG = WebsocketService::class.simpleName
    }
}