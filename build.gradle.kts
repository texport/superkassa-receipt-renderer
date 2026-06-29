import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.nmcp)
    alias(libs.plugins.kotlin.multiplatform)
    `maven-publish`
    signing
    jacoco
}

group = "io.github.texport"
version = "1.0.2"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm()
    
    val xcf = XCFramework("SuperkassaReceiptRenderer")

    iosArm64 {
        binaries.framework {
            baseName = "SuperkassaReceiptRenderer"
            xcf.add(this)
        }
    }
    iosX64 {
        binaries.framework {
            baseName = "SuperkassaReceiptRenderer"
            xcf.add(this)
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "SuperkassaReceiptRenderer"
            xcf.add(this)
        }
    }

    jvmToolchain(libs.versions.java.get().toInt())

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.superkassa.core.domain)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }
        jvmMain {
            dependencies {
                implementation(libs.slf4j.api)
                implementation(libs.zxing.core)
                implementation(libs.zxing.javase)
            }
        }
        jvmTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.mockk)
            }
        }
    }

    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        val javadocJarTask = tasks.register<Jar>("${name}JavadocJar") {
            description = "Generates Javadoc jar for publication ${this@configureEach.name}"
            archiveClassifier.set("javadoc")
            archiveAppendix.set(this@configureEach.name)
        }
        artifact(javadocJarTask)
        pom {
            name.set("superkassa-receipt-renderer")
            description.set("HTML/text receipt rendering engine for Superkassa")
            url.set("https://github.com/texport/superkassa-receipt-renderer")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("sergeyivanov")
                    name.set("Sergey Ivanov")
                    email.set("ivanov.sergey.ekb@gmail.com")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/texport/superkassa-receipt-renderer.git")
                developerConnection.set("scm:git:ssh://github.com/texport/superkassa-receipt-renderer.git")
                url.set("https://github.com/texport/superkassa-receipt-renderer")
            }
        }
    }
}

signing {
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    if (!signingKey.isNullOrEmpty() && !signingPassword.isNullOrEmpty()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    sign(publishing.publications)
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username.set(project.findProperty("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME"))
        password.set(project.findProperty("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD"))
        publishingType.set("USER_MANAGED")
    }
}

jacoco {
    toolVersion = "0.8.12"
}

val jacocoTestReport = tasks.register<JacocoReport>("jacocoTestReport") {
    description = "Generates Jacoco code coverage report for the JVM target."
    dependsOn(tasks.named("jvmTest"))
    classDirectories.setFrom(files(tasks.named("compileKotlinJvm")))
    sourceDirectories.setFrom(files("src/commonMain/kotlin", "src/jvmMain/kotlin"))
    executionData.setFrom(files(layout.buildDirectory.file("jacoco/jvmTest.exec")))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val jacocoTestCoverageVerification = tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    description = "Verifies code coverage metrics against thresholds."
    dependsOn(jacocoTestReport)
    executionData.setFrom(files(layout.buildDirectory.file("jacoco/jvmTest.exec")))
    classDirectories.setFrom(files(tasks.named("compileKotlinJvm")))
    violationRules {
        rule {
            limit {
                minimum = "0.93".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(jacocoTestCoverageVerification)
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
    autoCorrect = true
    source.setFrom(files("src/commonMain/kotlin", "src/jvmMain/kotlin", "src/iosMain/kotlin"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude("**/build/generated/**")
}

dependencies {
    detektPlugins(libs.detekt.formatting)
}

tasks.register("generateSpmManifest") {
    group = "publishing"
    description = "Zips SuperkassaReceiptRenderer XCFramework, calculates SHA-256 and writes Package.swift"
    dependsOn("assembleSuperkassaReceiptRendererReleaseXCFramework")

    doLast {
        val versionStr = project.version.toString()
        val repoUrl = "https://github.com/texport/superkassa-receipt-renderer"
        val zipName = "SuperkassaReceiptRenderer.xcframework.zip"
        val outputDir = layout.buildDirectory.dir("XCFrameworks/release").get().asFile
        val xcframeworkDir = File(outputDir, "SuperkassaReceiptRenderer.xcframework")
        val zipFile = File(outputDir, zipName)

        if (!xcframeworkDir.exists()) {
            throw GradleException("XCFramework not found at ${xcframeworkDir.absolutePath}")
        }

        // 1. Zipping XCFramework
        println("Zipping XCFramework to ${zipFile.absolutePath}...")
        zipFile.delete()
        ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
            xcframeworkDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(xcframeworkDir.parentFile).path
                    zos.putNextEntry(ZipEntry(relativePath))
                    file.inputStream().buffered().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }

        // 2. Compute SHA-256
        println("Computing SHA-256 checksum...")
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(zipFile).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
        }
        val checksumBytes = digest.digest()
        val checksum = checksumBytes.joinToString("") { "%02x".format(it) }
        println("SHA-256: $checksum")

        // 3. Write Package.swift
        val packageSwiftFile = rootProject.file("Package.swift")
        println("Writing Package.swift to ${packageSwiftFile.absolutePath}...")
        packageSwiftFile.writeText(
            """
            // swift-tools-version:5.5
            import PackageDescription

            let package = Package(
                name: "SuperkassaReceiptRenderer",
                platforms: [
                    .iOS(.v15)
                ],
                products: [
                    .library(
                        name: "SuperkassaReceiptRenderer",
                        targets: ["SuperkassaReceiptRenderer"]
                    ),
                ],
                dependencies: [],
                targets: [
                    .binaryTarget(
                        name: "SuperkassaReceiptRenderer",
                        url: "$repoUrl/releases/download/v$versionStr/$zipName",
                        checksum: "$checksum"
                    )
                ]
            )
            """.trimIndent() + "\n"
        )
        println("SPM manifest generation complete for version $versionStr!")
    }
}
