package org.lain.violin.client

import de.keksuccino.melody.resources.audio.SimpleAudioFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.text.Text
import org.lain.violin.PlayPacket
import org.lain.violin.RecentSlots
import org.lain.violin.RecentSlotsPacket
import org.lain.violin.StopPacket
import org.lain.violin.client.ViolinClient.Companion.COROUTINE_SCOPE

fun registerClientReceivers(violin: ViolinClient) {
    ClientPlayNetworking.registerGlobalReceiver(PlayPacket.CLIENTBOUND_PACKET_ID) { packet, ctx ->
        runBlocking {
            val slot = packet.slot
            if (!violin.containsSlot(slot)) {
                val audioFuture = SimpleAudioFactory.ogg(packet.url, SimpleAudioFactory.SourceType.WEB_FILE)
                val audioClip = COROUTINE_SCOPE.async { getOggWebAudioClip(audioFuture) { ctx.client().inGameHud.setOverlayMessage(Text.of(it), false) } }
                violin.play(slot, audioClip.await() ?: return@runBlocking)
            }
        }
    }
    ClientPlayNetworking.registerGlobalReceiver(StopPacket.PACKET_ID) { packet, ctx ->
        violin.stop(packet.slot)
    }
    ClientPlayNetworking.registerGlobalReceiver(RecentSlotsPacket.PACKET_ID) { packet, ctx ->
        RecentSlots.SLOTS.clear()
        RecentSlots.SLOTS.addAll(packet.slots)
    }
}