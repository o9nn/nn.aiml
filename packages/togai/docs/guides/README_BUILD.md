# Togai Android - Quick Start Guide

**Fast track to building and testing the Togai Android application**

---

## 🚀 Quick Start (3 Steps)

### 1. Prerequisites
```bash
# Verify you have:
java -version        # Java 11+
echo $ANDROID_HOME   # Android SDK path set
adb version          # ADB available
```

### 2. Build APK
```bash
./build_apk.sh
```

### 3. Install & Test
```bash
# Connect your Android device via USB
# Enable USB debugging on device

./install_apk.sh
```

**Done!** The app should launch automatically on your device.

---

## 📋 Detailed Instructions

See [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) for comprehensive documentation including:
- Complete prerequisites
- Environment setup
- Troubleshooting guide
- Advanced build options
- Testing procedures

---

## 🛠️ Available Scripts

### Build Scripts
- **`build_apk.sh`** - Automated APK build (recommended)
- **`validate_implementation.sh`** - Validate project structure

### Installation Scripts
- **`install_apk.sh`** - Install APK on connected device (recommended)
- **`test_device.sh`** - Quick device testing

### Manual Commands
```bash
# Build
./gradlew assembleDebug           # Build debug APK
./gradlew assembleRelease         # Build release APK
./gradlew clean                   # Clean build

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Test
adb logcat | grep Togai           # View logs
adb shell dumpsys meminfo org.ninelym.togai  # Memory stats
```

---

## 📱 Device Requirements

- **Android**: 8.0 (API 26) or higher
- **Architecture**: ARM64 (arm64-v8a)
- **RAM**: 2GB minimum, 4GB+ recommended
- **Storage**: 500MB free space

---

## 🔧 Troubleshooting

### Build fails with "ANDROID_HOME not set"
```bash
export ANDROID_HOME=$HOME/Android/Sdk
```

### No devices found
```bash
# Enable USB debugging on device:
# Settings → Developer Options → USB Debugging

# Restart ADB
adb kill-server
adb start-server
adb devices
```

### App crashes on startup
```bash
# Check logs for errors
adb logcat | grep -E "AndroidRuntime|FATAL"
```

See [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) for complete troubleshooting guide.

---

## 📊 Project Statistics

- **Kotlin Files**: 102
- **Lines of Code**: 41,861
- **Native Libraries**: 87 (153MB)
- **Min Android**: 8.0 (API 26)
- **Target Android**: 14 (API 34)

---

## 📚 Documentation

- [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md) - Complete build guide
- [IMPLEMENTATION_REPORT.md](IMPLEMENTATION_REPORT.md) - Implementation details
- [DEPLOYMENT_SUMMARY.md](DEPLOYMENT_SUMMARY.md) - Deployment checklist
- [COMPREHENSIVE_UPDATE_REPORT.md](COMPREHENSIVE_UPDATE_REPORT.md) - Technical report

---

## 🔗 Links

- **Repository**: https://github.com/o9nn/togai
- **Issues**: https://github.com/o9nn/togai/issues

---

## ⚡ One-Line Build & Install

```bash
./build_apk.sh && ./install_apk.sh
```

---

*For detailed instructions, see [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md)*
