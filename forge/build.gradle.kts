// Build plugin confirmed from official Forge MDK 26.3-66.0.8: id "net.minecraftforge.gradle" version "[7.0.17,8)" (ForgeGradle 7)
plugins {
    id("java")
    id("net.minecraftforge.gradle") version "[7.0.17,8)"
}

version = providers.gradleProperty("mod_version").get()

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

minecraft {
    runs {
        configureEach {
            workingDir = layout.projectDirectory.dir("run")
            systemProperty("eventbus.api.strictRuntimeChecks", "true")
        }
        register("client")
        register("server") { args("--nogui") }
    }
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
    mavenCentral()
}

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:26.3-66.0.8"))
    annotationProcessor("net.minecraftforge:eventbus-validator:7.0.6")
    implementation(project(":common"))

}

tasks.processResources {
    val modVersion = project.version.toString()
    inputs.property("version", modVersion)
    filesMatching("META-INF/mods.toml") {
        expand("version" to modVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// shortcut: common classes are merged into the jar instead of jar-in-jar, upgrade if FG7 jarJar is needed
tasks.jar {
    dependsOn(":common:jar")
    from({ zipTree(project(":common").tasks.named<Jar>("jar").get().archiveFile) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
