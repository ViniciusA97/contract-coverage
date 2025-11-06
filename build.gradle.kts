plugins {
    kotlin("jvm") version "2.1.10"
    application
    id("org.graalvm.buildtools.native") version "0.10.2"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("fr.inria.gforge.spoon:spoon-core:11.2.0")
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

// GraalVM Native Image Configuration
graalvmNative {
    binaries {
        named("main") {
            imageName.set("contract-coverage")
            debug.set(false)
            verbose.set(false)
            
            // Build options for native image
            buildArgs.add("--no-fallback")
            buildArgs.add("--enable-url-protocols=http,https")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            
            // Enable monitoring (replaces deprecated AllowVMInspection)
            buildArgs.add("--enable-monitoring=heapdump,jvmstat")
            
            // Runtime options
            runtimeArgs.add("-XX:+UseG1GC")
        }
    }
    
    // Toolchain configuration
    toolchainDetection.set(true)
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

// Task to create native binary distribution
tasks.register("createNativeDistribution") {
    description = "Creates a distribution with native binary (no JVM required)"
    group = "distribution"
    dependsOn("nativeCompile")
    
    doLast {
        val distDir = file("build/native-distribution")
        distDir.mkdirs()
        
        // Find the native binary - it could be in different locations
        val possiblePaths = listOf(
            file("build/native/nativeCompile/contract-coverage"),
            file("build/native/nativeCompile/contract-coverage.exe"),
            file("build/native/nativeCompile/main/contract-coverage"),
            file("build/native/nativeCompile/main/contract-coverage.exe")
        )
        
        val nativeBinary = possiblePaths.firstOrNull { it.exists() }
        
        if (nativeBinary != null) {
            copy {
                from(nativeBinary)
                into(distDir)
                fileMode = 0b111101101 // 755
            }
            
            println("\n✓ Native binary created successfully!")
            println("✓ Binary location: ${nativeBinary.absolutePath}")
            println("✓ Distribution location: ${distDir.absolutePath}")
            println("\nTo use the native binary:")
            println("  ${distDir.absolutePath}/${nativeBinary.name} <code-path> <pact-file>")
            println("\nNote: No JVM required to run this binary!")
        } else {
            println("⚠ Native binary not found. Make sure GraalVM is installed and nativeCompile task succeeded.")
            println("Searched in:")
            possiblePaths.forEach { println("  - ${it.absolutePath}") }
        }
    }
}