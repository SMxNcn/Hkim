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

        var activeColor: String? = null
        var bold = false
        var italic = false
        var underlined = false
        var strikethrough = false
        var obfuscated = false

        fun reset() {
            builder.append("§r")
            activeColor = null
            bold = false
            italic = false
            underlined = false
            strikethrough = false
            obfuscated = false
        }

        this.visit({ style: Style, text: String ->
            val colorCode = style.color?.let { getLegacyColorCode(it.value) }
            val wantBold = style.isBold
            val wantItalic = style.isItalic
            val wantUnderlined = style.isUnderlined
            val wantStrikethrough = style.isStrikethrough
            val wantObfuscated = style.isObfuscated

            if ((bold && !wantBold) || (italic && !wantItalic) ||
                (underlined && !wantUnderlined) || (strikethrough && !wantStrikethrough) ||
                (obfuscated && !wantObfuscated) ||
                (colorCode == null && activeColor != null)
            ) {
                reset()
            }

            if (colorCode != null && colorCode != activeColor) {
                builder.append(colorCode)
                activeColor = colorCode
                bold = false
                italic = false
                underlined = false
                strikethrough = false
                obfuscated = false
            }

            if (wantBold && !bold) { builder.append("§l"); bold = true }
            if (wantItalic && !italic) { builder.append("§o"); italic = true }
            if (wantUnderlined && !underlined) { builder.append("§n"); underlined = true }
            if (wantStrikethrough && !strikethrough) { builder.append("§m"); strikethrough = true }
            if (wantObfuscated && !obfuscated) { builder.append("§k"); obfuscated = true }

            builder.append(text)
            Optional.empty<Unit>()
        }, Style.EMPTY)

        return builder.toString()
    }
