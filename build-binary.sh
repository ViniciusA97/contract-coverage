#!/bin/bash

# Script to generate native binary distribution for contract-coverage
# This script checks prerequisites and creates a self-contained distribution using jpackage

set -e  # Exit on first error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored messages
print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

# Check if we're in the correct directory
if [ ! -f "build.gradle.kts" ]; then
    print_error "build.gradle.kts not found!"
    print_info "Please run this script from the project root directory"
    exit 1
fi

print_header "Contract Coverage - Native Distribution Build"

# 1. Check if Java is installed
print_info "Checking Java..."
if ! command -v java &> /dev/null; then
    print_error "Java not found in PATH"
    print_info "Please install Java 14 or higher (JDK recommended)"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1)
print_success "Java found: $JAVA_VERSION"

# 2. Check Java version (jpackage requires Java 14+)
print_info "Checking Java version..."
JAVA_MAJOR_VERSION=$(java -version 2>&1 | head -n 1 | sed -E 's/.*version "([0-9]+).*/\1/')
if [ "$JAVA_MAJOR_VERSION" -lt 14 ]; then
    print_error "Java 14 or higher is required for jpackage!"
    print_info "Current version: Java $JAVA_MAJOR_VERSION"
    print_info "Please install Java 14 or higher (Java 20 recommended)"
    exit 1
else
    print_success "Java version OK: $JAVA_MAJOR_VERSION"
fi

# 3. Check if jpackage is available
print_info "Checking jpackage..."
if ! command -v jpackage &> /dev/null; then
    # Try to find jpackage in JAVA_HOME
    if [ -n "$JAVA_HOME" ] && [ -f "$JAVA_HOME/bin/jpackage" ]; then
        print_success "jpackage found in JAVA_HOME: $JAVA_HOME/bin/jpackage"
    else
        print_error "jpackage not found!"
        print_info "jpackage is included with JDK 14+. Make sure you have a JDK (not just JRE) installed."
        print_info "If using JAVA_HOME, ensure it points to a JDK installation."
        exit 1
    fi
else
    JPACKAGE_VERSION=$(jpackage --version 2>&1 | head -n 1)
    print_success "jpackage found: $JPACKAGE_VERSION"
fi

# 4. Check if Gradle is available
print_info "Checking Gradle..."
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
    print_success "Gradle wrapper found"
elif command -v gradle &> /dev/null; then
    GRADLE_CMD="gradle"
    print_success "Gradle found in PATH"
else
    print_error "Gradle not found!"
    print_info "Please use gradlew or install Gradle"
    exit 1
fi

# 5. Clean previous build (optional)
print_info "Do you want to clean the previous build? (y/N)"
read -p "> " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_info "Cleaning previous build..."
    $GRADLE_CMD clean
    print_success "Build cleaned!"
fi

# 6. Build JAR first
print_header "Building JAR"
print_info "Building application JAR..."
if $GRADLE_CMD jar; then
    print_success "JAR built successfully!"
else
    print_error "JAR build failed!"
    print_info "Please check the errors above"
    exit 1
fi

# 7. Create distribution with jpackage
print_header "Creating Distribution with jpackage"
print_info "This may take a few minutes..."
print_info "Creating self-contained distribution (includes bundled JRE)..."
print_info ""

if $GRADLE_CMD jpackage; then
    print_success "Distribution created successfully!"
else
    print_error "Distribution creation failed!"
    print_info "Please check the errors above"
    exit 1
fi

# 8. Verify generated distribution
print_header "Verifying Generated Distribution"

DISTRIBUTION_DIR="build/jpackage-distribution/contract-coverage"
BINARY_PATH="$DISTRIBUTION_DIR/bin/contract-coverage"
SYMLINK_PATH="./contract-coverage"

if [ -d "$DISTRIBUTION_DIR" ] && [ -f "$BINARY_PATH" ]; then
    print_success "Distribution found: $DISTRIBUTION_DIR"
    echo ""
    
    # Distribution information
    DISTRIBUTION_SIZE=$(du -sh "$DISTRIBUTION_DIR" | cut -f1)
    print_info "Distribution size: $DISTRIBUTION_SIZE"
    
    # Check if binary is executable
    if [ -x "$BINARY_PATH" ]; then
        print_success "Binary is executable"
    else
        print_warning "Binary is not executable, adding permission..."
        chmod +x "$BINARY_PATH"
        print_success "Execution permission added"
    fi
    
    # Check for symlink
    if [ -L "$SYMLINK_PATH" ] || [ -f "$SYMLINK_PATH" ]; then
        print_success "Symlink found: $SYMLINK_PATH"
    else
        print_info "Symlink not found (this is OK, you can use the full path)"
    fi
    
    # Test execution
    print_info "Testing binary execution..."
    if "$BINARY_PATH" --help &> /dev/null; then
        print_success "Binary works correctly!"
    else
        print_warning "Binary found but did not respond to --help"
    fi
    
    echo ""
    print_header "Distribution Generated Successfully!"
    echo ""
    print_success "Distribution location: $DISTRIBUTION_DIR"
    echo ""
    print_info "To use the distribution:"
    if [ -L "$SYMLINK_PATH" ] || [ -f "$SYMLINK_PATH" ]; then
        echo "  ./contract-coverage --source-code-dir <code-path> --pact-path <pact-dir>"
    fi
    echo "  $BINARY_PATH --source-code-dir <code-path> --pact-path <pact-dir>"
    echo ""
    print_info "Example:"
    echo "  $BINARY_PATH --source-code-dir src/main/java --pact-path src/test/resources/pacts"
    echo ""
    print_info "Note: This distribution includes a bundled JRE (~170MB total)"
    print_info "      No system Java installation required on target system!"
    echo ""
    
else
    print_error "Distribution not found!"
    print_info "Expected location: $DISTRIBUTION_DIR"
    print_info "Expected binary: $BINARY_PATH"
    exit 1
fi

print_header "Build Completed!"

