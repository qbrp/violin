package org.lain.violin

import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.screen.slot.Slot

class PlayPacket private constructor(
    val url: String,
    val slot: String,
    val _id: CustomPayload.Id<PlayPacket>
) : CustomPayload {
    override fun getId(): CustomPayload.Id<PlayPacket> = _id

    companion object {
        val SERVERBOUND_PACKET_ID = CustomPayload.Id<PlayPacket>(Violin.id("c2s-play"))
        val CLIENTBOUND_PACKET_ID = CustomPayload.Id<PlayPacket>(Violin.id("s2c-play"))

        fun getCodec(id: CustomPayload.Id<PlayPacket>) = PacketCodec.of<PacketByteBuf, PlayPacket>(
            { packet, buf ->
                buf.writeString(packet.url)
                buf.writeString(packet.slot)
            },
            { buf -> PlayPacket(buf.readString(), buf.readString(), id) }
        )!!

        fun c2s(url: String, slot: String) = PlayPacket(url, slot, SERVERBOUND_PACKET_ID)
        fun s2c(playback: Playback) = PlayPacket(playback.url, playback.id, CLIENTBOUND_PACKET_ID)
    }
}

data class StopPacket(val slot: String) : CustomPayload {
    override fun getId(): CustomPayload.Id<StopPacket> = PACKET_ID

    companion object {
        val PACKET_ID = CustomPayload.Id<StopPacket>(Violin.id("stop"))
        val CODEC = PacketCodec.of<PacketByteBuf, StopPacket>(
            { packet, buf -> buf.writeString(packet.slot) },
            { buf -> StopPacket(buf.readString()) }
        )
    }
}


data class RecentSlotsPacket(val slots: Set<String>) : CustomPayload {
    override fun getId(): CustomPayload.Id<RecentSlotsPacket> = PACKET_ID

    companion object {
        val PACKET_ID = CustomPayload.Id<RecentSlotsPacket>(Violin.id("recent-slots"))
        val CODEC = PacketCodec.of<PacketByteBuf, RecentSlotsPacket>(
            { packet, buf -> buf.writeCollection(packet.slots) { buf, value -> buf.writeString(value) } },
            { buf ->
                RecentSlotsPacket(
                    buf.readCollection({ mutableSetOf() }, { it.readString() })
                )
            }
        )
    }
}