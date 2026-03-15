package org.lain.violin.client

import de.keksuccino.melody.resources.audio.openal.ALAudioClip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient

class ViolinClient : ClientModInitializer {
    private val slots = mutableMapOf<String, Slot>()
    private val fadeOuts = mutableListOf<ALAudioClip>()

    data class Slot(val audio: ALAudioClip)

    override fun onInitializeClient() {
        registerClientCommands(this)
        registerClientReceivers(this)
        ClientTickEvents.END_CLIENT_TICK.register { tick(it) }
    }

    fun containsSlot(slot: String) = slots.containsKey(slot)

    fun play(slot: String, audio: ALAudioClip) {
        if (slots.containsKey(slot)) return
        slots[slot] = Slot(audio)
        audio.play()
    }

    fun stop(slot: String) {
        slots.remove(slot)?.let { fadeOutAudio(it.audio) }
    }

    fun stopAll() {
        slots.forEach { (slotName, slot) -> stop(slotName) }
    }

    private fun fadeOutAudio(audio: ALAudioClip) {
        fadeOuts.add(audio)
    }

    private fun tick(client: MinecraftClient) {
        if ((!client.isInSingleplayer && !client.isIntegratedServerRunning) || client.world == null) stopAll()
        fadeOuts.forEach {
            it.volume *= 0.96f
            if (it.volume < 0.001f) {
                it.close()
            }
        }
        fadeOuts.removeIf { it.isClosed }
    }

    companion object {
        val COROUTINE_SCOPE = CoroutineScope(Dispatchers.IO)
    }
}
