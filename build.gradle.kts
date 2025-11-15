plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("fr.inria.gforge.spoon:spoon-core:11.2.2-beta-12")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("info.picocli:picocli:4.7.5")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.example.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    // Exclude signature files to avoid JAR signing issues
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}

application {
    mainClass.set("org.example.MainKt")
    applicationName = "contract-coverage"
}

// Task to create a standalone executable script in the project root
tasks.register<Copy>("createExecutable") {
    description = "Creates standalone executable scripts in the project root"
    group = "distribution"
    
    from("src/main/resources/contract-coverage.sh") {
        into(".")
        fileMode = 0b111101101 // 755 in octal
    }
    from("src/main/resources/contract-coverage.bat") {
        into(".")
    }
    
    // Copy JAR to bin directory
    from(tasks.jar) {
        into("bin")
    }
    
    doLast {
        println("✓ Executable scripts created in project root")
        println("✓ Run: ./contract-coverage.sh --help")
        println("✓ Or: ./contract-coverage.bat --help (Windows)")
    }
}

// Task to create a complete distribution
tasks.register("createDistribution") {
    description = "Creates a complete distribution with executable and JAR"
    group = "distribution"
    dependsOn("build", "createExecutable")
    
    doLast {
        println("\n✓ Distribution created successfully!")
        println("\nTo use the executable:")
        println("  Linux/Mac: ./contract-coverage.sh <code-path> <pact-file>")
        println("  Windows:   contract-coverage.bat <code-path> <pact-file>")
        println("\nOr use the JAR directly:")
        println("  java -jar bin/contract-coverage-1.0-SNAPSHOT.jar <code-path> <pact-file>")
    }
}

// Task to create custom JRE using jlink (requires Java 9+)
tasks.register<JavaExec>("jlink") {
    description = "Creates a custom JRE with only required modules using jlink"
    group = "distribution"
    dependsOn("jar")
    
    val jlinkDir = file("build/jlink-distribution")
    val jreDir = file("$jlinkDir/jre")
    
    doFirst {
        jlinkDir.mkdirs()
    }
    
    // Use jlink from the current Java installation
    val javaHome = System.getProperty("java.home")
    val jlinkExecutable = if (System.getProperty("os.name").lowercase().contains("win")) {
        "$javaHome/bin/jlink.exe"
    } else {
        "$javaHome/bin/jlink"
    }
    
    executable = jlinkExecutable
    args = listOf(
        "--module-path", "$javaHome/jmods",
        "--add-modules", "ALL-MODULE-PATH",
        "--strip-debug",
        "--compress", "2",
        "--no-header-files",
        "--no-man-pages",
        "--output", jreDir.absolutePath
    )
    
    doLast {
        // Copy JAR to distribution
        copy {
            from(tasks.jar)
            into(jlinkDir)
        }
        
        // Create launcher script
        val launcherScript = file("$jlinkDir/contract-coverage")
        val jarName = tasks.jar.get().archiveFileName.get()
        val scriptContent = """
            |#!/bin/bash
            |SCRIPT_DIR="$(cd "$(dirname "${'$'}{BASH_SOURCE[0]}")" && pwd)"
            |"${'$'}SCRIPT_DIR/jre/bin/java" -jar "${'$'}SCRIPT_DIR/$jarName" "${'$'}@"
        """.trimMargin()
        launcherScript.writeText(scriptContent)
        launcherScript.setExecutable(true)
        
        println("\n✓ Custom JRE created successfully!")
        println("✓ JRE location: ${jreDir.absolutePath}")
        println("✓ Distribution location: ${jlinkDir.absolutePath}")
        println("\nTo use:")
        println("  ${jlinkDir.absolutePath}/contract-coverage <code-path> <pact-file>")
        println("\nNote: Includes custom JRE, no system Java required!")
    }
}

// Task to create native package using jpackage (requires Java 14+)
tasks.register<Exec>("jpackage") {
    description = "Creates a native package/installer using jpackage"
    group = "distribution"
    dependsOn("jar")
    
    val jpackageDir = file("build/jpackage-distribution")
    
    doFirst {
        // Clean previous jpackage output
        val existingApp = file("${jpackageDir.absolutePath}/contract-coverage")
        if (existingApp.exists()) {
            existingApp.deleteRecursively()
        }
        jpackageDir.mkdirs()
    }
    
    // Use jpackage from the Java toolchain (same version used for compilation)
    val javaToolchain = javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(20))
    }.get()
    val javaHome = javaToolchain.metadata.installationPath.asFile.absolutePath
    val jpackageExecutable = if (System.getProperty("os.name").lowercase().contains("win")) {
        "$javaHome/bin/jpackage.exe"
    } else {
        "$javaHome/bin/jpackage"
    }
    
    commandLine(
        jpackageExecutable,
        "--name", "contract-coverage",
        "--input", "build/libs",
        "--main-jar", tasks.jar.get().archiveFileName.get(),
        "--main-class", "org.example.MainKt",
        "--type", "app-image",
        "--dest", jpackageDir.absolutePath,
        "--java-options", "-Xmx512m"
    )
    
    doLast {
        val targetBinary = file("${jpackageDir.absolutePath}/contract-coverage/bin/contract-coverage")
        if (targetBinary.exists()) {
            // Create a symlink in project root for convenience (Linux/Mac only)
            if (!System.getProperty("os.name").lowercase().contains("win")) {
                val symlink = file("contract-coverage")
                // Force remove if exists (even if it's a regular file or symlink)
                if (symlink.exists()) {
                    symlink.delete()
                }
                try {
                    // Use relative path for symlink
                    val relativePath = targetBinary.relativeTo(symlink.parentFile).path
                    // Ensure we're in the right directory and create symlink
                    val result = exec {
                        commandLine("ln", "-sfn", relativePath, symlink.name)
                        workingDir = symlink.parentFile
                        isIgnoreExitValue = false
                    }
                    println("\n✓ Native package created successfully!")
                    println("✓ Package location: ${jpackageDir.absolutePath}")
                    println("✓ Symlink created: ./contract-coverage -> ${relativePath}")
                    println("\nTo use:")
                    println("  ./contract-coverage <code-path> <pact-file>")
                    println("  Or: ${jpackageDir.absolutePath}/contract-coverage/bin/contract-coverage <code-path> <pact-file>")
                    println("\nNote: Includes bundled JRE, no system Java required!")
                } catch (e: Exception) {
                    println("\n✓ Native package created successfully!")
                    println("✓ Package location: ${jpackageDir.absolutePath}")
                    println("⚠ Could not create symlink: ${e.message}")
                    println("\nTo use:")
                    println("  ${jpackageDir.absolutePath}/contract-coverage/bin/contract-coverage <code-path> <pact-file>")
                    println("\nNote: Includes bundled JRE, no system Java required!")
                }
            } else {
                println("\n✓ Native package created successfully!")
                println("✓ Package location: ${jpackageDir.absolutePath}")
                println("\nTo use:")
                println("  ${jpackageDir.absolutePath}/contract-coverage/bin/contract-coverage <code-path> <pact-file>")
                println("\nNote: Includes bundled JRE, no system Java required!")
            }
        } else {
            println("\n✓ Native package created successfully!")
            println("✓ Package location: ${jpackageDir.absolutePath}")
            println("\nTo use:")
            println("  ${jpackageDir.absolutePath}/contract-coverage/bin/contract-coverage <code-path> <pact-file>")
            println("\nNote: Includes bundled JRE, no system Java required!")
        }
    }
}