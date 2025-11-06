# Contract Coverage

A tool for analyzing Java code to extract RestTemplate calls and compare them with Pact contracts to measure contract coverage.

## Features

- Analyzes Java source code to extract RestTemplate API calls
- Compares extracted endpoints with Pact contract definitions
- Generates coverage reports in JSON format
- Supports multiple RestTemplate methods (GET, POST, PUT, PATCH, DELETE, exchange)
- Handles path variables matching (e.g., `/users/{id}` matches `/users/999`)

## CLI Usage

The project provides a Command Line Interface (CLI) using Picocli.

### Basic Usage

```bash
# Using Gradle
./gradlew run --args="<code-path> <pact-file> [options]"

# Using the JAR file
java -jar build/libs/contract-coverage-1.0-SNAPSHOT.jar <code-path> <pact-file> [options]
```

### Arguments

- `<code-path>` - Path to the Java source code directory to analyze (required)
- `<pact-file>` - Path to the Pact JSON file (required)

### Options

- `-o, --output=<path>` - Output path for the coverage report (default: `./reports/report.json`)
- `-h, --help` - Show help message
- `-V, --version` - Print version information

### Examples

```bash
# Analyze code and compare with Pact file
./gradlew run --args="src/main/java src/test/resources/pacts/contract.json"

# Specify custom output path
./gradlew run --args="src/main/java src/test/resources/pacts/contract.json -o ./output/coverage.json"

# Show help
./gradlew run --args="--help"
```

### Building

```bash
# Build the project
./gradlew build

# Create executable JAR
./gradlew jar
```

The JAR file will be created at `build/libs/contract-coverage-1.0-SNAPSHOT.jar`.

### Building Native Binary (No JVM Required)

To create a native binary that doesn't require a JVM to run, you'll need GraalVM installed.

#### Prerequisites

1. **Install GraalVM:**
   ```bash
   # Download from https://www.graalvm.org/downloads/
   # Or use SDKMAN:
   sdk install java 22.3.0.r17-grl  # GraalVM Community Edition
   sdk use java 22.3.0.r17-grl
   ```

2. **Install Native Image:**
   ```bash
   gu install native-image
   ```

3. **Verify installation:**
   ```bash
   java -version  # Should show GraalVM
   native-image --version
   ```

#### Build Native Binary

**Opção 1: Usando o script automatizado (recomendado)**

```bash
# Script que verifica pré-requisitos e compila automaticamente
./build-native.sh
```

O script `build-native.sh`:
- ✅ Verifica se GraalVM está instalado
- ✅ Verifica se native-image está instalado
- ✅ Instala native-image automaticamente se necessário
- ✅ Compila o binário nativo
- ✅ Cria a distribuição
- ✅ Testa o binário gerado

**Opção 2: Usando Gradle diretamente**

```bash
# Build native binary
./gradlew nativeCompile

# Or create complete native distribution (recommended)
./gradlew createNativeDistribution
```

The native binary will be created at:
- `build/native/nativeCompile/contract-coverage` (Linux/Mac)
- `build/native/nativeCompile/contract-coverage.exe` (Windows)
- `build/native-distribution/contract-coverage` (after createNativeDistribution)

#### Usage

Once built, you can run the native binary directly without Java:

```bash
# Using the distribution directory (recommended)
./build/native-distribution/contract-coverage <code-path> <pact-file>

# Or directly from build output
./build/native/nativeCompile/contract-coverage <code-path> <pact-file>

# Windows
build\native-distribution\contract-coverage.exe <code-path> <pact-file>
```

**Benefits of Native Binary:**
- ✅ No JVM required to run
- ✅ Faster startup time (instant startup)
- ✅ Lower memory footprint
- ✅ Single executable file
- ✅ Easy to distribute

#### Troubleshooting

If you encounter issues building the native binary:

1. **Verify GraalVM installation:**
   ```bash
   java -version  # Should show GraalVM
   native-image --version
   ```

2. **Check if native-image is installed:**
   ```bash
   gu install native-image
   ```

3. **Build with verbose output:**
   ```bash
   ./gradlew nativeCompile --info
   ```

4. **Common issues:**
   - Missing native-image tool: Install with `gu install native-image`
   - Reflection errors: The configuration files in `src/main/resources/META-INF/native-image/` should handle most cases
   - Missing dependencies: Ensure all required libraries are compatible with GraalVM

## Test Cases

This project contains unit tests to validate the functionality of the `SpoonWrapper` class, which is responsible for analyzing Java source code and extracting API endpoint information from `RestTemplate.exchange` method calls. Below is a detailed description of the test cases and what each one verifies.

### 1. **Test Case: `test3/HttpClient3.java` and `test3/Main.java`**
- **Description**: This test case verifies that the `SpoonWrapper` can correctly extract the endpoint when the `RestTemplate.exchange` method is called directly with hardcoded arguments.
- **Expected Result**: The endpoint extracted should be:
    - **Path**: `/test-3`
    - **Method**: `POST`

### 2. **Test Case: `test5/Main.java`**
- **Description**: This test case ensures that the `SpoonWrapper` can handle cases where the URL is constructed using a constant and concatenation. The `RestTemplate.exchange` method is called with a dynamically constructed URL.
- **Expected Result**: The endpoint extracted should be:
    - **Path**: `/test-5`
    - **Method**: `POST`

### 3. **Test Case: `test6/HttpClient6.java`**
- **Description**: This test case validates that the `SpoonWrapper` can extract endpoint information when the `RestTemplate.exchange` method is called within a private method, and the arguments are defined as local variables.
- **Expected Result**: The endpoint extracted should be:
    - **Path**: `/test-6`
    - **Method**: `POST`

### 4. **Test Case: `test7/HttpClient7.java`**
- **Description**: This test case verifies that the `SpoonWrapper` can extract endpoints when the arguments of the `RestTemplate.exchange` method are passed as parameters to another method.
- **Expected Result**: The endpoint extracted should be:
    - **Path**: `/test-7`
    - **Method**: `GET`

### 5. **Test Case: `test8/HttpClient8.java`**
- **Description**: This test case ensures that the `SpoonWrapper` can handle cases where the URL and method are defined as constants in a separate class.
- **Expected Result**: The endpoint extracted should be:
    - **Path**: `/test-8`
    - **Method**: `PUT`

### 6. **Test Case: `test9/HttpClient9.java`**
- **Description**: This test case validates that the `SpoonWrapper` can process calls to the `RestTemplate.exchange` method where the arguments are obtained from helper methods.
- **Expected Result**: The endpoint extracted should be:
    - **Path**: `/test-9`
    - **Method**: `DELETE`

### 7. **Test Case: `test10/HttpClient10.java`**
- **Description**: This test case ensures that the `SpoonWrapper` can handle multiple calls to the `RestTemplate.exchange` method within the same method.
- **Expected Result**: The endpoints extracted should be:
    - **Path**: `/test-10-a`
    - **Method**: `PATCH`
    - **Path**: `/test-10-b`
    - **Method**: `POST`

## Summary

Each test case is designed to cover a different scenario:
- **Hardcoded arguments** (`test3`).
- **Dynamic URL construction** (`test5`).
- **Local variable usage in private methods** (`test6`).
- **Arguments passed as method parameters** (`test7`).
- **Constants in separate classes** (`test8`).
- **Helper methods for arguments** (`test9`).
- **Multiple calls in the same method** (`test10`).

These tests ensure that the `SpoonWrapper` can handle various ways of defining and passing arguments to the `RestTemplate.exchange` method.