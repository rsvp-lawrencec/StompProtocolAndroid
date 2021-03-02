package ua.naiksoftware.stomp.pathmatcher

import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.StompHeader
import ua.naiksoftware.stomp.dto.StompMessage

class SubscriptionPathMatcher(private val stompClient: StompClient) : PathMatcher {
    override fun matches(path: String, msg: StompMessage): Boolean {
        // Compare subscription
        val pathSubscription = stompClient.getTopicId(path) ?: return false
        val subscription = msg.findHeader(StompHeader.SUBSCRIPTION)
        return pathSubscription == subscription
    }

}