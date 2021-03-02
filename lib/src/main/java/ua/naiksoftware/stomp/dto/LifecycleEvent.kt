package ua.naiksoftware.stomp.dto

import java.util.*

/**
 * Created by naik on 05.05.16.
 */
class LifecycleEvent {
    enum class Type {
        OPENED, CLOSED, ERROR, FAILED_SERVER_HEARTBEAT
    }

    val type: Type

    //Nullable
    var exception: Exception? = null
        private set

    //Nullable
    var message: String? = null
        private set
    var handshakeResponseHeaders: TreeMap<String, String?>? = TreeMap()

    constructor(type: Type) {
        this.type = type
    }

    constructor(type: Type, exception: Exception?) {
        this.type = type
        this.exception = exception
    }

    constructor(type: Type, message: String?) {
        this.type = type
        this.message = message
    }

}