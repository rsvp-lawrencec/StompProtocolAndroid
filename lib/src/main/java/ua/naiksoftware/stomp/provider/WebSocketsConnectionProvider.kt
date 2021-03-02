package ua.naiksoftware.stomp.provider

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.exceptions.InvalidDataException
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.handshake.ServerHandshake
import ua.naiksoftware.stomp.dto.LifecycleEvent
import java.net.URI
import java.util.*
import javax.net.ssl.SSLContext
import kotlin.Throws

/**
 * Created by naik on 05.05.16.
 */
class WebSocketsConnectionProvider(
        private val mUri: String,
        connectHttpHeaders: Map<String, String>?
) : AbstractConnectionProvider() {

    private val mConnectHttpHeaders: Map<String, String>
    private var mWebSocketClient: WebSocketClient? = null
    private var haveConnection = false
    private var mServerHandshakeHeaders: TreeMap<String, String?>? = null
    public override fun rawDisconnect() {
        try {
            mWebSocketClient!!.closeBlocking()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Thread interrupted while waiting for Websocket closing: ", e)
            throw RuntimeException(e)
        }
    }

    override fun createWebSocketConnection() {
        check(!haveConnection) { "Already have connection to web socket" }
        mWebSocketClient = object : WebSocketClient(URI.create(mUri), Draft_6455(), mConnectHttpHeaders, 0) {
            @Throws(InvalidDataException::class)
            override fun onWebsocketHandshakeReceivedAsClient(conn: WebSocket, request: ClientHandshake, response: ServerHandshake) {
                Log.d(TAG, "onWebsocketHandshakeReceivedAsClient with response: " + response.httpStatus + " " + response.httpStatusMessage)
                mServerHandshakeHeaders = TreeMap()
                val keys = response.iterateHttpFields()
                while (keys.hasNext()) {
                    val key = keys.next()
                    mServerHandshakeHeaders!![key] = response.getFieldValue(key)
                }
            }

            override fun onOpen(handshakeData: ServerHandshake) {
                Log.d(TAG, "onOpen with handshakeData: " + handshakeData.httpStatus + " " + handshakeData.httpStatusMessage)
                val openEvent = LifecycleEvent(LifecycleEvent.Type.OPENED)
                openEvent.handshakeResponseHeaders = mServerHandshakeHeaders
                emitLifecycleEvent(openEvent)
            }

            override fun onMessage(message: String) {
                Log.d(TAG, "onMessage: $message")
                emitMessage(message)
            }

            override fun onClose(code: Int, reason: String, remote: Boolean) {
                Log.d(TAG, "onClose: code=$code reason=$reason remote=$remote")
                haveConnection = false
                emitLifecycleEvent(LifecycleEvent(LifecycleEvent.Type.CLOSED))
                Log.d(TAG, "Disconnect after close.")
                disconnect()
            }

            override fun onError(ex: Exception) {
                Log.e(TAG, "onError", ex)
                emitLifecycleEvent(LifecycleEvent(LifecycleEvent.Type.ERROR, ex))
            }
        }
        if (mUri.startsWith("wss")) {
            try {
                val sc = SSLContext.getInstance("TLS")
                sc.init(null, null, null)
                val factory = sc.socketFactory
                mWebSocketClient?.setSocketFactory(factory)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mWebSocketClient?.connect()
        haveConnection = true
    }

    override fun rawSend(stompMessage: String?) {
        mWebSocketClient!!.send(stompMessage)
    }

    protected override val socket: Any?
        protected get() = mWebSocketClient

    companion object {
        private val TAG = WebSocketsConnectionProvider::class.java.simpleName
    }

    /**
     * Support UIR scheme ws://host:port/path
     *
     * @param connectHttpHeaders may be null
     */
    init {
        mConnectHttpHeaders = connectHttpHeaders ?: HashMap()
    }
}