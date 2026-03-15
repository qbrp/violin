package org.lain.violin

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import de.keksuccino.melody.resources.audio.SimpleAudioFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.lain.violin.Violin.Companion.LOGGER
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class FriendlyException(message: String) : Exception(message)

fun <T, S> RequiredArgumentBuilder<S, T>.executeCatching(statement: CommandContext<S>.() -> Unit) = executes { ctx ->
    val commandName = ctx.rootNode.name
    try {
        ctx.statement()
    } catch (e: Throwable) {
        when (val source = ctx.source) {
            is ServerCommandSource -> {
                source.sendError(e.message ?: "Что-то пошло не так.")
            }
        }
        if (e !is FriendlyException) {
            LOGGER.error("При выполнении команды $commandName возникла ошибка:", e)
        }
    }
    1
}

fun registerCommands(context: ViolinContext) {
    CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
        dispatcher.register(
            CommandManager.literal("vstop")
                .then(
                    CommandManager.argument("slot", StringArgumentType.word())
                        .suggests(ActiveSlotsSuggestionProvider(context))
                        .executeCatching {
                            val slotName = StringArgumentType.getString(this, "slot")
                            val slot = context.slots.remove(slotName) ?: throw FriendlyException("Слот $slotName неактивен")
                            source.server.broadcastStopPacket(slot.id)
                            source.sendFeedback("Остановлено воспроизведение слота $slotName", Formatting.GOLD)
                        }
                )
        )
    }
}

private fun ServerCommandSource.sendFeedback(message: String, format: Formatting) {
    sendFeedback({ Text.literal(message).formatted(format) }, false)
}

private fun ServerCommandSource.sendError(message: String) {
    sendError(Text.of(message))
}

class ActiveSlotsSuggestionProvider(private val violinContext: ViolinContext) : SuggestionProvider<ServerCommandSource> {
    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        violinContext.slots.forEach { (slot, _) -> builder.suggest(slot) }
        return builder.buildFuture()
    }
}

object RecentSlots {
    private val FILE = File("recent-slots.txt")
    val SLOTS = mutableSetOf<String>()

    init { if (!FILE.exists()) FILE.createNewFile() }

    fun addSlotToHistory(slot: String) {
        if (slot == "master") return
        if (!SLOTS.add(slot)) return
        FILE.writeText(SLOTS.joinToString("\n"))
    }

    fun loadHistory() {
        SLOTS.addAll(FILE.readLines())
    }
}