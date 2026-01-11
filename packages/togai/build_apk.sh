#!/bin/bash

################################################################################
# Togai Android APK Build Script
# 
# This script builds the Togai Android application and generates APK files
# for installation on Android devices.
#
# Requirements:
# - Android SDK installed
# - ANDROID_HOME environment variable set
# - Java 11 or higher
# - Gradle 8.0+
################################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="${PROJECT_DIR}/app/build/outputs/apk"
CACHE_DIR="${PROJECT_DIR}/.gradle-cache"

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}Togai Android APK Build Script${NC}"
echo -e "${BLUE}================================${NC}"
echo ""

################################################################################
# Function: Check Prerequisites
################################################################################
check_prerequisites() {
    echo -e "${YELLOW}[1/7] Checking prerequisites...${NC}"
    
    # Check Java
    if ! command -v java &> /dev/null; then
        echo -e "${RED}✗ Java not found. Please install Java 11 or higher.${NC}"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 11 ]; then
        echo -e "${RED}✗ Java 11 or higher required. Found: Java $JAVA_VERSION${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Java $JAVA_VERSION${NC}"
    
    # Check Android SDK
    if [ -z "$ANDROID_HOME" ]; then
        echo -e "${RED}✗ ANDROID_HOME not set. Please set it to your Android SDK path.${NC}"
        echo -e "${YELLOW}  Example: export ANDROID_HOME=/path/to/android-sdk${NC}"
        exit 1
    fi
    
    if [ ! -d "$ANDROID_HOME" ]; then
        echo -e "${RED}✗ ANDROID_HOME directory does not exist: $ANDROID_HOME${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Android SDK: $ANDROID_HOME${NC}"
    
    # Check Android SDK Build Tools
    if [ ! -d "$ANDROID_HOME/build-tools" ]; then
        echo -e "${RED}✗ Android Build Tools not found${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Android Build Tools${NC}"
    
    # Check Gradle wrapper
    if [ ! -f "${PROJECT_DIR}/gradlew" ]; then
        echo -e "${YELLOW}⚠ Gradle wrapper not found. Downloading...${NC}"
        download_gradle_wrapper
    fi
    echo -e "${GREEN}✓ Gradle wrapper${NC}"
    
    echo ""
}

################################################################################
# Function: Download Gradle Wrapper
################################################################################
download_gradle_wrapper() {
    cd "$PROJECT_DIR"
    
    # Create gradle wrapper directory
    mkdir -p gradle/wrapper
    
    # Download gradle wrapper jar
    GRADLE_VERSION="8.5"
    WRAPPER_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
    
    echo -e "${YELLOW}Downloading Gradle ${GRADLE_VERSION}...${NC}"
    
    # Create gradlew script
    cat > gradlew << 'EOF'
#!/bin/sh
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
exec "$GRADLE_USER_HOME/wrapper/dists/gradle-8.5-bin/*/gradle-8.5/bin/gradle" "$@"
EOF
    
    chmod +x gradlew
    
    echo -e "${GREEN}✓ Gradle wrapper created${NC}"
}

################################################################################
# Function: Setup Gradle Cache
################################################################################
setup_gradle_cache() {
    echo -e "${YELLOW}[2/7] Setting up Gradle cache...${NC}"
    
    # Create cache directory
    mkdir -p "$CACHE_DIR"
    
    # Set Gradle user home to cache directory
    export GRADLE_USER_HOME="$CACHE_DIR"
    
    echo -e "${GREEN}✓ Gradle cache: $CACHE_DIR${NC}"
    echo ""
}

################################################################################
# Function: Clean Previous Builds
################################################################################
clean_build() {
    echo -e "${YELLOW}[3/7] Cleaning previous builds...${NC}"
    
    cd "$PROJECT_DIR"
    
    # Clean build directory
    if [ -d "app/build" ]; then
        rm -rf app/build
        echo -e "${GREEN}✓ Removed app/build${NC}"
    fi
    
    # Clean .gradle directory (optional, uncomment for deep clean)
    # if [ -d ".gradle" ]; then
    #     rm -rf .gradle
    #     echo -e "${GREEN}✓ Removed .gradle${NC}"
    # fi
    
    # Run Gradle clean
    ./gradlew clean --no-daemon --warning-mode all || true
    
    echo -e "${GREEN}✓ Build cleaned${NC}"
    echo ""
}

################################################################################
# Function: Download Dependencies
################################################################################
download_dependencies() {
    echo -e "${YELLOW}[4/7] Downloading dependencies...${NC}"
    
    cd "$PROJECT_DIR"
    
    # Download dependencies without building
    ./gradlew --no-daemon dependencies || {
        echo -e "${RED}✗ Failed to download dependencies${NC}"
        exit 1
    }
    
    echo -e "${GREEN}✓ Dependencies downloaded${NC}"
    echo ""
}

################################################################################
# Function: Build Debug APK
################################################################################
build_debug_apk() {
    echo -e "${YELLOW}[5/7] Building Debug APK...${NC}"
    
    cd "$PROJECT_DIR"
    
    # Build debug APK
    ./gradlew assembleDebug --no-daemon --stacktrace || {
        echo -e "${RED}✗ Debug build failed${NC}"
        exit 1
    }
    
    # Check if APK was created
    DEBUG_APK="${BUILD_DIR}/debug/app-debug.apk"
    if [ -f "$DEBUG_APK" ]; then
        APK_SIZE=$(du -h "$DEBUG_APK" | cut -f1)
        echo -e "${GREEN}✓ Debug APK created: $DEBUG_APK ($APK_SIZE)${NC}"
    else
        echo -e "${RED}✗ Debug APK not found${NC}"
        exit 1
    fi
    
    echo ""
}

################################################################################
# Function: Build Release APK (Unsigned)
################################################################################
build_release_apk() {
    echo -e "${YELLOW}[6/7] Building Release APK (unsigned)...${NC}"
    
    cd "$PROJECT_DIR"
    
    # Build release APK
    ./gradlew assembleRelease --no-daemon --stacktrace || {
        echo -e "${YELLOW}⚠ Release build failed (this is expected without signing config)${NC}"
        echo -e "${YELLOW}  Continuing with debug APK only...${NC}"
        return 0
    }
    
    # Check if APK was created
    RELEASE_APK="${BUILD_DIR}/release/app-release-unsigned.apk"
    if [ -f "$RELEASE_APK" ]; then
        APK_SIZE=$(du -h "$RELEASE_APK" | cut -f1)
        echo -e "${GREEN}✓ Release APK created: $RELEASE_APK ($APK_SIZE)${NC}"
    else
        echo -e "${YELLOW}⚠ Release APK not created (signing required)${NC}"
    fi
    
    echo ""
}

################################################################################
# Function: Display Build Summary
################################################################################
build_summary() {
    echo -e "${YELLOW}[7/7] Build Summary${NC}"
    echo ""
    
    echo -e "${BLUE}Built APKs:${NC}"
    
    # List all APKs
    if [ -d "$BUILD_DIR" ]; then
        find "$BUILD_DIR" -name "*.apk" -type f | while read -r apk; do
            APK_SIZE=$(du -h "$apk" | cut -f1)
            APK_NAME=$(basename "$apk")
            echo -e "  ${GREEN}✓${NC} $APK_NAME ($APK_SIZE)"
            echo -e "    ${BLUE}Path:${NC} $apk"
        done
    else
        echo -e "${RED}✗ No APKs found${NC}"
    fi
    
    echo ""
    echo -e "${BLUE}Next Steps:${NC}"
    echo -e "  1. Connect your Android device via USB"
    echo -e "  2. Enable USB debugging on your device"
    echo -e "  3. Run: ${GREEN}./install_apk.sh${NC}"
    echo ""
    echo -e "${GREEN}✓ Build complete!${NC}"
}

################################################################################
# Main Execution
################################################################################
main() {
    check_prerequisites
    setup_gradle_cache
    clean_build
    download_dependencies
    build_debug_apk
    build_release_apk
    build_summary
}

# Run main function
main "$@"
