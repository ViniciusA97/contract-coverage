# Contract Coverage

**Contract Coverage** is a command-line tool that analyzes Java source code to extract REST API calls made using Spring's `RestTemplate` and compares them with Pact contract definitions to measure contract coverage.

## What is Contract Coverage?

Contract Coverage helps you ensure that your Java application's REST API calls are properly covered by Pact contracts. It:

- **Scans** your Java source code to find all `RestTemplate` API calls
- **Extracts** endpoint paths and HTTP methods from your code
- **Compares** extracted endpoints with your Pact contract definitions
- **Calculates** coverage percentage and identifies missing endpoints
- **Generates** detailed JSON reports with coverage metrics

This tool is particularly useful for:
- Validating that all API calls in your codebase have corresponding Pact contracts
- Identifying endpoints that need contract coverage
- Ensuring contract testing completeness in CI/CD pipelines
- Tracking contract coverage over time

## Installation

### Option 1: Using the Pre-built Binary (Recommended)

Download the self-contained distribution from the releases page. The distribution includes a bundled JRE, so no Java installation is required on the target system.

### Option 2: Building from Source

#### Prerequisites

- Java 20 or higher (JDK)
- Gradle (or use the included Gradle wrapper)

#### Build Steps

```bash
# Clone the repository
git clone <repository-url>
cd contract-coverage

# Build the JAR
./gradlew jar

# Or build the self-contained distribution
./gradlew jpackage
```

The JAR will be created at `build/libs/contract-coverage-1.0-SNAPSHOT.jar`.

For a self-contained distribution (includes bundled JRE), use:
```bash
./gradlew jpackage
```

The distribution will be created at `build/jpackage-distribution/contract-coverage/`.

## Usage

### Basic Command

```bash
contract-coverage --source-code-dir <code-path> --pact-path <pact-dir>
```

### Command-Line Options

#### Required Options

| Option | Short | Description |
|--------|-------|-------------|
| `--source-code-dir` | `-s` | Path to the Java source code directory to analyze |
| `--pact-path` | `-p` | Path to the directory containing Pact JSON files |

#### Optional Options

| Option | Short | Description | Default |
|--------|-------|-------------|---------|
| `--output` | `-o` | Output path for the coverage report | `./reports/report.json` |
| `--threshold` | `-t` | Minimum coverage percentage required (0-100). Fails if coverage is below threshold | None |
| `--dry-run` | `-d` | Run in dry-run mode (always returns exit code 0, even on errors) | `false` |
| `--help` | `-h` | Show help message | - |
| `--version` | `-V` | Print version information | - |

### Examples

#### Basic Analysis

```bash
# Analyze code and compare with Pact contracts
./contract-coverage -s src/main/java -p src/test/resources/pacts

# Using long options
./contract-coverage --source-code-dir src/main/java --pact-path src/test/resources/pacts
```

#### Custom Output Location

```bash
./contract-coverage -s src/main/java -p ./pacts -o ./output/coverage.json
```

#### With Coverage Threshold

```bash
# Require at least 80% coverage (fails if below threshold)
./contract-coverage -s src/main/java -p ./pacts --threshold 80

# If coverage >= 80%: Exit code 0 (success)
# If coverage < 80%: Exit code 1 (failure)
```

#### Dry-Run Mode

```bash
# Run analysis but always return exit code 0 (useful for CI/CD)
./contract-coverage -s src/main/java -p ./pacts --dry-run
```

#### Using the JAR File

```bash
java -jar contract-coverage-1.0-SNAPSHOT.jar -s src/main/java -p ./pacts
```

#### Using Gradle

```bash
./gradlew run --args="-s src/main/java -p src/test/resources/pacts"
```

## Output

### Console Output

When you run Contract Coverage, you'll see output like this:

```
╔═══════════════════════════════════════════════════════════════╗
║                                                               ║
║                    Contract Coverage                          ║
║                                                               ║
║                     v1.0-SNAPSHOT                             ║
║                                                               ║
╚═══════════════════════════════════════════════════════════════╝

Coverage: 75.00%
Total endpoints: 4
Matched: 3
Missing: 1

Matched endpoints:
  ✓ GET /api/users
  ✓ POST /api/users
  ✓ PUT /api/users/{id}

Missing endpoints:
  ✗ DELETE /api/users/{id}

Report generated at: ./reports/report.json

✓ Threshold met: 75.00% >= 70.00%
```

### JSON Report

The tool generates a detailed JSON report at the specified output path (default: `./reports/report.json`):

```json
{
  "endpoints": [
    {
      "path": "/api/users",
      "method": "GET"
    },
    {
      "path": "/api/users",
      "method": "POST"
    }
  ],
  "coverage": {
    "totalCodeEndpoints": 4,
    "matchedByPact": 3,
    "coveragePercent": 75.0,
    "missingEndpoints": [
      {
        "path": "/api/users/{id}",
        "method": "DELETE"
      }
    ],
    "matchedEndpoints": [
      {
        "path": "/api/users",
        "method": "GET"
      },
      {
        "path": "/api/users",
        "method": "POST"
      },
      {
        "path": "/api/users/{id}",
        "method": "PUT"
      }
    ]
  }
}
```

## Supported RestTemplate Methods

Contract Coverage currently supports the following `RestTemplate` methods:

### HTTP Methods Supported

| Method | RestTemplate Methods |
|--------|---------------------|
| **GET** | `getForEntity()`, `getForObject()`, `exchange()` |
| **POST** | `postForEntity()`, `postForObject()`, `exchange()` |
| **PUT** | `put()`, `exchange()` |
| **PATCH** | `patchForObject()`, `patchForEntity()`, `patch()`, `exchange()` |
| **DELETE** | `delete()`, `exchange()` |

### Example Code Patterns Supported

The tool can extract endpoints from various code patterns:

```java
// Direct method calls
restTemplate.getForObject("/api/users", User.class);
restTemplate.postForEntity("/api/users", user, User.class);
restTemplate.put("/api/users/{id}", user, id);
restTemplate.patchForObject("/api/users/{id}", user, User.class, id);
restTemplate.patchForEntity("/api/users/{id}", user, User.class, id);
restTemplate.patch("/api/users/{id}", user, id);
restTemplate.delete("/api/users/{id}", id);

// Using exchange() method
restTemplate.exchange("/api/users", HttpMethod.GET, null, User.class);
restTemplate.exchange("/api/users/{id}", HttpMethod.DELETE, request, Void.class, id);

// Dynamic URL construction
String baseUrl = "/api";
restTemplate.getForObject(baseUrl + "/users", User.class);

// Path variables
restTemplate.getForObject("/api/users/{id}", User.class, userId);

// Constants and helper methods
restTemplate.exchange(API_ENDPOINTS.USERS, HttpMethod.GET, null, User.class);
```

### Path Variable Matching

The tool supports path variable matching, so these are considered equivalent:

- `/users/{id}` matches `/users/123`
- `/users/{userId}/posts/{postId}` matches `/users/456/posts/789`
- `/api/v1/users/{id}` matches `/api/v1/users/999`

## Exit Codes

The tool returns the following exit codes:

| Exit Code | Meaning |
|-----------|---------|
| `0` | Success - Analysis completed successfully |
| `1` | Software error - Analysis failed or threshold not met |
| `2` | Usage error - Invalid arguments or missing required options |

**Note:** In `--dry-run` mode, the tool always returns exit code `0`, even if errors occur or thresholds are not met.

## Troubleshooting

### Common Issues

#### "No JSON files found in Pact directory"

**Problem:** The Pact directory doesn't contain any `.json` files.

**Solution:** Ensure your Pact directory contains valid Pact JSON files. The tool reads all `.json` files in the specified directory.

#### "Code path does not exist"

**Problem:** The source code directory path is incorrect.

**Solution:** Verify the path to your Java source code directory. Use absolute paths if relative paths don't work.

#### "Threshold not met"

**Problem:** The coverage percentage is below the specified threshold.

**Solution:** 
- Review the missing endpoints in the output
- Add Pact contracts for the missing endpoints
- Or adjust the threshold if appropriate
- Use `--dry-run` to test without failing the build

## Building the Distribution

To create a self-contained distribution with bundled JRE:

```bash
# Using the automated script
./build-binary.sh

# Or manually using Gradle
./gradlew jpackage
```

The distribution will be created at `build/jpackage-distribution/contract-coverage/`.

**Distribution Size:** ~170MB (includes bundled JRE)

**Note:** The entire distribution folder must be copied when distributing the application, as it contains the bundled JRE.

## Requirements

- **Java Source Code:** Java 8+ source files using Spring's `RestTemplate`
- **Pact Files:** Valid Pact JSON contract files (Pact specification v1+)
- **Runtime:** Java 20+ (for building) or the pre-built distribution (no Java required)

## Limitations

- Currently only supports Spring's `RestTemplate` (not WebClient or other HTTP clients)
- Requires Java source code (not compiled bytecode)
- Path variable matching is based on segment count and pattern matching
- Complex URL construction may not be fully resolved in all cases

## Contributing

Contributions are welcome! Please feel free to submit issues or pull requests.

## License

[Add your license information here]

```json
{
  "consumer": {
    "name": "OrderService"
  },
  "provider": {
    "name": "PaymentService"
  },
  "interactions": [
    {
      "description": "retrieve payment by id",
      "providerStates": [
        {
          "name": "a payment with id 10 exists"
        }
      ],
      "request": {
        "method": "GET",
        "path": "/payments/10",
        "headers": {
          "Accept": "application/json"
        }
      },
      "response": {
        "status": 200,
        "headers": {
          "Content-Type": "application/json"
        },
        "body": {
          "id": 10,
          "status": "CONFIRMED",
          "amount": 150.0
        }
      }
    }
  ],
  "metadata": {
    "pactSpecification": {
      "version": "4.0"
    }
  }
}
```