package cn.hkim.addon.utils

import cn.hkim.addon.Hkim.mc
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.*

val prefix = Component.literal("[").withStyle(*arrayOf(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD))
    .append(coloredChar("H", 0x9e65d7))
    .append(coloredChar("k", 0x8b73dd))
    .append(coloredChar("i", 0x7980e4))
    .append(coloredChar("m", 0x668eea))
    .append(Component.literal("]").withStyle(*arrayOf(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD)))

fun modMessage(message: Any?) {
    val text = prefix.copy().append(Component.literal("§r §7$message"))
    if (mc.isSameThread) mc.gui.chat.addClientSystemMessage(text)
    else mc.execute { mc.gui.chat.addClientSystemMessage(text) }
}

fun sendChatMessage(message: Any) {
    mc.execute { mc.player?.connection?.sendChat(message.toString()) }
}

fun sendCommand(command: String) {
    mc.execute { mc.player?.connection?.sendCommand(command) }
}

fun coloredChar(char: String, color: Int) =
    Component.literal(char).withColor(color)

val String.clean: String
    get() = this.replace(Regex("§[0-9a-fk-or]"), "")

val Component.cleanString: String
    get() = this.string.replace(Regex("§[0-9a-fk-or]"), "").replace(Regex("\\[(.*?)]"), "$1")

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

private fun getLegacyColorCode(rgb: Int): String? = when (rgb) {
    0x000000 -> "§0"
    0x0000AA -> "§1"
    0x00AA00 -> "§2"
    0x00AAAA -> "§3"
    0xAA0000 -> "§4"
    0xAA00AA -> "§5"
    0xFFAA00 -> "§6"
    0xAAAAAA -> "§7"
    0x555555 -> "§8"
    0x5555FF -> "§9"
    0x55FF55 -> "§a"
    0x55FFFF -> "§b"
    0xFF5555 -> "§c"
    0xFF55FF -> "§d"
    0xFFFF55 -> "§e"
    0xFFFFFF -> "§f"
    else -> null
}