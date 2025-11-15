#!/bin/bash

# Script to generate native binary for contract-coverage
# This script checks prerequisites and compiles the native binary using GraalVM

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

print_header "Contract Coverage - Native Binary Build"

# 1. Check if Java is installed
print_info "Checking Java..."
if ! command -v java &> /dev/null; then
    print_error "Java not found in PATH"
    print_info "Please install Java/GraalVM"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1)
print_success "Java found: $JAVA_VERSION"

# 2. Check if it's GraalVM
print_info "Checking if it's GraalVM..."
if ! java -version 2>&1 | grep -qi "graalvm"; then
    print_warning "Does not appear to be GraalVM!"
    print_info "Current version: $JAVA_VERSION"
    print_info ""
    print_info "To generate native binary, you need GraalVM:"
    print_info "  1. Install GraalVM: https://www.graalvm.org/downloads/"
    print_info "  2. Or use SDKMAN: sdk install java 23.0.2-graalce"
    print_info "  3. Configure JAVA_HOME to point to GraalVM"
    print_info ""
    read -p "Do you want to continue anyway? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    print_success "GraalVM detected!"
fi

# 3. Check if native-image is installed
print_info "Checking native-image..."
if ! command -v native-image &> /dev/null; then
    print_warning "native-image not found!"
    print_info "Installing native-image..."
    
    if command -v gu &> /dev/null; then
        gu install native-image
        print_success "native-image installed!"
    else
        print_error "Tool 'gu' not found!"
        print_info "Please install native-image manually:"
        print_info "  gu install native-image"
        exit 1
    fi
else
    NATIVE_IMAGE_VERSION=$(native-image --version 2>&1 | head -n 1)
    print_success "native-image found: $NATIVE_IMAGE_VERSION"
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

# 6. Compile native binary
print_header "Compiling Native Binary"
print_info "This may take several minutes..."
print_info ""

# Execute nativeCompile
if $GRADLE_CMD nativeCompile --info; then
    print_success "Native compilation completed!"
else
    print_error "Native compilation failed!"
    print_info "Please check the errors above"
    exit 1
fi

# 7. Create distribution
print_header "Creating Distribution"
if $GRADLE_CMD createNativeDistribution; then
    print_success "Distribution created!"
else
    print_warning "createNativeDistribution task failed, but binary may be available"
fi

# 8. Verify generated binary
print_header "Verifying Generated Binary"

BINARY_PATHS=(
    "build/native/nativeCompile/contract-coverage"
    "build/native/nativeCompile/main/contract-coverage"
    "build/native-distribution/contract-coverage"
)

BINARY_FOUND=""
for path in "${BINARY_PATHS[@]}"; do
    if [ -f "$path" ]; then
        BINARY_FOUND="$path"
        break
    fi
done

if [ -n "$BINARY_FOUND" ]; then
    print_success "Binary found: $BINARY_FOUND"
    echo ""
    
    # Binary information
    BINARY_SIZE=$(du -h "$BINARY_FOUND" | cut -f1)
    print_info "Size: $BINARY_SIZE"
    
    # Check if it's executable
    if [ -x "$BINARY_FOUND" ]; then
        print_success "Binary is executable"
    else
        print_warning "Binary is not executable, adding permission..."
        chmod +x "$BINARY_FOUND"
        print_success "Execution permission added"
    fi
    
    # Test execution
    print_info "Testing binary execution..."
    if "$BINARY_FOUND" --help &> /dev/null; then
        print_success "Binary works correctly!"
    else
        print_warning "Binary found but did not respond to --help"
    fi
    
    echo ""
    print_header "Native Binary Generated Successfully!"
    echo ""
    print_success "Location: $BINARY_FOUND"
    echo ""
    print_info "To use the binary:"
    echo "  $BINARY_FOUND <code-path> <pact-file>"
    echo ""
    print_info "Example:"
    echo "  $BINARY_FOUND src/main/java src/test/resources/pacts/contract.json"
    echo ""
    print_info "Note: This binary does NOT require JVM to run!"
    echo ""
    
else
    print_error "Binary not found!"
    print_info "Checked paths:"
    for path in "${BINARY_PATHS[@]}"; do
        echo "  - $path"
    done
    exit 1
fi

print_header "Build Completed!"

