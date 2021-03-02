package ua.naiksoftware.stomp.provider

import okhttp3.*
import okio.ByteString
import ua.naiksoftware.stomp.dto.LifecycleEvent
import java.util.*

class OkHttpConnectionProvider(
        private val mUri: String,
        connectHttpHeaders: Map<String, String>?,
        okHttpClient: OkHttpClient
) : AbstractConnectionProvider() {

    private val mConnectHttpHeaders: Map<String, String> = connectHttpHeaders ?: HashMap()
    private val mOkHttpClient: OkHttpClient = okHttpClient

    private var openSocket: WebSocket? = null
    public override fun rawDisconnect() {
        if (openSocket != null) {
            openSocket!!.close(1000, "")
        }
    }

    override fun createWebSocketConnection() {
        val requestBuilder = Request.Builder()
                .url(mUri)
        addConnectionHeadersToBuilder(requestBuilder, mConnectHttpHeaders)
        openSocket = mOkHttpClient.newWebSocket(requestBuilder.build(),
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        val openEvent = LifecycleEvent(LifecycleEvent.Type.OPENED)
                        val headersAsMap = headersAsMap(response)
                        openEvent.handshakeResponseHeaders = headersAsMap
                        emitLifecycleEvent(openEvent)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        emitMessage(text)
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        emitMessage(bytes.utf8())
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        openSocket = null
                        emitLifecycleEvent(LifecycleEvent(LifecycleEvent.Type.CLOSED))
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        // in OkHttp, a Failure is equivalent to a JWS-Error *and* a JWS-Close
                        emitLifecycleEvent(LifecycleEvent(LifecycleEvent.Type.ERROR, Exception(t)))
                        openSocket = null
                        emitLifecycleEvent(LifecycleEvent(LifecycleEvent.Type.CLOSED))
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        webSocket.close(code, reason)
                    }
                }
        )
    }

    override fun rawSend(stompMessage: String?) {
        openSocket!!.send(stompMessage!!)
    }

    protected override val socket: Any?
        protected get() = openSocket

    private fun headersAsMap(response: Response): TreeMap<String, String?> {
        val headersAsMap = TreeMap<String, String?>()
        val headers = response.headers
        for (key in headers.names()) {
            headersAsMap[key] = headers[key]
        }
        return headersAsMap
    }

    private fun addConnectionHeadersToBuilder(requestBuilder: Request.Builder, mConnectHttpHeaders: Map<String, String>) {
        for ((key, value) in mConnectHttpHeaders) {
            requestBuilder.addHeader(key, value)
        }
    }

    companion object {
        const val TAG = "OkHttpConnProvider"
    }
}