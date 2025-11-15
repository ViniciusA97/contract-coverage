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
./gradlew run --args="--source-code-dir <code-path> --pact-path <pact-file> [options]"

# Using the JAR file
java -jar build/libs/contract-coverage-1.0-SNAPSHOT.jar --source-code-dir <code-path> --pact-path <pact-file> [options]

# Using native binary (jpackage)
./contract-coverage --source-code-dir <code-path> --pact-path <pact-file> [options]
# Or: ./build/jpackage-distribution/contract-coverage/bin/contract-coverage --source-code-dir <code-path> --pact-path <pact-file> [options]
```

### Required Options

- `-s, --source-code-dir=<path>` - Path to the Java source code directory to analyze (required)
- `-p, --pact-path=<path>` - Path to the Pact directory containing JSON files (required)

### Optional Options

- `-o, --output=<path>` - Output path for the coverage report (default: `./reports/report.json`)
- `-h, --help` - Show help message
- `-V, --version` - Print version information

### Examples

```bash
# Analyze code and compare with all Pact files in directory
./gradlew run --args="--source-code-dir src/main/java --pact-path src/test/resources/pacts"

# Using short options
./gradlew run --args="-s src/main/java -p src/test/resources/pacts"

# Specify custom output path
./gradlew run --args="--source-code-dir src/main/java --pact-path ./pacts --output ./output/coverage.json"

# Using native binary (jpackage)
./contract-coverage -s src/main/java -p ./pacts -o ./reports/coverage.json

./contract-coverage --source-code-dir ../contract-example/src/main/java/org/example/contractexample/ --pact-path ../contract-example/build/pacts

# Show help
./gradlew run --args="--help"
```

**Note:** The `--pact-path` option accepts a directory path. All JSON files in that directory will be read and their endpoints will be combined for comparison with the code endpoints.

### Building

```bash
# Build the project
./gradlew build

# Create executable JAR
./gradlew jar
```

The JAR file will be created at `build/libs/contract-coverage-1.0-SNAPSHOT.jar`.

### Building Native Binary Distribution (No System Java Required)

To create a native binary distribution that includes a bundled JRE (no system Java required), we use **jpackage** (available in Java 14+).

#### Prerequisites

1. **Java 14 or higher** (Java 20 recommended, same as compilation)
   ```bash
   java -version  # Should show Java 14+
   ```

2. **jpackage tool** (included with Java 14+)
   ```bash
   jpackage --version  # Should show jpackage version
   ```

#### Build Native Binary Distribution

```bash
# Build the distribution with bundled JRE
./gradlew jpackage
```

The distribution will be created at:
- `build/jpackage-distribution/contract-coverage/` (Linux/Mac/Windows)

A symlink will be automatically created in the project root:
- `./contract-coverage` → `build/jpackage-distribution/contract-coverage/bin/contract-coverage`

#### Distribution Structure

```
build/jpackage-distribution/contract-coverage/
├── bin/
│   └── contract-coverage          ← Executable launcher
└── lib/
    ├── app/
    │   └── contract-coverage-1.0-SNAPSHOT.jar  ← Application JAR
    └── runtime/                   ← Bundled JRE (~149MB)
```

**Total size:** ~170MB (includes bundled JRE)

#### Usage

Once built, you can run the binary directly:

```bash
# Using the symlink (recommended)
./contract-coverage --source-code-dir <code-path> --pact-path <pact-file>

# Or using the full path
./build/jpackage-distribution/contract-coverage/bin/contract-coverage --source-code-dir <code-path> --pact-path <pact-file>
```

**Benefits of jpackage Distribution:**
- ✅ No system Java required (JRE is bundled)
- ✅ Self-contained distribution (single folder)
- ✅ Works on any Linux system (same architecture)
- ✅ Easy to distribute (just copy the folder)
- ✅ Compatible with Spoon/JDT (full JVM available)

#### Distributing the Binary

To distribute the application, copy the entire `contract-coverage` folder:

```bash
# Copy the distribution folder
cp -r build/jpackage-distribution/contract-coverage /path/to/distribute/

# Users can run it directly
/path/to/distribute/contract-coverage/bin/contract-coverage --help
```

**Note:** The entire folder must be distributed as it contains the bundled JRE. The distribution is self-contained and does not require Java to be installed on the target system.

#### Troubleshooting

If you encounter issues building the distribution:

1. **Verify Java version:**
   ```bash
   java -version  # Should be Java 14+
   ```

2. **Check if jpackage is available:**
   ```bash
   jpackage --version
   ```

3. **Build with verbose output:**
   ```bash
   ./gradlew jpackage --info
   ```

4. **Common issues:**
   - Java version mismatch: Ensure the Java version used for compilation matches the one used by jpackage (Java 20 recommended)
   - Missing jpackage: Install a JDK 14+ that includes jpackage
   - Permission errors: Ensure the build directory is writable

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