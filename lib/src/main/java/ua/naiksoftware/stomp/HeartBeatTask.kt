package ua.naiksoftware.stomp

import android.util.Log
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import ua.naiksoftware.stomp.dto.StompCommand
import ua.naiksoftware.stomp.dto.StompHeader
import ua.naiksoftware.stomp.dto.StompMessage
import java.util.concurrent.TimeUnit

class HeartBeatTask(
        private val sendCallback: SendCallback,
        private val failedListener: FailedListener?
) {
    private var scheduler: Scheduler? = null
    private var serverHeartbeat = 0
    private var clientHeartbeat = 0
    private var serverHeartbeatNew = 0
    private var clientHeartbeatNew = 0

    @Transient
    private var lastServerHeartBeat: Long = 0

    @Transient
    private var clientSendHeartBeatTask: Disposable? = null

    @Transient
    private var serverCheckHeartBeatTask: Disposable? = null
    fun setServerHeartbeat(serverHeartbeat: Int) {
        serverHeartbeatNew = serverHeartbeat
    }

    fun setClientHeartbeat(clientHeartbeat: Int) {
        clientHeartbeatNew = clientHeartbeat
    }

    fun getServerHeartbeat(): Int {
        return serverHeartbeatNew
    }

    fun getClientHeartbeat(): Int {
        return clientHeartbeatNew
    }

    fun consumeHeartBeat(message: StompMessage): Boolean {
        when (message.stompCommand) {
            StompCommand.CONNECTED -> heartBeatHandshake(message.findHeader(StompHeader.Companion.HEART_BEAT))
            StompCommand.SEND -> abortClientHeartBeatSend()
            StompCommand.MESSAGE ->                 //a MESSAGE works as an hear-beat too.
                abortServerHeartBeatCheck()
            StompCommand.UNKNOWN -> if ("\n" == message.payload) {
                Log.d(TAG, "<<< PONG")
                abortServerHeartBeatCheck()
                return false
            }
        }
        return true
    }

    fun shutdown() {
        if (clientSendHeartBeatTask != null) {
            clientSendHeartBeatTask!!.dispose()
        }
        if (serverCheckHeartBeatTask != null) {
            serverCheckHeartBeatTask!!.dispose()
        }
        lastServerHeartBeat = 0
    }

    /**
     * Analise heart-beat sent from server (if any), to adjust the frequency.
     * Startup the heart-beat logic.
     */
    private fun heartBeatHandshake(heartBeatHeader: String?) {
        if (heartBeatHeader != null) {
            // The heart-beat header is OPTIONAL
            val heartbeats = heartBeatHeader.split(",").toTypedArray()
            if (clientHeartbeatNew > 0) {
                //there will be heart-beats every MAX(<cx>,<sy>) milliseconds
                clientHeartbeat = Math.max(clientHeartbeatNew, heartbeats[1].toInt())
            }
            if (serverHeartbeatNew > 0) {
                //there will be heart-beats every MAX(<cx>,<sy>) milliseconds
                serverHeartbeat = Math.max(serverHeartbeatNew, heartbeats[0].toInt())
            }
        }
        if (clientHeartbeat > 0 || serverHeartbeat > 0) {
            scheduler = Schedulers.io()
            if (clientHeartbeat > 0) {
                //client MUST/WANT send heart-beat
                Log.d(TAG, "Client will send heart-beat every $clientHeartbeat ms")
                scheduleClientHeartBeat()
            }
            if (serverHeartbeat > 0) {
                Log.d(TAG, "Client will listen to server heart-beat every $serverHeartbeat ms")
                //client WANT to listen to server heart-beat
                scheduleServerHeartBeatCheck()

                // initialize the server heartbeat
                lastServerHeartBeat = System.currentTimeMillis()
            }
        }
    }

    private fun scheduleServerHeartBeatCheck() {
        if (serverHeartbeat > 0 && scheduler != null) {
            val now = System.currentTimeMillis()
            Log.d(TAG, "Scheduling server heart-beat to be checked in $serverHeartbeat ms and now is '$now'")
            //add some slack on the check
            serverCheckHeartBeatTask = scheduler!!.scheduleDirect({ checkServerHeartBeat() }, serverHeartbeat.toLong(), TimeUnit.MILLISECONDS)
        }
    }

    private fun checkServerHeartBeat() {
        if (serverHeartbeat > 0) {
            val now = System.currentTimeMillis()
            //use a forgiving boundary as some heart beats can be delayed or lost.
            val boundary = now - 3 * serverHeartbeat
            //we need to check because the task could failed to abort
            if (lastServerHeartBeat < boundary) {
                Log.d(TAG, "It's a sad day ;( Server didn't send heart-beat on time. Last received at '$lastServerHeartBeat' and now is '$now'")
                failedListener?.onServerHeartBeatFailed()
            } else {
                Log.d(TAG, "We were checking and server sent heart-beat on time. So well-behaved :)")
                lastServerHeartBeat = System.currentTimeMillis()
            }
        }
    }

    /**
     * Used to abort the server heart-beat check.
     */
    private fun abortServerHeartBeatCheck() {
        lastServerHeartBeat = System.currentTimeMillis()
        Log.d(TAG, "Aborted last check because server sent heart-beat on time ('$lastServerHeartBeat'). So well-behaved :)")
        if (serverCheckHeartBeatTask != null) {
            serverCheckHeartBeatTask!!.dispose()
        }
        scheduleServerHeartBeatCheck()
    }

    /**
     * Schedule a client heart-beat if clientHeartbeat > 0.
     */
    private fun scheduleClientHeartBeat() {
        if (clientHeartbeat > 0 && scheduler != null) {
            Log.d(TAG, "Scheduling client heart-beat to be sent in $clientHeartbeat ms")
            clientSendHeartBeatTask = scheduler!!.scheduleDirect({ sendClientHeartBeat() }, clientHeartbeat.toLong(), TimeUnit.MILLISECONDS)
        }
    }

    /**
     * Send the raw heart-beat to the server.
     */
    private fun sendClientHeartBeat() {
        sendCallback.sendClientHeartBeat("\r\n")
        Log.d(TAG, "PING >>>")
        //schedule next client heart beat
        scheduleClientHeartBeat()
    }

    /**
     * Used when we have a scheduled heart-beat and we send a new message to the server.
     * The new message will work as an heart-beat so we can abort current one and schedule another
     */
    private fun abortClientHeartBeatSend() {
        if (clientSendHeartBeatTask != null) {
            clientSendHeartBeatTask!!.dispose()
        }
        scheduleClientHeartBeat()
    }

    interface FailedListener {
        fun onServerHeartBeatFailed()
    }

    interface SendCallback {
        fun sendClientHeartBeat(pingMessage: String)
    }

    companion object {
        private val TAG = HeartBeatTask::class.java.simpleName
    }

}