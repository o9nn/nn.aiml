#!/bin/bash

################################################################################
# Togai Device Testing Script
# 
# Quick validation script to test the installed application on device
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PACKAGE_NAME="org.ninelym.togai"

echo -e "${BLUE}=============================${NC}"
echo -e "${BLUE}Togai Device Testing Script${NC}"
echo -e "${BLUE}=============================${NC}"
echo ""

# Check if app is installed
echo -e "${YELLOW}[1/5] Checking installation...${NC}"
if adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    echo -e "${GREEN}✓ Togai is installed${NC}"
else
    echo -e "${RED}✗ Togai is not installed${NC}"
    echo -e "${YELLOW}  Run: ./install_apk.sh${NC}"
    exit 1
fi
echo ""

# Get app info
echo -e "${YELLOW}[2/5] Application info...${NC}"
VERSION=$(adb shell dumpsys package "$PACKAGE_NAME" | grep versionName | head -1 | awk '{print $1}')
echo -e "${GREEN}✓ $VERSION${NC}"
echo ""

# Check if app is running
echo -e "${YELLOW}[3/5] Checking app status...${NC}"
PID=$(adb shell pidof "$PACKAGE_NAME" 2>/dev/null || echo "")
if [ -n "$PID" ]; then
    echo -e "${GREEN}✓ App is running (PID: $PID)${NC}"
else
    echo -e "${YELLOW}⚠ App is not running${NC}"
    echo -e "${YELLOW}  Launching app...${NC}"
    adb shell am start -n "$PACKAGE_NAME/.MainActivity" > /dev/null 2>&1
    sleep 2
    PID=$(adb shell pidof "$PACKAGE_NAME" 2>/dev/null || echo "")
    if [ -n "$PID" ]; then
        echo -e "${GREEN}✓ App launched (PID: $PID)${NC}"
    else
        echo -e "${RED}✗ Failed to launch app${NC}"
        exit 1
    fi
fi
echo ""

# Check memory usage
echo -e "${YELLOW}[4/5] Memory usage...${NC}"
MEMORY=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL PSS" | awk '{print $3}')
if [ -n "$MEMORY" ]; then
    MEMORY_MB=$((MEMORY / 1024))
    echo -e "${GREEN}✓ Memory: ${MEMORY_MB} MB${NC}"
else
    echo -e "${YELLOW}⚠ Could not retrieve memory info${NC}"
fi
echo ""

# Monitor logs for 5 seconds
echo -e "${YELLOW}[5/5] Monitoring logs (5 seconds)...${NC}"
echo -e "${BLUE}Recent log entries:${NC}"
timeout 5 adb logcat | grep -E "Togai|MemoryOptimizer|QuantumProcessor" | head -10 || true
echo ""

# Summary
echo -e "${BLUE}=============================${NC}"
echo -e "${GREEN}✓ Testing complete!${NC}"
echo -e "${BLUE}=============================${NC}"
echo ""

echo -e "${BLUE}Useful commands:${NC}"
echo -e "  ${GREEN}View live logs:${NC}"
echo -e "    adb logcat | grep Togai"
echo ""
echo -e "  ${GREEN}Take screenshot:${NC}"
echo -e "    adb shell screencap -p /sdcard/screenshot.png"
echo -e "    adb pull /sdcard/screenshot.png"
echo ""
echo -e "  ${GREEN}Restart app:${NC}"
echo -e "    adb shell am force-stop $PACKAGE_NAME"
echo -e "    adb shell am start -n $PACKAGE_NAME/.MainActivity"
echo ""
