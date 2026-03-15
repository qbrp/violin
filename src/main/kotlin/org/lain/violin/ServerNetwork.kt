package org.lain.violin

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.Formatting

fun registerPayloadTypes() {
    PayloadTypeRegistry.playS2C().register(StopPacket.PACKET_ID, StopPacket.CODEC)
    PayloadTypeRegistry.playS2C().register(RecentSlotsPacket.PACKET_ID, RecentSlotsPacket.CODEC)
    PayloadTypeRegistry.playS2C().register(PlayPacket.CLIENTBOUND_PACKET_ID, PlayPacket.getCodec(PlayPacket.CLIENTBOUND_PACKET_ID))
    PayloadTypeRegistry.playC2S().register(PlayPacket.SERVERBOUND_PACKET_ID, PlayPacket.getCodec(PlayPacket.SERVERBOUND_PACKET_ID))
}

fun registerServerReceivers(context: ViolinContext) {
    registerServerReceiver(PlayPacket.SERVERBOUND_PACKET_ID) { packet, player, server ->
        val url = packet.url
        val slot = packet.slot
        if (!isValidURL(url)) {
            player.sendError("Ссылка имеет неправильный формат или ошибки")
            return@registerServerReceiver
        }
        if (context.slots[slot] != null) {
            player.sendError("Слот $slot занят. Чтобы использовать его для проигрывания трека, остановите воспроизведение через /vstop")
            return@registerServerReceiver
        }

        val playback = Playback(slot, url)
        context.slots[slot] = playback
        RecentSlots.addSlotToHistory(slot)
        if (!server.isSingleplayer) server.broadcastRecentSlots(RecentSlots.SLOTS)
        server.broadcastPlayPacket(playback)
        player.sendSuccess("Назначено воспроизведение трека по ссылке $url под слотом $slot")
    }
}

fun MinecraftServer.broadcastRecentSlots(slots: Set<String>) {
    playerManager.playerList.forEach { player -> player.sendRecentSlotsPacket(slots) }
}

fun ServerPlayerEntity.sendRecentSlotsPacket(slots: Set<String>) {
    ServerPlayNetworking.send(this, RecentSlotsPacket(slots))
}

fun MinecraftServer.broadcastStopPacket(slot: String) {
    playerManager.playerList.forEach { player -> ServerPlayNetworking.send(player, StopPacket(slot)) }
}

fun ServerPlayerEntity.sendPlayPacket(playback: Playback) {
    ServerPlayNetworking.send(this, PlayPacket.s2c(playback))
}

private fun MinecraftServer.broadcastPlayPacket(playback: Playback) {
    playerManager.playerList.forEach { player -> player.sendPlayPacket(playback) }
}

private fun <T : CustomPayload> registerServerReceiver(type: CustomPayload.Id<T>, handler: (T, ServerPlayerEntity, MinecraftServer) -> Unit) {
    ServerPlayNetworking.registerGlobalReceiver(type) { packet, ctx ->
        ctx.server().execute { handler(packet, ctx.player(), ctx.server()) }
    }
}

private fun ServerPlayerEntity.sendError(text: String) {
    sendMessage(Text.literal(text).formatted(Formatting.RED))
}

private fun ServerPlayerEntity.sendSuccess(text: String) {
    sendMessage(Text.literal(text).formatted(Formatting.GREEN))
}