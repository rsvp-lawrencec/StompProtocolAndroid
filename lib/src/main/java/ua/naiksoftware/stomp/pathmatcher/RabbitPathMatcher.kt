package ua.naiksoftware.stomp.pathmatcher

import ua.naiksoftware.stomp.dto.StompHeader
import ua.naiksoftware.stomp.dto.StompMessage
import java.util.*

class RabbitPathMatcher : PathMatcher {
    /**
     * RMQ-style wildcards.
     * See more info [here](https://www.rabbitmq.com/tutorials/tutorial-five-java.html).
     */
    override fun matches(path: String, msg: StompMessage): Boolean {
        val dest = msg.findHeader(StompHeader.Companion.DESTINATION) ?: return false

        // for example string "lorem.ipsum.*.sit":

        // split it up into ["lorem", "ipsum", "*", "sit"]
        val split = path.split("\\.").toTypedArray()
        val transformed = ArrayList<String>()
        // check for wildcards and replace with corresponding regex
        for (s in split) {
            when (s) {
                "*" -> transformed.add("[^.]+")
                "#" ->                     // TODO: make this work with zero-word
                    // e.g. "lorem.#.dolor" should ideally match "lorem.dolor"
                    transformed.add(".*")
                else -> transformed.add(s.replace("\\*".toRegex(), ".*"))
            }
        }
        // at this point, 'transformed' looks like ["lorem", "ipsum", "[^.]+", "sit"]
        val sb = StringBuilder()
        for (s in transformed) {
            if (sb.length > 0) sb.append("\\.")
            sb.append(s)
        }
        val join = sb.toString()
        // join = "lorem\.ipsum\.[^.]+\.sit"
        return dest.matches(Regex.fromLiteral(join))
    }
}