package org.lain.violin

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

class Violin : ModInitializer {
    override fun onInitialize() {
        registerPayloadTypes()
        registerServerReceivers(CONTEXT)
        registerCommands(CONTEXT)

        ServerLifecycleEvents.SERVER_STARTED.register { server -> RecentSlots.loadHistory() }
        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            CONTEXT.slots.clear()
            RecentSlots.SLOTS.clear()
        }
        ServerPlayConnectionEvents.JOIN.register { handler, sender, server ->
            handler.player.sendRecentSlotsPacket(RecentSlots.SLOTS)
            CONTEXT.slots.forEach { (_, playback) -> handler.player.sendPlayPacket(playback) }
        }
    }

    companion object {
        val CONTEXT = ViolinContext()
        val LOGGER = LoggerFactory.getLogger("Violin")

        fun id(path: String) = Identifier.of("violin", path)
    }
}
