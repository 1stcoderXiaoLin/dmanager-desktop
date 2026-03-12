import java.io.File

plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    application
    jacoco
    id("com.diffplug.spotless") version "6.25.0"
}

group = "com.dmanager"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.12")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.12")
    implementation("io.ktor:ktor-server-websockets-jvm:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.12")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("com.hierynomus:sshj:0.38.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.24")
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.12")
    testImplementation("io.mockk:mockk:1.13.10")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.dmanager.backend.ApplicationKt")
    applicationName = "dmanager-backend"
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("1.2.1")
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.2.1")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.register("formatCheck") {
    group = "formatting"
    description = "Check backend formatting"
    dependsOn("spotlessCheck")
}

tasks.register("formatFix") {
    group = "formatting"
    description = "Fix backend formatting"
    dependsOn("spotlessApply")
}

val tauriResourcesDir = rootProject.file("../desktop/src-tauri/resources")
val tauriBackendDir = File(tauriResourcesDir, "backend")
val tauriJreDir = File(tauriResourcesDir, "jre")
val backendInstallDir = layout.buildDirectory.dir("install/dmanager-backend")

fun currentJavaTool(name: String): String {
    val executable =
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            "$name.exe"
        } else {
            name
        }
    return File(System.getProperty("java.home"), "bin/$executable").absolutePath
}

fun runCommand(
    command: List<String>,
    workingDir: File,
    captureStdout: Boolean = false,
): String {
    val processBuilder = ProcessBuilder(command)
    processBuilder.directory(workingDir)
    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
    if (!captureStdout) {
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT)
    }

    val process = processBuilder.start()
    val output =
        if (captureStdout) {
            process.inputStream.bufferedReader().readText()
        } else {
            process.inputStream.close()
            ""
        }

    val exitCode = process.waitFor()
    if (exitCode != 0) {
        error("Command failed (${command.joinToString(" ")}) with exit code $exitCode")
    }
    return output.trim()
}

tasks.register("syncBundledRuntime") {
    group = "distribution"
    description = "Generate a trimmed JRE with jlink"
    dependsOn(tasks.installDist)

    doLast {
        val libDir = backendInstallDir.get().dir("lib").asFile
        val jars =
            libDir.listFiles { file -> file.extension == "jar" }?.sortedBy { it.name }
                ?: error("No backend jars found under ${libDir.absolutePath}")
        val mainJar =
            jars.firstOrNull { it.name.startsWith("${application.applicationName}-") }
                ?: error("Cannot find backend application jar under ${libDir.absolutePath}")
        val classpath = jars.joinToString(File.pathSeparator) { it.absolutePath }
        val detectedModulesOutput =
            runCommand(
                listOf(
                    currentJavaTool("jdeps"),
                    "--ignore-missing-deps",
                    "--multi-release",
                    "17",
                    "--print-module-deps",
                    "--class-path",
                    classpath,
                    mainJar.absolutePath,
                ),
                workingDir = project.projectDir,
                captureStdout = true,
            )
        val detectedModules =
            detectedModulesOutput
                .split(",")
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toMutableSet()
        detectedModules.add("jdk.crypto.ec")

        project.delete(tauriJreDir)
        runCommand(
            listOf(
                currentJavaTool("jlink"),
                "--add-modules",
                detectedModules.joinToString(","),
                "--output",
                tauriJreDir.absolutePath,
                "--strip-debug",
                "--no-header-files",
                "--no-man-pages",
            ),
            workingDir = project.projectDir,
        )
    }
}

tasks.register<Sync>("syncTauriResources") {
    group = "distribution"
    description = "Sync backend distribution and bundled JRE into Tauri resources"
    dependsOn(tasks.installDist, tasks.named("syncBundledRuntime"))
    from(backendInstallDir)
    into(tauriBackendDir)
}
