package org.lain.violin.client

import de.keksuccino.melody.resources.audio.openal.ALAudioClip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

const val TIMEOUT_SECONDS = 10L

suspend fun getOggWebAudioClip(
    audio: CompletableFuture<ALAudioClip>,
    errorHandler: (String) -> Unit
) = withContext(Dispatchers.IO) {
    runCatching { audio.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        .onFailure { e -> errorHandler(e.message ?: "Unknown error") }
        .getOrNull()
}