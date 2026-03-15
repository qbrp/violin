package org.lain.violin

import java.util.concurrent.ConcurrentHashMap

data class ViolinContext(
    val slots: MutableMap<String, Playback> = mutableMapOf(),
)

data class Playback(val id: String, val url: String)