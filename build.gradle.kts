import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.language.jvm.tasks.ProcessResources

/* ------------------------------ Plugins ------------------------------ */
plugins {
    id("java")
    id("java-library")
    id("com.diffplug.spotless") version "6.25.0"
    id("com.gradleup.shadow") version "8.3.9"
    id("checkstyle")
    eclipse
    kotlin("jvm") version "2.1.21"
}

extra["kotlinAttribute"] = Attribute.of("kotlin-tag", Boolean::class.javaObjectType)

val kotlinAttribute: Attribute<Boolean> by rootProject.extra

/* --------------------------- JDK / Kotlin ---------------------------- */
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.GRAAL_VM)
    }

    withSourcesJar()
}

kotlin {
    jvmToolchain(17)
}

/* ----------------------------- Metadata ------------------------------ */
group = "plugily.projects"
version = "5.1.3"

description = "BuildBattle"

val apiVersion = "1.19"

/* ----------------------------- Resources ----------------------------- */
tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version, "apiVersion" to apiVersion)
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
    from("LICENSE.md") { into("/") }
}

/* ---------------------------- Repos ---------------------------------- */
repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://repo.purpurmc.org/snapshots") }
    maven { url = uri("https://maven.plugily.xyz/releases") }
    maven { url = uri("https://maven.plugily.xyz/snapshots") }
    maven { url = uri("https://repo.citizensnpcs.co/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("file://${System.getProperty("user.home")}/.m2/repository") }
    System.getProperty("SELF_MAVEN_LOCAL_REPO")?.let {
        val dir = file(it)
        if (dir.isDirectory) {
            println("Using SELF_MAVEN_LOCAL_REPO at: $it")
            maven { url = uri("file://${dir.absolutePath}") }
        } else {
            logger.error("SELF_MAVEN_LOCAL_REPO was set but not found, defaulting to ~/.m2 for mavenLocal()")
            mavenLocal()
        }
    } ?: mavenLocal()
}

/* ---------------------- Java project deps ---------------------------- */
dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("net.citizensnpcs:citizensapi:2.0.26-SNAPSHOT") {
        exclude(group = "ch.ethz.globis.phtree", module = "phtree")
    }
    implementation("plugily.projects:MiniGamesBox-Classic:1.4.5") {
        attributes { attribute(kotlinAttribute, true) }
    }
}

apply(from = "eclipse.gradle.kts")

/* ---------------------- Reproducible jars ---------------------------- */
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

/* ----------------------------- Shadow -------------------------------- */
tasks.named<ShadowJar>("shadowJar") {
    isEnableRelocation = true
    relocationPrefix = "${project.group}.shadow"
    relocate("com.zaxxer.hikari", "plugily.projects.buildbattle.database.hikari")
    relocate("plugily.projects.minigamesbox", "plugily.projects.buildbattle.minigamesbox")
    archiveClassifier.set("")
    minimize()
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("part")
}

tasks.named("build") {
    dependsOn(tasks.named("spotlessApply"), tasks.named("shadowJar"))
}

/* --------------------------- Javac opts ------------------------------- */
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
    options.isFork = true
    options.compilerArgs.add("-Xlint:deprecation")
    options.encoding = "UTF-8"
    options.release.set(17)
}

/* ----------------------------- Auto Formatting ------------------------ */
spotless {
    java {
        eclipse().configFile("config/formatter/eclipse-java-formatter.xml")
        leadingTabsToSpaces()
        removeUnusedImports()
        target("src/**/*.java")
    }
    kotlinGradle {
        ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) }
        target("build.gradle.kts", "settings.gradle.kts", "eclipse.gradle.kts")
    }
}

checkstyle {
    toolVersion = "10.18.1"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = true
    isShowViolations = true
}

tasks.named("compileJava") {
    dependsOn("spotlessApply")
}

tasks.named("spotlessCheck") {
    dependsOn("spotlessApply")
}

/* ------------------------------ Eclipse SHIM ------------------------- */
subprojects {
    apply(plugin = "java-library")
    apply(plugin = "eclipse")
    eclipse.project.name = "${project.name}-${rootProject.name}"
    tasks.withType<Jar>().configureEach { archiveBaseName.set("${project.name}-${rootProject.name}") }
}
