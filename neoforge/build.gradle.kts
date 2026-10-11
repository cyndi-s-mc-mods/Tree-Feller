// Setup mirrors NeoForgeMDKs/MDK-26.3-ModDevGradle (ModDevGradle 2.0.148)
plugins {
    id("java-library")
    id("net.neoforged.moddev") version "2.0.148"
}

version = providers.gradleProperty("mod_version").get()

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

neoForge {
    version = providers.gradleProperty("neo_version").get()
    runs {
        register("client") { client() }
        register("server") {
            server()
            programArgument("--nogui")
        }
    }
    mods {
        register("treefeller") { sourceSet(sourceSets.main.get()) }
    }
}

dependencies {
    implementation(project(":common"))
}

tasks.processResources {
    val modVersion = project.version.toString()
    inputs.property("version", modVersion)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("version" to modVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// shortcut: common classes are merged into the jar instead of jar-in-jar, upgrade if jarJar is needed
tasks.jar {
    dependsOn(":common:jar")
    from({ zipTree(project(":common").tasks.named<Jar>("jar").get().archiveFile) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
