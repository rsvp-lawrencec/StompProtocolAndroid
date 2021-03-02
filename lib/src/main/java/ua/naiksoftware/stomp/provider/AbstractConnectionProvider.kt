package ua.naiksoftware.stomp.provider

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import ua.naiksoftware.stomp.dto.LifecycleEvent

/**
 * Created by forresthopkinsa on 8/8/2017.
 *
 *
 * Created because there was a lot of shared code between JWS and OkHttp connection providers.
 */
abstract class AbstractConnectionProvider : ConnectionProvider {

    private val lifecycleStream: PublishSubject<LifecycleEvent> = PublishSubject.create()
    private val messagesStream: PublishSubject<String> = PublishSubject.create()

    override fun messages(): Observable<String?> {
        return messagesStream.startWith(initSocket().toObservable())
    }

    /**
     * Simply close socket.
     *
     *
     * For example:
     * <pre>
     * webSocket.close();
    </pre> *
     */
    protected abstract fun rawDisconnect()
    override fun disconnect(): Completable {
        return Completable
                .fromAction { rawDisconnect() }
    }

    private fun initSocket(): Completable {
        return Completable
                .fromAction { createWebSocketConnection() }
    }

    /**
     * Most important method: connects to websocket and notifies program of messages.
     *
     *
     * See implementations in OkHttpConnectionProvider and WebSocketsConnectionProvider.
     */
    protected abstract fun createWebSocketConnection()

    override fun send(stompMessage: String?): Completable {
        return Completable.fromCallable {
            checkNotNull(socket) { "Not connected" }
            Log.d(TAG, "Send STOMP message: $stompMessage")
            rawSend(stompMessage)
            return@fromCallable null
        }
    }

    /**
     * Just a simple message send.
     *
     *
     * For example:
     * <pre>
     * webSocket.send(stompMessage);
    </pre> *
     *
     * @param stompMessage message to send
     */
    protected abstract fun rawSend(stompMessage: String?)

    /**
     * Get socket object.
     * Used for null checking; this object is expected to be null when the connection is not yet established.
     *
     *
     * For example:
     * <pre>
     * return webSocket;
    </pre> *
     */
    protected abstract val socket: Any?
    protected fun emitLifecycleEvent(lifecycleEvent: LifecycleEvent) {
        Log.d(TAG, "Emit lifecycle event: " + lifecycleEvent.type.name)
        lifecycleStream.onNext(lifecycleEvent)
    }

    protected fun emitMessage(stompMessage: String) {
        Log.d(TAG, "Receive STOMP message: $stompMessage")
        messagesStream.onNext(stompMessage)
    }

    override fun lifecycle(): Observable<LifecycleEvent?> {
        return lifecycleStream
    }

    companion object {
        private val TAG = AbstractConnectionProvider::class.java.simpleName
    }
}