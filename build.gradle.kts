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
    
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    jvmToolchain(17)

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
}

publishing {
    publications.withType<MavenPublication>().configureEach {
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
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
}
