package uk.fishgames.fpsserver_outgame

import java.security.MessageDigest
import java.time.Instant
import java.util.*

class FishUtil {
    companion object {
        fun hash(s: String?): String {
            val md = MessageDigest.getInstance("SHA3-512")
            md.update(s?.encodeToByteArray())
            return Base64.getEncoder().encodeToString(md.digest())
        }
        fun uuid(s:String): String {
            val str = s+Instant.now().epochSecond.toString()
            return UUID.nameUUIDFromBytes(str.toByteArray()).toString()
        }
        fun randomUUID(): String {
            return UUID.randomUUID().toString()
        }
    }
}