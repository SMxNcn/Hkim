package cn.hkim.addon.features

enum class Category {
    SKYBLOCK, RENDER, MISC
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ModuleInfo(
    val id: String,
    val category: Category,
    val default: Boolean = false
)