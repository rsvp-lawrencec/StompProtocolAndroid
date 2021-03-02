package ua.naiksoftware.stomp

import okhttp3.OkHttpClient
import ua.naiksoftware.stomp.provider.ConnectionProvider
import ua.naiksoftware.stomp.provider.OkHttpConnectionProvider
import ua.naiksoftware.stomp.provider.WebSocketsConnectionProvider

/**
 * Supported overlays:
 * - org.java_websocket.WebSocket ('org.java-websocket:Java-WebSocket:1.3.2')
 * - okhttp3.WebSocket ('com.squareup.okhttp3:okhttp:3.8.1')
 *
 *
 * You can add own relay, just implement ConnectionProvider for you stomp transport,
 * such as web socket.
 *
 *
 * Created by naik on 05.05.16.
 */
object Stomp {
    /**
     * `webSocketClient` can accept the following type of clients:
     *
     *  * `org.java_websocket.WebSocket`: cannot accept an existing client
     *  * `okhttp3.WebSocket`: can accept a non-null instance of `okhttp3.OkHttpClient`
     *
     *
     * @param connectionProvider connectionProvider method
     * @param uri                URI to connect
     * @param connectHttpHeaders HTTP headers, will be passed with handshake query, may be null
     * @param okHttpClient       Existing client that will be used to open the WebSocket connection, may be null to use default client
     * @return StompClient for receiving and sending messages. Call #StompClient.connect
     */
    /**
     * @param connectionProvider connectionProvider method
     * @param uri                URI to connect
     * @param connectHttpHeaders HTTP headers, will be passed with handshake query, may be null
     * @return StompClient for receiving and sending messages. Call #StompClient.connect
     */
    @JvmOverloads
    fun over(
            connectionProvider: ConnectionProvider,
            uri: String, connectHttpHeaders: Map<String, String>? = null,
            okHttpClient: OkHttpClient? = null
    ): StompClient {
        if (connectionProvider == ConnectionProvider.JWS) {
            require(okHttpClient == null) { "You cannot pass an OkHttpClient when using JWS. Use null instead." }
            return createStompClient(WebSocketsConnectionProvider(uri, connectHttpHeaders))
        }
        if (connectionProvider == ConnectionProvider.OKHTTP) {
            return createStompClient(OkHttpConnectionProvider(uri, connectHttpHeaders, okHttpClient
                    ?: OkHttpClient()))
        }
        throw IllegalArgumentException("ConnectionProvider type not supported: $connectionProvider")
    }

    private fun createStompClient(connectionProvider: ua.naiksoftware.stomp.provider.ConnectionProvider): StompClient {
        return StompClient(connectionProvider)
    }

    enum class ConnectionProvider {
        OKHTTP, JWS
    }
}