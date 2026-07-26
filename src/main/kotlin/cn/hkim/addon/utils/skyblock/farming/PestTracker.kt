package cn.hkim.addon.utils.skyblock.farming

import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.decoration.ArmorStand
import java.util.*

object PestTracker {
    @Volatile var aliveCount: Int = -1
    @Volatile var pestPlots: Set<Int> = emptySet()
    var lastPestPlot = -1

    private val knownPestIds = mutableSetOf<Int>()
    private val checkedNonPestIds = mutableSetOf<Int>()
    private val pestTextures = setOf(
        "f379e09252817314bd0b694f7d53b48af2c7fa8499109802a41bb294d2f93e3e", // Field Mouse
        "9d90e777826a52461368e26d1b2e19bfa1ba582d602483e545f4124d0f731842", // Fly
        "bee4fcc5aac27bdbb0be210df08b05bca5aebc89bf2821f608a55fd2cf0434be", // Lunar Moth
        "a24c69f96ce556221e195c8ef2bfad71ebf7f95f5ae914a484a8d0ec21672674", // Cricket
        "4b24a482a32db1ea78fb98060b0c2fa4a373cbd18a68edddeb7419455a59cda9", // Locust
        "a8abb471db0ab78703011979dc8b40798a941f3a4dec3ec61cbeec2af8cffe8", // Rat
        "52a9fe05bc663efcd12e56a3ccc5ec035bf577b78708548b6f4ffcf1d30eccfe", // Mosquito
        "6403ba4027a333d8d2fd32ab59d1cfdbaa7d908d80d2381db2a69cbe65450ad8", // Earthworm
        "be6baf6431a9daa2ca604d5a3c26e9a761d5952f0817174a4fe0b764616e21ff", // Mite
        "65485c4b34e5b5470be94de100e61f7816f81bc5a11dfdf0eccf890172da5d0a", // Moth
        "7a79d0fd677b54530961117ef84adc206e2cc5045c1344d61d776bf8ac2fe1ba", // Slug
        "70a1e836bf1968b2eaa4837227a19204f17295d870ee9e754bd6b6d60ddbed3c", // Beetle
        "4ce79e90adf34718f313ec24d6c6135b69b3788c618498446ccc83ca640c0b14", // Firefly
        "3e52782d7f2aaee8af5ba2928fec7885e94879334c2296bc9e7e2dbc5418e58f", // Firefly (light)
        "254aff4c0b2dce3a672349cc0ee9e6f3a9deebe4b3556e84611eca250a7821bf", // Dragonfly
        "1e04bb6367caa4e88f5fd0ee80f0745d137a6060223dbbc42a16471fdf64bb83"  // Praying Mantis
    )

    @JvmStatic
    fun isPestEntity(entity: ArmorStand): Boolean {
        if (entity.id in knownPestIds) return true
        if (entity.id in checkedNonPestIds) return false

        val headItem = entity.getItemBySlot(EquipmentSlot.HEAD)
        if (headItem.isEmpty) {
            checkedNonPestIds.add(entity.id)
            return false
        }

        val profile = headItem.get(DataComponents.PROFILE) ?: run {
            checkedNonPestIds.add(entity.id)
            return false
        }
        val gameProfile = profile.partialProfile()

        val texturesProperty = gameProfile.properties()["textures"].firstOrNull() ?: run {
            checkedNonPestIds.add(entity.id)
            return false
        }
        val base64Value = texturesProperty.value() ?: run {
            checkedNonPestIds.add(entity.id)
            return false
        }

        val decoded = try {
            String(Base64.getDecoder().decode(base64Value))
        } catch (_: Exception) {
            checkedNonPestIds.add(entity.id)
            return false
        }

        val matched = pestTextures.any { decoded.contains(it) }
        if (matched) knownPestIds.add(entity.id)
        else checkedNonPestIds.add(entity.id)
        return matched
    }

    fun clearKnownPests() {
        knownPestIds.clear()
    }

    @JvmStatic
    fun excludeEntity(id: Int) {
        checkedNonPestIds.add(id)
    }
}
