package org.lain.violin.client

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import de.keksuccino.melody.resources.audio.SimpleAudioFactory
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.lain.violin.PlayPacket
import org.lain.violin.RecentSlots
import org.lain.violin.client.ViolinClient.Companion.COROUTINE_SCOPE
import org.lain.violin.executeCatching
import java.util.concurrent.CompletableFuture

fun registerClientCommands(violin: ViolinClient) {
    ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
        dispatcher.register(
            ClientCommandManager.literal("vplay")
                .then(
                    ClientCommandManager.argument("slot", StringArgumentType.word())
                        .suggests(RecentSlotsSuggestionProvider())
                        .then(
                            ClientCommandManager.argument("url", StringArgumentType.greedyString()
                            )
                                .executeCatching {
                                    source.sendFeedback("Трек загружается. Это может продолжаться некоторое время и вызывать фризы.")
                                    val slot = StringArgumentType.getString(this, "slot")
                                    val url = StringArgumentType.getString(this, "url")
                                    source.client.execute {
                                        val audioFuture =
                                            SimpleAudioFactory.ogg(url, SimpleAudioFactory.SourceType.WEB_FILE)
                                        COROUTINE_SCOPE.launch {
                                            getOggWebAudioClip(audioFuture) { source.sendError(it) }
                                                ?.let {
                                                    violin.play(slot, it)
                                                    ClientPlayNetworking.send(PlayPacket.c2s(url, slot))
                                                }
                                        }
                                    }
                                }
                        )
                )
        )
    }
}

private fun FabricClientCommandSource.sendFeedback(message: String) {
    sendFeedback(Text.literal(message).formatted(Formatting.GREEN))
}

private fun FabricClientCommandSource.sendError(message: String) {
    sendError(Text.of(message))
}

class RecentSlotsSuggestionProvider : SuggestionProvider<FabricClientCommandSource> {
    override fun getSuggestions(
        context: CommandContext<FabricClientCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val slots = RecentSlots.SLOTS + "master"
        slots.reversed().forEach { builder.suggest(it) }
        return builder.buildFuture()
    }
}