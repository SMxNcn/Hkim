package cn.hkim.addon.utils

import cn.hkim.addon.Hkim.mc
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.*

val prefix = Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
    .append(coloredChar("H", 0xAD74E6).withStyle(ChatFormatting.BOLD))
    .append(coloredChar("k", 0x9A82EC).withStyle(ChatFormatting.BOLD))
    .append(coloredChar("i", 0x888FF3).withStyle(ChatFormatting.BOLD))
    .append(coloredChar("m", 0x759DF9).withStyle(ChatFormatting.BOLD))
    .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY))

fun modMessage(message: Any?) {
    val text = prefix.copy().append(Component.literal("§r §7$message"))
    mc.execute { mc.gui.hud.chat.addClientSystemMessage(text) }
}

fun sendChatMessage(message: Any) {
    mc.execute { mc.player?.connection?.sendChat(message.toString()) }
}

fun sendCommand(command: String) {
    mc.execute { mc.player?.connection?.sendCommand(command) }
}

val String.clean: String
    get() = this.replace(Regex("§."), "")

val String.cleanInvisible: String
    get() = this.replace(Regex("§[^0-9a-fk-orz]"), "")

val Component.cleanString: String
    get() = this.string.replace(Regex("§."), "").removeSurrounding("[", "]")

val Component.legacy: String
    get() {
        val builder = StringBuilder()

        this.visit({ style: Style, text: String ->
            if (!style.isEmpty) {
                style.color?.let { color ->
                    val colorCode = getLegacyColorCode(color.value)
                    if (colorCode != null) {
                        builder.append(colorCode)
                    }
                }

                if (style.isBold) builder.append("§l")
                if (style.isItalic) builder.append("§o")
                if (style.isUnderlined) builder.append("§n")
                if (style.isStrikethrough) builder.append("§m")
                if (style.isObfuscated) builder.append("§k")
            }

            builder.append(text)
            Optional.empty<Unit>()
        }, Style.EMPTY)

        return builder.toString()
    }
