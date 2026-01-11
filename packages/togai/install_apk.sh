#!/bin/bash

################################################################################
# Togai Android APK Installation Script
# 
# This script installs the Togai APK on connected Android devices
# and provides testing utilities.
#
# Requirements:
# - Android SDK installed with platform-tools (adb)
# - USB debugging enabled on target device
# - Device connected via USB or WiFi
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
PACKAGE_NAME="org.ninelym.togai"

echo -e "${BLUE}=====================================${NC}"
echo -e "${BLUE}Togai Android APK Installation Script${NC}"
echo -e "${BLUE}=====================================${NC}"
echo ""

################################################################################
# Function: Check ADB
################################################################################
check_adb() {
    echo -e "${YELLOW}[1/6] Checking ADB...${NC}"
    
    if ! command -v adb &> /dev/null; then
        if [ -n "$ANDROID_HOME" ] && [ -f "$ANDROID_HOME/platform-tools/adb" ]; then
            export PATH="$ANDROID_HOME/platform-tools:$PATH"
            echo -e "${GREEN}✓ ADB found in ANDROID_HOME${NC}"
        else
            echo -e "${RED}✗ ADB not found. Please install Android SDK platform-tools.${NC}"
            exit 1
        fi
    else
        echo -e "${GREEN}✓ ADB: $(which adb)${NC}"
    fi
    
    # Check ADB version
    ADB_VERSION=$(adb version | head -1)
    echo -e "${GREEN}✓ $ADB_VERSION${NC}"
    
    echo ""
}

################################################################################
# Function: Check Connected Devices
################################################################################
check_devices() {
    echo -e "${YELLOW}[2/6] Checking connected devices...${NC}"
    
    # Start ADB server
    adb start-server > /dev/null 2>&1
    
    # Get list of devices
    DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)
    
    if [ "$DEVICES" -eq 0 ]; then
        echo -e "${RED}✗ No devices connected${NC}"
        echo ""
        echo -e "${YELLOW}Troubleshooting:${NC}"
        echo -e "  1. Connect your device via USB"
        echo -e "  2. Enable USB debugging in Developer Options"
        echo -e "  3. Accept the USB debugging prompt on your device"
        echo -e "  4. Run: ${GREEN}adb devices${NC}"
        echo ""
        exit 1
    fi
    
    echo -e "${GREEN}✓ Found $DEVICES device(s)${NC}"
    echo ""
    
    # List devices
    echo -e "${BLUE}Connected Devices:${NC}"
    adb devices -l | grep -v "List" | grep "device$" | while read -r line; do
        DEVICE_ID=$(echo "$line" | awk '{print $1}')
        DEVICE_MODEL=$(echo "$line" | grep -oP 'model:\K[^ ]+' || echo "Unknown")
        echo -e "  ${GREEN}•${NC} $DEVICE_ID ($DEVICE_MODEL)"
    done
    echo ""
}

################################################################################
# Function: Select APK
################################################################################
select_apk() {
    echo -e "${YELLOW}[3/6] Selecting APK...${NC}"
    
    # Find APKs
    DEBUG_APK="${BUILD_DIR}/debug/app-debug.apk"
    RELEASE_APK="${BUILD_DIR}/release/app-release.apk"
    
    # Prefer debug APK
    if [ -f "$DEBUG_APK" ]; then
        SELECTED_APK="$DEBUG_APK"
        APK_TYPE="Debug"
    elif [ -f "$RELEASE_APK" ]; then
        SELECTED_APK="$RELEASE_APK"
        APK_TYPE="Release"
    else
        echo -e "${RED}✗ No APK found. Please run ./build_apk.sh first${NC}"
        exit 1
    fi
    
    APK_SIZE=$(du -h "$SELECTED_APK" | cut -f1)
    echo -e "${GREEN}✓ Selected: $APK_TYPE APK ($APK_SIZE)${NC}"
    echo -e "  ${BLUE}Path:${NC} $SELECTED_APK"
    echo ""
}

################################################################################
# Function: Uninstall Previous Version
################################################################################
uninstall_previous() {
    echo -e "${YELLOW}[4/6] Checking for previous installation...${NC}"
    
    # Check if app is installed
    if adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
        echo -e "${YELLOW}⚠ Previous version found. Uninstalling...${NC}"
        adb uninstall "$PACKAGE_NAME" || {
            echo -e "${RED}✗ Failed to uninstall previous version${NC}"
            exit 1
        }
        echo -e "${GREEN}✓ Previous version uninstalled${NC}"
    else
        echo -e "${GREEN}✓ No previous installation found${NC}"
    fi
    
    echo ""
}

################################################################################
# Function: Install APK
################################################################################
install_apk() {
    echo -e "${YELLOW}[5/6] Installing APK...${NC}"
    
    # Install APK
    echo -e "${BLUE}Installing $SELECTED_APK...${NC}"
    
    adb install -r "$SELECTED_APK" || {
        echo -e "${RED}✗ Installation failed${NC}"
        echo ""
        echo -e "${YELLOW}Troubleshooting:${NC}"
        echo -e "  1. Check device storage space"
        echo -e "  2. Ensure USB debugging is enabled"
        echo -e "  3. Try: ${GREEN}adb install -r -d $SELECTED_APK${NC}"
        exit 1
    }
    
    echo -e "${GREEN}✓ APK installed successfully${NC}"
    echo ""
}

################################################################################
# Function: Launch Application
################################################################################
launch_app() {
    echo -e "${YELLOW}[6/6] Launching application...${NC}"
    
    # Launch main activity
    MAIN_ACTIVITY="${PACKAGE_NAME}/.MainActivity"
    
    adb shell am start -n "$MAIN_ACTIVITY" || {
        echo -e "${YELLOW}⚠ Failed to launch automatically${NC}"
        echo -e "${YELLOW}  Please launch the app manually from your device${NC}"
        return 0
    }
    
    echo -e "${GREEN}✓ Application launched${NC}"
    echo ""
}

################################################################################
# Function: Display Post-Installation Info
################################################################################
post_install_info() {
    echo -e "${BLUE}=====================================${NC}"
    echo -e "${BLUE}Installation Complete!${NC}"
    echo -e "${BLUE}=====================================${NC}"
    echo ""
    
    echo -e "${GREEN}✓ Togai installed on device${NC}"
    echo ""
    
    echo -e "${BLUE}Useful Commands:${NC}"
    echo -e "  ${GREEN}View logs:${NC}"
    echo -e "    adb logcat | grep Togai"
    echo ""
    echo -e "  ${GREEN}View app info:${NC}"
    echo -e "    adb shell dumpsys package $PACKAGE_NAME"
    echo ""
    echo -e "  ${GREEN}Clear app data:${NC}"
    echo -e "    adb shell pm clear $PACKAGE_NAME"
    echo ""
    echo -e "  ${GREEN}Uninstall app:${NC}"
    echo -e "    adb uninstall $PACKAGE_NAME"
    echo ""
    echo -e "  ${GREEN}Take screenshot:${NC}"
    echo -e "    adb shell screencap -p /sdcard/screenshot.png"
    echo -e "    adb pull /sdcard/screenshot.png"
    echo ""
    
    echo -e "${BLUE}Testing:${NC}"
    echo -e "  1. Check memory status display"
    echo -e "  2. Verify native library loading"
    echo -e "  3. Test AI system button"
    echo -e "  4. Monitor logcat for errors"
    echo ""
}

################################################################################
# Function: Interactive Device Selection
################################################################################
select_device() {
    DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device$" | wc -l)
    
    if [ "$DEVICE_COUNT" -gt 1 ]; then
        echo -e "${YELLOW}Multiple devices found. Please select:${NC}"
        
        # Create array of devices
        DEVICE_LIST=($(adb devices | grep -v "List" | grep "device$" | awk '{print $1}'))
        
        # Display devices
        for i in "${!DEVICE_LIST[@]}"; do
            DEVICE_ID="${DEVICE_LIST[$i]}"
            DEVICE_MODEL=$(adb -s "$DEVICE_ID" shell getprop ro.product.model 2>/dev/null || echo "Unknown")
            echo -e "  ${GREEN}$((i+1))${NC}. $DEVICE_ID ($DEVICE_MODEL)"
        done
        
        echo ""
        read -p "Select device (1-$DEVICE_COUNT): " SELECTION
        
        if [ "$SELECTION" -ge 1 ] && [ "$SELECTION" -le "$DEVICE_COUNT" ]; then
            SELECTED_DEVICE="${DEVICE_LIST[$((SELECTION-1))]}"
            export ANDROID_SERIAL="$SELECTED_DEVICE"
            echo -e "${GREEN}✓ Selected: $SELECTED_DEVICE${NC}"
            echo ""
        else
            echo -e "${RED}✗ Invalid selection${NC}"
            exit 1
        fi
    fi
}

################################################################################
# Main Execution
################################################################################
main() {
    check_adb
    check_devices
    select_device
    select_apk
    uninstall_previous
    install_apk
    launch_app
    post_install_info
}

# Run main function
main "$@"
