#!/bin/bash

echo "=================================="
echo "Togai Implementation Validation"
echo "=================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
PASS=0
FAIL=0

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
        ((PASS++))
    else
        echo -e "${RED}✗${NC} $1 (missing)"
        ((FAIL++))
    fi
}

check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
        ((PASS++))
    else
        echo -e "${RED}✗${NC} $1 (missing)"
        ((FAIL++))
    fi
}

echo "=== Core Configuration Files ==="
check_file "build.gradle.kts"
check_file "settings.gradle.kts"
check_file "gradle.properties"
check_file "app/build.gradle.kts"
check_file "app/proguard-rules.pro"
echo ""

echo "=== Android Manifest & Resources ==="
check_file "app/src/main/AndroidManifest.xml"
check_file "app/src/main/res/values/strings.xml"
check_file "app/src/main/res/values/colors.xml"
check_file "app/src/main/res/values/themes.xml"
check_file "app/src/main/res/xml/file_paths.xml"
check_file "app/src/main/res/xml/backup_rules.xml"
check_file "app/src/main/res/xml/data_extraction_rules.xml"
echo ""

echo "=== Application Components ==="
check_file "app/src/main/kotlin/org/ninelym/togai/TogaiApplication.kt"
check_file "app/src/main/kotlin/org/ninelym/togai/MainActivity.kt"
check_file "app/src/main/kotlin/org/ninelym/togai/BuildConfig.kt"
check_file "app/src/main/kotlin/org/ninelym/togai/service/AIInferenceService.kt"
check_file "app/src/main/kotlin/org/ninelym/togai/service/TogaiFirebaseMessagingService.kt"
echo ""

echo "=== Core Systems ==="
check_file "app/src/main/kotlin/org/ninelym/native/NativeLibraryLoader.kt"
check_file "app/src/main/kotlin/org/ninelym/optimization/MemoryOptimizer.kt"
check_file "app/src/main/kotlin/org/ninelym/optimization/PerformanceMonitor.kt"
check_file "app/src/main/kotlin/org/ninelym/quantum/QuantumNeuralProcessor.kt"
check_file "app/src/main/kotlin/org/ninelym/evolution/MultimodalOrchestrator.kt"
echo ""

echo "=== Native Libraries ==="
check_dir "app/src/main/jniLibs/arm64-v8a"
NATIVE_COUNT=$(find app/src/main/jniLibs/arm64-v8a -name "*.so" 2>/dev/null | wc -l)
if [ "$NATIVE_COUNT" -eq 87 ]; then
    echo -e "${GREEN}✓${NC} Found 87 native libraries"
    ((PASS++))
else
    echo -e "${YELLOW}⚠${NC} Found $NATIVE_COUNT native libraries (expected 87)"
    ((FAIL++))
fi
echo ""

echo "=== Documentation ==="
check_file "README.md"
check_file "COMPREHENSIVE_UPDATE_REPORT.md"
check_file "DEPLOYMENT_SUMMARY.md"
check_file "IMPLEMENTATION_REPORT.md"
check_file "docs/ARM64_NATIVE_LIBRARIES.md"
echo ""

echo "=== Kotlin Source Files ==="
KOTLIN_COUNT=$(find app/src/main/kotlin -name "*.kt" 2>/dev/null | wc -l)
if [ "$KOTLIN_COUNT" -ge 100 ]; then
    echo -e "${GREEN}✓${NC} Found $KOTLIN_COUNT Kotlin files"
    ((PASS++))
else
    echo -e "${YELLOW}⚠${NC} Found $KOTLIN_COUNT Kotlin files (expected 100+)"
    ((FAIL++))
fi
echo ""

echo "=== Directory Structure ==="
check_dir "app/src/main/kotlin/org/ninelym/cognitive"
check_dir "app/src/main/kotlin/org/ninelym/evolution"
check_dir "app/src/main/kotlin/org/ninelym/native"
check_dir "app/src/main/kotlin/org/ninelym/optimization"
check_dir "app/src/main/kotlin/org/ninelym/quantum"
check_dir "app/src/main/kotlin/org/ninelym/togai"
check_dir "app/src/main/kotlin/org/ninelym/togai/service"
echo ""

echo "=== Summary ==="
echo -e "Passed: ${GREEN}$PASS${NC}"
echo -e "Failed: ${RED}$FAIL${NC}"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✓ All validation checks passed!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠ Some validation checks failed${NC}"
    exit 1
fi
