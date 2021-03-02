package ua.naiksoftware.stomp.pathmatcher

import ua.naiksoftware.stomp.dto.StompMessage

interface PathMatcher {
    fun matches(path: String, msg: StompMessage): Boolean
}