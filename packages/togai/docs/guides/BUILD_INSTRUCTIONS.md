# Togai Android - Build & Installation Instructions

**Project**: Togai (9mly) - Advanced Android AI Assistant  
**Repository**: https://github.com/o9nn/togai  
**Build System**: Gradle 8.5 + Android SDK 34

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Building the APK](#building-the-apk)
4. [Installing on Device](#installing-on-device)
5. [Testing & Debugging](#testing--debugging)
6. [Troubleshooting](#troubleshooting)
7. [Advanced Options](#advanced-options)

---

## Prerequisites

### Required Software

#### 1. **Java Development Kit (JDK) 11+**
```bash
# Check Java version
java -version

# Should show: openjdk version "11.0" or higher
```

**Installation**:
- **Ubuntu/Debian**: `sudo apt install openjdk-11-jdk`
- **macOS**: `brew install openjdk@11`
- **Windows**: Download from [AdoptOpenJDK](https://adoptopenjdk.net/)

#### 2. **Android SDK**
- **Android Studio** (recommended): Includes SDK, tools, and emulator
- **Command Line Tools**: Minimal SDK installation

**Download**: https://developer.android.com/studio

#### 3. **Android SDK Components**
Required SDK components:
- **Platform**: Android 14 (API 34)
- **Build Tools**: 34.0.0 or higher
- **Platform Tools**: Latest (includes adb)
- **NDK**: For native library support

**Install via Android Studio**:
```
Tools → SDK Manager → SDK Platforms → Android 14.0 (API 34)
Tools → SDK Manager → SDK Tools → Android SDK Build-Tools 34
Tools → SDK Manager → SDK Tools → Android SDK Platform-Tools
Tools → SDK Manager → SDK Tools → NDK (Side by side)
```

**Install via Command Line**:
```bash
# Using sdkmanager
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
sdkmanager "platform-tools"
sdkmanager "ndk;25.2.9519653"
```

### Hardware Requirements

#### Development Machine
- **RAM**: 8GB minimum, 16GB recommended
- **Storage**: 10GB free space (for SDK + build cache)
- **OS**: Linux, macOS, or Windows 10+

#### Target Android Device
- **Android Version**: 8.0 (API 26) or higher
- **Architecture**: ARM64 (arm64-v8a)
- **RAM**: 2GB minimum, 4GB+ recommended
- **Storage**: 500MB free space

---

## Environment Setup

### 1. Set Environment Variables

#### Linux/macOS
Add to `~/.bashrc` or `~/.zshrc`:
```bash
# Android SDK
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/tools/bin

# Java (if needed)
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH=$PATH:$JAVA_HOME/bin
```

Apply changes:
```bash
source ~/.bashrc  # or source ~/.zshrc
```

#### Windows
Set in System Environment Variables:
```
ANDROID_HOME = C:\Users\YourName\AppData\Local\Android\Sdk
JAVA_HOME = C:\Program Files\Java\jdk-11

PATH += %ANDROID_HOME%\platform-tools
PATH += %ANDROID_HOME%\tools
PATH += %JAVA_HOME%\bin
```

### 2. Verify Installation

```bash
# Check Java
java -version

# Check Android SDK
echo $ANDROID_HOME
ls $ANDROID_HOME

# Check ADB
adb version

# Check Gradle (will be downloaded automatically)
./gradlew --version
```

### 3. Clone Repository

```bash
git clone https://github.com/o9nn/togai.git
cd togai
```

---

## Building the APK

### Method 1: Automated Build Script (Recommended)

The project includes a comprehensive build script that handles all steps automatically.

```bash
# Make script executable
chmod +x build_apk.sh

# Run build
./build_apk.sh
```

**What it does**:
1. ✓ Checks prerequisites (Java, Android SDK)
2. ✓ Sets up Gradle cache
3. ✓ Cleans previous builds
4. ✓ Downloads dependencies
5. ✓ Builds debug APK
6. ✓ Attempts release APK (if signing configured)
7. ✓ Displays build summary

**Output**:
```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk (if applicable)
```

### Method 2: Manual Gradle Commands

#### Build Debug APK
```bash
# Clean previous builds
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

#### Build Release APK (Unsigned)
```bash
# Build release APK
./gradlew assembleRelease

# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

#### Build with Specific Options
```bash
# Build with stacktrace for debugging
./gradlew assembleDebug --stacktrace

# Build with info logging
./gradlew assembleDebug --info

# Build offline (use cached dependencies)
./gradlew assembleDebug --offline

# Build with parallel execution
./gradlew assembleDebug --parallel

# Build with build cache
./gradlew assembleDebug --build-cache
```

### Method 3: Android Studio

1. Open Android Studio
2. File → Open → Select `togai` directory
3. Wait for Gradle sync to complete
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. APK location shown in notification

---

## Installing on Device

### Method 1: Automated Installation Script (Recommended)

```bash
# Make script executable
chmod +x install_apk.sh

# Run installation
./install_apk.sh
```

**What it does**:
1. ✓ Checks ADB availability
2. ✓ Detects connected devices
3. ✓ Selects appropriate APK
4. ✓ Uninstalls previous version
5. ✓ Installs new APK
6. ✓ Launches application
7. ✓ Displays testing commands

### Method 2: Manual ADB Commands

#### Prepare Device
1. **Enable Developer Options**:
   - Settings → About Phone → Tap "Build Number" 7 times

2. **Enable USB Debugging**:
   - Settings → Developer Options → USB Debugging → ON

3. **Connect Device**:
   - Connect via USB cable
   - Accept USB debugging prompt on device

#### Install APK
```bash
# Check connected devices
adb devices

# Install debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Install with replace flag (if already installed)
adb install -r -d app/build/outputs/apk/debug/app-debug.apk

# Install on specific device (if multiple connected)
adb -s DEVICE_ID install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Launch Application
```bash
# Start main activity
adb shell am start -n org.ninelym.togai/.MainActivity

# Start with intent
adb shell am start -a android.intent.action.MAIN -n org.ninelym.togai/.MainActivity
```

### Method 3: Wireless Installation (WiFi ADB)

```bash
# Connect device via USB first
adb devices

# Enable TCP/IP mode
adb tcpip 5555

# Find device IP address
adb shell ip addr show wlan0

# Connect wirelessly (replace with your device IP)
adb connect 192.168.1.XXX:5555

# Disconnect USB cable

# Install APK wirelessly
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Testing & Debugging

### View Application Logs

#### Real-time Logcat
```bash
# View all Togai logs
adb logcat | grep Togai

# View specific tags
adb logcat | grep -E "TogaiApplication|MainActivity|MemoryOptimizer"

# View with colors (if supported)
adb logcat -v color | grep Togai

# Save logs to file
adb logcat | grep Togai > togai_logs.txt
```

#### Filtered Logcat
```bash
# View only errors
adb logcat *:E | grep Togai

# View warnings and errors
adb logcat *:W | grep Togai

# View specific package
adb logcat --pid=$(adb shell pidof -s org.ninelym.togai)
```

### Monitor Application

#### Memory Usage
```bash
# View memory stats
adb shell dumpsys meminfo org.ninelym.togai

# Continuous monitoring
watch -n 1 'adb shell dumpsys meminfo org.ninelym.togai | grep -A 10 "App Summary"'
```

#### CPU Usage
```bash
# View CPU stats
adb shell top | grep togai

# Detailed process info
adb shell ps | grep togai
```

#### Application Info
```bash
# View package info
adb shell dumpsys package org.ninelym.togai

# View permissions
adb shell dumpsys package org.ninelym.togai | grep permission

# View activities
adb shell dumpsys package org.ninelym.togai | grep Activity
```

### Performance Profiling

#### GPU Rendering
```bash
# Enable GPU rendering profile
adb shell setprop debug.hwui.profile visual_bars

# Disable
adb shell setprop debug.hwui.profile false
```

#### Layout Bounds
```bash
# Show layout bounds
adb shell setprop debug.layout true

# Restart app to apply
adb shell am force-stop org.ninelym.togai
adb shell am start -n org.ninelym.togai/.MainActivity
```

### Testing Checklist

After installation, verify:

1. **Application Launch**
   - [ ] App launches without crashes
   - [ ] UI displays correctly
   - [ ] No ANR (Application Not Responding)

2. **Memory Status**
   - [ ] Memory state indicator shows (Optimal/Moderate/Warning/Critical)
   - [ ] Memory statistics update in real-time
   - [ ] Available/Used/Total memory displayed

3. **Native Libraries**
   - [ ] Library count shows 87 (or loaded count)
   - [ ] No library loading errors in logcat
   - [ ] GPU detection works (if device supports)

4. **AI Capabilities**
   - [ ] AI capabilities display correctly
   - [ ] Test button responds
   - [ ] Multimodal processing works

5. **Performance**
   - [ ] UI is responsive (60fps)
   - [ ] No memory leaks
   - [ ] Battery usage acceptable

---

## Troubleshooting

### Build Issues

#### Issue: "ANDROID_HOME not set"
```bash
# Solution: Set environment variable
export ANDROID_HOME=$HOME/Android/Sdk

# Verify
echo $ANDROID_HOME
```

#### Issue: "SDK location not found"
```bash
# Solution: Create local.properties
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

#### Issue: "Gradle sync failed"
```bash
# Solution: Clear Gradle cache
rm -rf ~/.gradle/caches
./gradlew clean --refresh-dependencies
```

#### Issue: "Java version mismatch"
```bash
# Solution: Set JAVA_HOME to Java 11
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# Verify
java -version
```

#### Issue: "Out of memory during build"
```bash
# Solution: Increase Gradle heap size
# Edit gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
```

### Installation Issues

#### Issue: "No devices found"
```bash
# Solution 1: Check USB connection
adb devices

# Solution 2: Restart ADB server
adb kill-server
adb start-server

# Solution 3: Check USB debugging
# Enable in: Settings → Developer Options → USB Debugging
```

#### Issue: "Installation failed: INSTALL_FAILED_INSUFFICIENT_STORAGE"
```bash
# Solution: Free up device storage
adb shell df

# Clear app data
adb shell pm clear org.ninelym.togai
```

#### Issue: "Installation failed: INSTALL_FAILED_UPDATE_INCOMPATIBLE"
```bash
# Solution: Uninstall previous version
adb uninstall org.ninelym.togai

# Then reinstall
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Issue: "Device unauthorized"
```bash
# Solution: Accept USB debugging prompt on device
# If prompt doesn't appear:
adb kill-server
adb start-server
adb devices
```

### Runtime Issues

#### Issue: "App crashes on startup"
```bash
# Solution: Check logcat for errors
adb logcat | grep -E "AndroidRuntime|FATAL"

# Common causes:
# - Missing native libraries
# - Insufficient memory
# - Permission issues
```

#### Issue: "Native library not found"
```bash
# Solution: Verify libraries are in APK
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "lib/arm64-v8a"

# Should show 87 .so files
```

#### Issue: "Out of memory"
```bash
# Solution: Check device memory
adb shell cat /proc/meminfo

# Close other apps
adb shell am force-stop <package_name>
```

---

## Advanced Options

### Signing Release APK

#### 1. Create Keystore
```bash
keytool -genkey -v -keystore togai-release.keystore \
  -alias togai -keyalg RSA -keysize 2048 -validity 10000
```

#### 2. Configure Signing
Create `keystore.properties`:
```properties
storeFile=togai-release.keystore
storePassword=YOUR_STORE_PASSWORD
keyAlias=togai
keyPassword=YOUR_KEY_PASSWORD
```

#### 3. Update `app/build.gradle.kts`
```kotlin
signingConfigs {
    create("release") {
        val keystoreProperties = Properties()
        keystoreProperties.load(FileInputStream(rootProject.file("keystore.properties")))
        
        storeFile = file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

#### 4. Build Signed APK
```bash
./gradlew assembleRelease
```

### Building AAB (Android App Bundle)

```bash
# Build app bundle
./gradlew bundleRelease

# Output: app/build/outputs/bundle/release/app-release.aab
```

### ProGuard/R8 Optimization

Already configured in `app/proguard-rules.pro`. To enable:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### Multi-APK Build

Build for specific ABIs:

```bash
# ARM64 only (default)
./gradlew assembleDebug

# Multiple ABIs
# Edit app/build.gradle.kts:
ndk {
    abiFilters += listOf("arm64-v8a", "armeabi-v7a")
}
```

### Continuous Integration

#### GitHub Actions Example
```yaml
name: Build APK

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '11'
      - name: Build APK
        run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v3
        with:
          name: app-debug
          path: app/build/outputs/apk/debug/app-debug.apk
```

---

## Quick Reference

### Essential Commands

```bash
# Build
./build_apk.sh                    # Automated build
./gradlew assembleDebug           # Manual debug build
./gradlew assembleRelease         # Manual release build

# Install
./install_apk.sh                  # Automated installation
adb install -r app-debug.apk      # Manual installation

# Debug
adb logcat | grep Togai           # View logs
adb shell dumpsys meminfo ...     # Memory stats
adb shell am force-stop ...       # Stop app

# Cleanup
./gradlew clean                   # Clean build
adb uninstall org.ninelym.togai   # Uninstall app
```

### File Locations

```
togai/
├── app/build/outputs/apk/
│   ├── debug/app-debug.apk           # Debug APK
│   └── release/app-release.apk       # Release APK
├── build_apk.sh                      # Build script
├── install_apk.sh                    # Installation script
└── BUILD_INSTRUCTIONS.md             # This file
```

---

## Support

- **Repository**: https://github.com/o9nn/togai
- **Issues**: https://github.com/o9nn/togai/issues
- **Documentation**: `/docs` directory

---

*Last Updated: December 25, 2025*
