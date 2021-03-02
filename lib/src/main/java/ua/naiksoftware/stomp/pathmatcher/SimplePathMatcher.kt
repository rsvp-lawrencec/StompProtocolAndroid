package ua.naiksoftware.stomp.pathmatcher

import ua.naiksoftware.stomp.dto.StompHeader
import ua.naiksoftware.stomp.dto.StompMessage

class SimplePathMatcher : PathMatcher {
    override fun matches(path: String, msg: StompMessage): Boolean {
        val dest = msg.findHeader(StompHeader.DESTINATION)
        return if (dest == null) false else path == dest
    }
}