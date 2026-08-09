import org.gradle.kotlin.dsl.support.unzipTo
//import org.jetbrains.dokka.versioning.VersioningConfiguration
//import org.jetbrains.dokka.versioning.VersioningPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.HttpURLConnection
import java.io.ByteArrayOutputStream
import java.net.URI

//buildscript {
//    dependencies {
//        classpath(libs.versioning)
//    }
//}

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.serialization)
    alias(libs.plugins.loom)
    alias(libs.plugins.dokka)
//    alias(libs.plugins.validator)
//    alias(libs.plugins.ksp)
}

//if (!project.hasProperty("full")) {
//    project.gradle.startParameter.excludedTaskNames.add("kspKotlin")
//}

version = property("mod_version").toString()

repositories {
    maven("https://jitpack.io")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://maven.terraformersmc.com/releases")
    maven("https://repo.essential.gg/repository/maven-public")
}

dependencies {
    // To change the versions see the gradle/libs.versions.toml
    minecraft(libs.minecraft)
    implementation(libs.bundles.fabric)

    implementation(libs.bundles.included) { include(this) }
    implementation(include("gg.essential:vigilance:314")!!)
    implementation(include("gg.essential:elementa:750")!!)
    implementation(include("gg.essential:universalcraft-26.2-fabric:511")!!)

//    modApi(libs.modmenu)
//    modRuntimeOnly(libs.devauth)
    dokkaPlugin(libs.versioning)

    implementation(kotlin("stdlib-jdk8"))
//    implementation(project(":typing-generator"))
//    ksp(project(":typing-generator"))
}

loom {
    accessWidenerPath.set(file("src/main/resources/ctjs.accesswidener"))
}

base {
    archivesName.set(property("archives_base_name") as String)
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

//apiValidation {
//    validationDisabled = true
//    ignoredProjects += "typing-generator"
//    ignoredPackages += "com.chattriggers.ctjs.internal"
//}

tasks {
    processResources {
        val flkVersion = libs.versions.fabric.kotlin.get()
        val fapiVersion = libs.versions.fabric.api.get()
        val loaderVersion = libs.versions.loader.get()

        inputs.property("version", project.version)
        inputs.property("fabric_kotlin_version", flkVersion)
        inputs.property("fabric_api_version", fapiVersion)
        inputs.property("loader_version", loaderVersion)

        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version,
                "fabric_kotlin_version" to flkVersion,
                "fabric_api_version" to fapiVersion,
                "loader_version" to loaderVersion
            )
        }
    }

    withType<JavaCompile>().configureEach {
        options.release.set(25)
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
            freeCompilerArgs = listOf("-Xcontext-parameters")
        }
    }

    jar {
        from("LICENSE") {
            rename { "${name}_${base.archivesName.get()}" }
        }
    }

    dokkaHtml {
        // Just use the module name here since the MC version doesn't affect CT's API
        // across the same mod version
        moduleVersion.set(project.version.toString())
        moduleName.set("ctjs")

        val docVersionsDir = projectDir.resolve("build/javadocs")
        val currentVersion = project.version.toString()
        val currentDocsDir = docVersionsDir.resolve(currentVersion)
        outputs.upToDateWhen { docVersionsDir.exists() }

        outputDirectory.set(file(currentDocsDir))

//        pluginConfiguration<VersioningPlugin, VersioningConfiguration> {
//            version = project.version.toString()
//            olderVersionsDir = docVersionsDir
//            renderVersionsNavigationOnAllPages = true
//        }

        suppressObviousFunctions.set(true)
        suppressInheritedMembers.set(true)

        val branch = getBranch()
        dokkaSourceSets {
            configureEach {
                jdkVersion.set(25)

                perPackageOption {
                    matchingRegex.set("com\\.chattriggers\\.ctjs\\.internal(\$|\\.).*")
                    suppress.set(true)
                }

                sourceLink {
                    localDirectory.set(file("src/main/kotlin"))
                    remoteUrl.set(URI.create("https://github.com/ChatTriggers/ctjs/blob/$branch/src/main/kotlin").toURL())
                    remoteLineSuffix.set("#L")
                }

                // maybe add mcsrc link to the version
//                externalDocumentationLink {
//                    val yarnVersion = libs.versions.yarn.get()
//
//                    url.set(URI.create("https://maven.fabricmc.net/docs/yarn-$yarnVersion/").toURL())
//                    packageListUrl.set(URI.create("https://maven.fabricmc.net/docs/yarn-$yarnVersion/element-list").toURL())
//                }
            }
        }

        doFirst {
            val archiveBase = "https://www.chattriggers.com/javadocs-archive/"
            val versions = String(downloadFile(archiveBase + "versions")).lines().map(String::trim)
            val tmpFile = File(temporaryDir, "oldVersionsZip.zip")

            versions.filter(String::isNotEmpty).map(String::trim).forEach { version ->
                val zipBytes = downloadFile("$archiveBase$version.zip")
                tmpFile.writeBytes(zipBytes)
                unzipTo(docVersionsDir, tmpFile)
            }

            tmpFile.delete()
        }

        doLast {
            // At this point we have a structure that looks something like this:
            // javadocs
            //   \-- 2.2.0-1.8.9
            //   \-- 3.0.0
            //         \-- older
            //
            // The "older" directory contains all old versions, so we want to
            // delete the top-level older versions and move everything inside the
            // latest directory to the top level so the GitHub actions workflow
            // doesn't need to figure out the correct version name

            docVersionsDir.listFiles()?.forEach {
                if (it.name != version)
                    it.deleteRecursively()
            }

            val latestVersionDir = docVersionsDir.listFiles()!!.single()
            latestVersionDir.listFiles()!!.forEach {
                it.renameTo(File(it.parentFile.parentFile, it.name))
            }
            latestVersionDir.deleteRecursively()
        }
    }
}

fun downloadFile(url: String): ByteArray {
    return (URI.create(url).toURL().openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        doOutput = true
    }.inputStream.readAllBytes()
}

fun getBranch(): String {
    val stdout = ByteArrayOutputStream()
    providers.exec {
        commandLine("git", "rev-parse", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}