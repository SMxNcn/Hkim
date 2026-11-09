package cn.hkim.addon.utils.skyblock.slayer

enum class SlayerType(
    val displayName: String,
    val meterName: String,
    val meterKey: String
) {
    REVENANT("Revenant Horror", "Zombie Slayer", "Zombie"),
    TARANTULA("Tarantula Broodfather", "Spider Slayer", "Spider"),
    SVEN("Sven Packmaster", "Wolf Slayer", "Wolf"),
    VOID("Voidgloom Seraph", "Enderman Slayer", "Enderman"),
    INFERNO("Inferno Demonlord", "Blaze Slayer", "Blaze"),
    VAMPIRE("Riftstalker Bloodfiend", "Vampire Slayer", "Vampire");

    val allNames: List<String> get() = listOf(displayName, meterName)

    companion object {
        fun fromText(text: String): SlayerType? =
            entries.firstOrNull { type ->
                type.allNames.any { text.contains(it, ignoreCase = true) }
            }
    }
}
