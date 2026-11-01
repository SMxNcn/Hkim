package cn.hkim.addon.utils.notification

import cn.hkim.addon.features.impl.DynamicIsland
import cn.hkim.addon.utils.modMessage
import cn.hkim.addon.utils.render.island.IslandContent
import cn.hkim.addon.utils.render.island.IslandQueue

object NotificationManager {
    fun push(notification: Notification) {
        IslandQueue.show(
            IslandContent(
                key = notification,
                priority = notification.type.priority,
                title = notification.title,
                detail = notification.detail,
                icon = notification.icon,
                iconColor = notification.iconColor,
                titleColor = notification.titleColor,
                detailColor = notification.detailColor,
                durationMs = notification.durationMs,
            )
        )
    }

    fun push(
        type: NotificationType,
        detail: String,
        icon: String = type.defaultIcon,
        iconColor: Int = type.defaultIconColor,
        durationMs: Long = type.defaultDurationMs,
    ) = push(Notification(type, detail, icon, iconColor, durationMs))

    fun toggle(name: String, enabled: Boolean) {
        if (DynamicIsland.enabled) {
            push(
                Notification(
                    type = NotificationType.MODULE,
                    detail = "$name ${if (enabled) "enabled" else "disabled"}",
                    icon = if (enabled) ICON_ENABLED else ICON_DISABLED,
                    iconColor = if (enabled) NOTIFICATION_SUCCESS_COLOR else NOTIFICATION_FAILURE_COLOR,
                )
            )
        } else {
            modMessage("§6$name${if (enabled) "§a enabled." else "§c disabled."}")
        }
    }

    fun alert(type: NotificationType, detail: String, chat: String) {
        if (DynamicIsland.enabled) push(type, detail)
        else modMessage(chat)
    }
}
