import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom")
    kotlin("jvm")
    `maven-publish`
}

data class McVersion(
    val minecraft: String,
    val loader: String,
    val fabricApi: String,
    val modmenu: String,
    val iris: String,
    val placeholderApi: String,
    val minecraftRange: String,
)

val mcVersion = stonecutter.current.version
val mc = when (mcVersion) {
    "26.2" -> McVersion(
        minecraft = "26.2",
        loader = "0.20.1",
        fabricApi = "0.161.0+26.2",
        modmenu = "20.0.1",
        iris = "1.11.4+26.2-fabric",
        placeholderApi = "3.1.0-beta.1+26.2",
        minecraftRange = "~26.2",
    )
    "26.3" -> McVersion(
        minecraft = "26.3",
        loader = "0.20.1",
        fabricApi = "0.165.0+26.3",
        modmenu = "21.0.0",
        iris = "1.11.6+26.3-fabric",
        placeholderApi = "3.2.0+26.3",
        minecraftRange = "~26.3",
    )
    else -> error("Unsupported Minecraft version: $mcVersion")
}

extra["minecraft_version"] = mc.minecraft
extra["loader_version"] = mc.loader
extra["fabric_api_version"] = mc.fabricApi
extra["modmenu_version"] = mc.modmenu
extra["iris_version"] = mc.iris
extra["placeholder_api_version"] = mc.placeholderApi
extra["minecraft_range"] = mc.minecraftRange

group = property("maven_group")!!
version = property("mod_version")!!

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://maven.meteordev.org/releases")
    maven("https://maven.meteordev.org/snapshots")
    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.deftu.dev/snapshots")
    maven("https://maven.deftu.dev/releases")
    maven("https://maven.nucleoid.xyz/releases")
    maven("https://redirector.kotlinlang.org/maven/compose-dev")
    maven("https://api.modrinth.com/maven")
}

dependencies {
    minecraft("com.mojang:minecraft:${mc.minecraft}")

    implementation("net.fabricmc:fabric-loader:${mc.loader}")
    implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${mc.fabricApi}")

    implementation("meteordevelopment:orbit:${property("orbit_version")}")
    implementation("com.github.stivais:Commodore:${property("commodore_version")}")
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth_version")}")
    compileOnly("com.terraformersmc:modmenu:${mc.modmenu}")
    compileOnly("eu.pb4:placeholder-api:${mc.placeholderApi}")
    compileOnly("maven.modrinth:iris:${mc.iris}")

    include("meteordevelopment:orbit:${property("orbit_version")}")
    include("com.github.stivais:Commodore:${property("commodore_version")}")

    implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:${property("skiko_version")}")
}

if (project != rootProject) {
    afterEvaluate {
        val main = sourceSets.main.get()
        if (mcVersion == stonecutter.active?.version) {
            main.kotlin.setSrcDirs(
                listOf(
                    rootProject.file("src/main/kotlin"),
                    rootProject.file("src/main/java"),
                    project.file("src/main/kotlin"),
                    project.file("src/main/java"),
                )
            )
            main.java.setSrcDirs(
                listOf(
                    rootProject.file("src/main/java"),
                    project.file("src/main/java"),
                )
            )
            main.resources.setSrcDirs(
                listOf(
                    rootProject.file("src/main/resources"),
                    project.file("src/main/resources"),
                )
            )
        }

    }

    gradle.projectsEvaluated {
        val main = sourceSets.main.get()
        val generatedRoot = layout.buildDirectory.dir("generated/stonecutter").get().asFile.toPath()
        val rawRoots = listOf(
            rootProject.file("src/main/kotlin").toPath(),
            rootProject.file("src/main/java").toPath(),
        )
        fun isGenerated(dir: java.io.File) = dir.toPath().startsWith(generatedRoot)
        fun isRawRoot(dir: java.io.File) = rawRoots.any { it == dir.toPath() }
        if (main.kotlin.srcDirs.any(::isGenerated) && main.kotlin.srcDirs.any(::isRawRoot)) {
            val keepGenerated = mcVersion != stonecutter.active?.version
            val drop: (java.io.File) -> Boolean = if (keepGenerated) ::isRawRoot else ::isGenerated
            main.kotlin.setSrcDirs(main.kotlin.srcDirs.filterNot(drop))
            main.java.setSrcDirs(main.java.srcDirs.filterNot(drop))
        }
    }
}

loom {
    runConfigs.named("client") {
        isIdeConfigGenerated = true
        vmArgs.addAll(
            arrayOf(
                "-Dmixin.debug.export=true",
                "-Ddevauth.enabled=true",
                "-Ddevauth.account=main",
                "-Dmeteordevelopment.orbit.lambdas=false"
            )
        )
    }

    runConfigs.named("server") {
        isIdeConfigGenerated = false
    }
}

afterEvaluate {
    loom.runs.named("client") {
        vmArg("-javaagent:${configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") }}")
    }
}

tasks {
    processResources {
        val expandProps = mapOf(
            "mod_version" to version,
            "loader_version" to mc.loader,
            "fabric_api_version" to mc.fabricApi,
            "fabric_kotlin_version" to providers.gradleProperty("fabric_kotlin_version").get(),
            "minecraft_version" to mc.minecraft,
            "minecraft_range" to mc.minecraftRange,
        )
        inputs.properties(expandProps)
        filesMatching("fabric.mod.json") {
            expand(expandProps)
        }
    }

    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
            freeCompilerArgs.add("-Xlambdas=class")
        }
    }

    compileJava {
        sourceCompatibility = "25"
        targetCompatibility = "25"
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
}

base {
    archivesName.set(project.property("archives_base_name") as String)
}

// Put both versions' artifacts into the root build/libs and make their names
// Unique: Hkim-<mod_version>-mc<minecraft>.jar.
tasks.withType<Jar>().configureEach {
    archiveVersion.set("${project.property("mod_version")}-mc$mcVersion")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
}

tasks.named<Delete>("clean") {
    delete(tasks.named<Jar>("jar").map { it.archiveFile })
}

val targetJavaVersion = 25
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}
