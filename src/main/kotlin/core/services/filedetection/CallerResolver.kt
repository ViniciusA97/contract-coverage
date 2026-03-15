package org.example.core.services.filedetection

import java.io.File

/**
 * Data class representing a fully qualified class name.
 */
data class QualifiedClass(
    val packageName: String,
    val className: String
) {
    val fullyQualifiedName: String get() = if (packageName.isNotEmpty()) "$packageName.$className" else className
}

/**
 * Resolves callers of detected client files.
 * Finds files that explicitly import the client-containing classes.
 */
class CallerResolver {
    
    /**
     * Find files that might call/use the given client files.
     * Uses import statements to find callers precisely.
     * 
     * @param clientFiles Files that contain HTTP client usage
     * @param allJavaFiles All Java files in the project
     * @param maxDepth Maximum depth of caller chain to follow (default: 1)
     * @return Set of files that call the client files (including the client files themselves)
     */
    fun findCallers(
        clientFiles: List<File>,
        allJavaFiles: List<File>,
        maxDepth: Int = 1
    ): Set<File> {
        val result = clientFiles.toMutableSet()
        var currentLevel = clientFiles.toSet()
        
        repeat(maxDepth) {
            val nextLevel = mutableSetOf<File>()
            
            // Extract fully qualified class names from current level files
            val qualifiedClasses = currentLevel.flatMap { extractQualifiedClasses(it) }.toSet()
            
            if (qualifiedClasses.isEmpty()) return@repeat
            
            // Find files that import these classes
            allJavaFiles.forEach { file ->
                if (file !in result && importsAnyClass(file, qualifiedClasses)) {
                    nextLevel.add(file)
                }
            }
            
            if (nextLevel.isEmpty()) return@repeat
            
            result.addAll(nextLevel)
            currentLevel = nextLevel
        }
        
        return result
    }
    
    /**
     * Extract fully qualified class names from a Java file.
     * Combines package declaration with class/interface/enum declarations.
     */
    private fun extractQualifiedClasses(file: File): List<QualifiedClass> {
        val classes = mutableListOf<QualifiedClass>()
        var packageName = ""
        val classPattern = Regex("""^\s*(?:public\s+)?(?:abstract\s+)?(?:final\s+)?(?:class|interface|enum)\s+(\w+)""")
        val packagePattern = Regex("""^\s*package\s+([\w.]+)\s*;""")
        
        try {
            file.useLines { lines ->
                lines.forEach { line ->
                    // Extract package
                    packagePattern.find(line)?.let { match ->
                        packageName = match.groupValues[1]
                    }
                    
                    // Extract class names
                    classPattern.find(line)?.let { match ->
                        classes.add(QualifiedClass(packageName, match.groupValues[1]))
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore read errors
        }
        
        return classes
    }
    
    /**
     * Check if a file imports or references any of the given classes.
     * Checks:
     * 1. Explicit import statements
     * 2. Same package (no import needed) - only checks classes from that specific package
     */
    private fun importsAnyClass(file: File, qualifiedClasses: Set<QualifiedClass>): Boolean {
        // Build set of import patterns to match
        val importPatterns = qualifiedClasses.flatMap { qc ->
            listOf(
                // Exact import: import com.example.MyClass;
                "import ${qc.fullyQualifiedName};",
                // Wildcard import: import com.example.*;
                "import ${qc.packageName}.*;"
            )
        }.filter { it.isNotBlank() && !it.contains("import ;") && !it.contains("import .*;") }.toSet()
        
        // Group classes by package for same-package detection
        val classesByPackage: Map<String, List<Regex>> = qualifiedClasses
            .filter { it.packageName.isNotEmpty() }
            .groupBy { it.packageName }
            .mapValues { (_, classes) ->
                classes.map { qc -> Regex("""\b${Regex.escape(qc.className)}\b""") }
            }
        
        var filePackage = ""
        var hasImport = false
        var usesClassFromSamePackage = false
        
        return try {
            file.useLines { lines ->
                for (line in lines) {
                    val trimmedLine = line.trim()
                    
                    // Check for package declaration
                    if (trimmedLine.startsWith("package ")) {
                        val packageMatch = Regex("""^package\s+([\w.]+)\s*;""").find(trimmedLine)
                        filePackage = packageMatch?.groupValues?.get(1) ?: ""
                    }
                    
                    // Check for explicit import
                    if (importPatterns.any { pattern -> trimmedLine == pattern }) {
                        hasImport = true
                        break
                    }
                    
                    // Check for same-package class usage
                    // Only check class names that belong to THIS file's package
                    if (filePackage.isNotEmpty()) {
                        val samePackagePatterns = classesByPackage[filePackage]
                        if (samePackagePatterns != null) {
                            if (samePackagePatterns.any { it.containsMatchIn(line) }) {
                                usesClassFromSamePackage = true
                                break
                            }
                        }
                    }
                }
                
                hasImport || usesClassFromSamePackage
            }
        } catch (e: Exception) {
            false
        }
    }
}
