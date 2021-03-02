package ua.naiksoftware.stomp.dto

import java.io.StringReader
import java.util.*
import java.util.regex.Pattern

/**
 * Created by naik on 05.05.16.
 */
class StompMessage(
        val stompCommand: String?,
        val stompHeaders: List<StompHeader>?,
        val payload: String?
) {

    fun findHeader(key: String): String? {
        if (stompHeaders == null) return null
        for (header in stompHeaders) {
            if (header.key == key) return header.value
        }
        return null
    }

    fun compile(): String {
        return compile(false)
    }

    fun compile(legacyWhitespace: Boolean): String {
        val builder = StringBuilder()
        builder.append(stompCommand).append('\n')
        for (header in stompHeaders!!) {
            builder.append(header.key).append(':').append(header.value).append('\n')
        }
        builder.append('\n')
        if (payload != null) {
            builder.append(payload)
            if (legacyWhitespace) builder.append("\n\n")
        }
        builder.append(TERMINATE_MESSAGE_SYMBOL)
        return builder.toString()
    }

    override fun toString(): String {
        return "StompMessage{" +
                "command='" + stompCommand + '\'' +
                ", headers=" + stompHeaders +
                ", payload='" + payload + '\'' +
                '}'
    }

    companion object {
        const val TERMINATE_MESSAGE_SYMBOL = "\u0000"
        private val PATTERN_HEADER = Pattern.compile("([^:\\s]+)\\s*:\\s*([^:\\s]+)")
        fun from(data: String?): StompMessage {
            if (data == null || data.trim { it <= ' ' }.isEmpty()) {
                return StompMessage(StompCommand.UNKNOWN, null, data)
            }
            val reader = Scanner(StringReader(data))
            reader.useDelimiter("\\n")
            val command = reader.next()
            val headers: MutableList<StompHeader> = ArrayList()
            while (reader.hasNext(PATTERN_HEADER)) {
                val matcher = PATTERN_HEADER.matcher(reader.next())
                matcher.find()
                headers.add(StompHeader(matcher.group(1), matcher.group(2)))
            }
            reader.skip("\n\n")
            reader.useDelimiter(TERMINATE_MESSAGE_SYMBOL)
            val payload = if (reader.hasNext()) reader.next() else null
            return StompMessage(command, headers, payload)
        }
    }

}