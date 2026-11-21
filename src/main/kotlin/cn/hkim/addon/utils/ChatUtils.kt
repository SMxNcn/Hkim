package cn.hkim.addon.utils

import cn.hkim.addon.Hkim.mc
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.*

val prefix = Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
    .append("H".withColor(0xAD74E6).withStyle(ChatFormatting.BOLD))
    .append("k".withColor(0x9A82EC).withStyle(ChatFormatting.BOLD))
    .append("i".withColor(0x888FF3).withStyle(ChatFormatting.BOLD))
    .append("m".withColor(0x759DF9).withStyle(ChatFormatting.BOLD))
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
        var active = false

        this.visit({ style: Style, text: String ->
            if (text.isNotEmpty()) {
                val colorCode = style.color?.let { getLegacyColorCode(it.value) }
                if (colorCode != null) builder.append(colorCode)
                else if (active) builder.append("§r")

                val bold = style.isBold
                val italic = style.isItalic
                val underlined = style.isUnderlined
                val strikethrough = style.isStrikethrough
                val obfuscated = style.isObfuscated
                if (bold) builder.append("§l")
                if (italic) builder.append("§o")
                if (underlined) builder.append("§n")
                if (strikethrough) builder.append("§m")
                if (obfuscated) builder.append("§k")

                active = colorCode != null || bold || italic || underlined || strikethrough || obfuscated
            }

            builder.append(text)
            Optional.empty<Unit>()
        }, Style.EMPTY)

        return builder.toString()
    }
