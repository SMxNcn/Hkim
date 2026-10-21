package cn.hkim.addon.utils.notification

import cn.hkim.addon.utils.render.island.IslandQueue

private const val ICON_DIR = "assets/hkim/textures/clickgui/"

const val ICON_ENABLED = "${ICON_DIR}NotificationEnabled.svg"
const val ICON_DISABLED = "${ICON_DIR}NotificationDisabled.svg"
const val ICON_WARNING = "${ICON_DIR}NotificationWarning.svg"

val NOTIFICATION_TITLE_COLOR = 0xFF8A8A8A.toInt()
val NOTIFICATION_DETAIL_COLOR = 0xFFFFFFFF.toInt()
val NOTIFICATION_SUCCESS_COLOR = 0xFF55FF55.toInt()
val NOTIFICATION_FAILURE_COLOR = 0xFFFF5555.toInt()

enum class NotificationType(
    val displayName: String,
    val defaultIcon: String,
    val defaultIconColor: Int,
    val defaultDurationMs: Long,
    val priority: Int,
    val defaultTitleColor: Int? = null,
    val defaultDetailColor: Int? = null,
) {
    MODULE("Module", ICON_ENABLED, NOTIFICATION_SUCCESS_COLOR, 500L, IslandQueue.PRIORITY_MODULE),
    CHECK("Fail Safe", ICON_WARNING, NOTIFICATION_FAILURE_COLOR, 4000L, IslandQueue.PRIORITY_FAIL_SAFE, NOTIFICATION_FAILURE_COLOR, NOTIFICATION_FAILURE_COLOR),
}

data class Notification(
    val type: NotificationType,
    val detail: String,
    val icon: String = type.defaultIcon,
    val iconColor: Int = type.defaultIconColor,
    val durationMs: Long = type.defaultDurationMs,
    val titleColor: Int? = type.defaultTitleColor,
    val detailColor: Int? = type.defaultDetailColor,
) {
    val title: String get() = type.displayName
}
