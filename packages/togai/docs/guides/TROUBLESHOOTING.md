# Togai Android - Troubleshooting Guide

**Common issues and solutions for building, installing, and running Togai**

---

## Table of Contents

1. [Build Issues](#build-issues)
2. [Installation Issues](#installation-issues)
3. [Runtime Issues](#runtime-issues)
4. [Performance Issues](#performance-issues)
5. [Device-Specific Issues](#device-specific-issues)
6. [Diagnostic Commands](#diagnostic-commands)

---

## Build Issues

### Issue: "ANDROID_HOME not set"

**Symptoms**:
```
Error: ANDROID_HOME environment variable is not set
```

**Solutions**:

**Linux/macOS**:
```bash
# Find Android SDK location
ls -la ~/Android/Sdk
# or
ls -la ~/Library/Android/sdk

# Set environment variable
export ANDROID_HOME=$HOME/Android/Sdk

# Add to shell profile
echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc
```

**Windows**:
```cmd
# Set system environment variable
setx ANDROID_HOME "C:\Users\YourName\AppData\Local\Android\Sdk"

# Or use System Properties → Environment Variables
```

**Verify**:
```bash
echo $ANDROID_HOME
ls $ANDROID_HOME
```

---

### Issue: "SDK location not found"

**Symptoms**:
```
SDK location not found. Define location with an ANDROID_SDK_ROOT environment variable
or by setting the sdk.dir path in your project's local properties file
```

**Solutions**:

**Option 1**: Create `local.properties`
```bash
cd /path/to/togai
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

**Option 2**: Set ANDROID_SDK_ROOT
```bash
export ANDROID_SDK_ROOT=$ANDROID_HOME
```

---

### Issue: "Java version mismatch"

**Symptoms**:
```
Unsupported Java version
Android Gradle plugin requires Java 11 to run
```

**Solutions**:

**Check current version**:
```bash
java -version
```

**Install Java 11**:
```bash
# Ubuntu/Debian
sudo apt install openjdk-11-jdk

# macOS
brew install openjdk@11

# Windows
# Download from https://adoptopenjdk.net/
```

**Set JAVA_HOME**:
```bash
# Linux
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# macOS
export JAVA_HOME=$(/usr/libexec/java_home -v 11)

# Verify
java -version
echo $JAVA_HOME
```

---

### Issue: "Gradle sync failed"

**Symptoms**:
```
Gradle sync failed: Connection timed out
Could not resolve dependencies
```

**Solutions**:

**Option 1**: Clear Gradle cache
```bash
rm -rf ~/.gradle/caches
rm -rf .gradle
./gradlew clean --refresh-dependencies
```

**Option 2**: Use offline mode (if dependencies cached)
```bash
./gradlew assembleDebug --offline
```

**Option 3**: Check network/proxy settings
```bash
# Edit ~/.gradle/gradle.properties
systemProp.http.proxyHost=proxy.company.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=proxy.company.com
systemProp.https.proxyPort=8080
```

---

### Issue: "Out of memory during build"

**Symptoms**:
```
OutOfMemoryError: Java heap space
GC overhead limit exceeded
```

**Solutions**:

**Increase Gradle heap size**:
```bash
# Edit gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError

# Or use command line
./gradlew assembleDebug -Dorg.gradle.jvmargs="-Xmx4096m"
```

**Enable parallel builds**:
```bash
# Edit gradle.properties
org.gradle.parallel=true
org.gradle.caching=true
```

---

### Issue: "NDK not found"

**Symptoms**:
```
NDK is not installed
No version of NDK matched the requested version
```

**Solutions**:

**Install NDK via Android Studio**:
```
Tools → SDK Manager → SDK Tools → NDK (Side by side)
```

**Install NDK via command line**:
```bash
sdkmanager "ndk;25.2.9519653"
```

**Specify NDK version in build.gradle.kts**:
```kotlin
android {
    ndkVersion = "25.2.9519653"
}
```

---

## Installation Issues

### Issue: "No devices found"

**Symptoms**:
```
adb devices
List of devices attached
(empty)
```

**Solutions**:

**Step 1**: Enable Developer Options
```
Settings → About Phone → Tap "Build Number" 7 times
```

**Step 2**: Enable USB Debugging
```
Settings → Developer Options → USB Debugging → ON
```

**Step 3**: Check USB connection
```bash
# Restart ADB server
adb kill-server
adb start-server

# Check devices
adb devices

# If device shows as "unauthorized"
# Accept the USB debugging prompt on your device
```

**Step 4**: Try different USB cable/port
- Use original USB cable
- Try different USB port
- Avoid USB hubs

**Step 5**: Check device drivers (Windows)
- Install device-specific USB drivers
- Check Device Manager for unknown devices

---

### Issue: "Device unauthorized"

**Symptoms**:
```
adb devices
List of devices attached
XXXXXXXXXX      unauthorized
```

**Solutions**:

**Step 1**: Accept USB debugging prompt on device
- Look for popup on device screen
- Check "Always allow from this computer"
- Tap "OK"

**Step 2**: Revoke and re-authorize
```bash
# On device: Settings → Developer Options → Revoke USB debugging authorizations

# Reconnect device
adb kill-server
adb start-server
adb devices
```

**Step 3**: Check ADB keys
```bash
# Remove old keys
rm ~/.android/adbkey*

# Restart ADB
adb kill-server
adb start-server
```

---

### Issue: "Installation failed: INSTALL_FAILED_INSUFFICIENT_STORAGE"

**Symptoms**:
```
Failure [INSTALL_FAILED_INSUFFICIENT_STORAGE]
```

**Solutions**:

**Check device storage**:
```bash
adb shell df -h
```

**Free up space**:
```bash
# Clear app cache
adb shell pm clear org.ninelym.togai

# Remove unused apps
adb shell pm list packages
adb uninstall <package_name>

# Clear system cache (requires root)
adb shell rm -rf /data/dalvik-cache/*
```

---

### Issue: "Installation failed: INSTALL_FAILED_UPDATE_INCOMPATIBLE"

**Symptoms**:
```
Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: Package org.ninelym.togai signatures do not match previously installed version]
```

**Solutions**:

**Uninstall previous version**:
```bash
adb uninstall org.ninelym.togai

# Then reinstall
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Or use installation script**:
```bash
./install_apk.sh  # Automatically handles uninstall
```

---

### Issue: "Installation failed: INSTALL_FAILED_INVALID_APK"

**Symptoms**:
```
Failure [INSTALL_FAILED_INVALID_APK]
```

**Solutions**:

**Verify APK integrity**:
```bash
# Check APK exists
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Verify APK structure
unzip -l app/build/outputs/apk/debug/app-debug.apk | head -20
```

**Rebuild APK**:
```bash
./gradlew clean
./gradlew assembleDebug
```

**Check device architecture**:
```bash
# Get device ABI
adb shell getprop ro.product.cpu.abi

# Should be: arm64-v8a
# Togai only supports ARM64
```

---

## Runtime Issues

### Issue: "App crashes on startup"

**Symptoms**:
- App closes immediately after launch
- Black screen then crash

**Solutions**:

**Check crash logs**:
```bash
# View crash logs
adb logcat | grep -E "AndroidRuntime|FATAL"

# Save crash log
adb logcat -d > crash_log.txt
```

**Common causes**:

**1. Missing native libraries**
```bash
# Verify libraries in APK
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep "lib/arm64-v8a"

# Should show 87 .so files
```

**2. Insufficient memory**
```bash
# Check device memory
adb shell cat /proc/meminfo | grep MemAvailable

# Close other apps
adb shell am force-stop <package_name>
```

**3. Permission issues**
```bash
# Check app permissions
adb shell dumpsys package org.ninelym.togai | grep permission

# Grant permissions manually
adb shell pm grant org.ninelym.togai android.permission.RECORD_AUDIO
adb shell pm grant org.ninelym.togai android.permission.CAMERA
```

---

### Issue: "Native library not found"

**Symptoms**:
```
java.lang.UnsatisfiedLinkError: dlopen failed: library "libllama.so" not found
```

**Solutions**:

**Verify library in APK**:
```bash
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep libllama.so
```

**Check device architecture**:
```bash
adb shell getprop ro.product.cpu.abi
# Must be: arm64-v8a
```

**Rebuild with native libraries**:
```bash
# Ensure jniLibs are included
ls -la app/src/main/jniLibs/arm64-v8a/

# Clean and rebuild
./gradlew clean
./gradlew assembleDebug
```

---

### Issue: "Out of memory (OOM)"

**Symptoms**:
```
OutOfMemoryError
App killed by system
```

**Solutions**:

**Check app memory usage**:
```bash
adb shell dumpsys meminfo org.ninelym.togai
```

**Monitor memory in real-time**:
```bash
watch -n 1 'adb shell dumpsys meminfo org.ninelym.togai | grep -A 10 "App Summary"'
```

**Reduce memory usage**:
- Close other apps
- Restart device
- Use lower memory mode (app will auto-detect)

**Check for memory leaks**:
```bash
# Enable strict mode in app
# Monitor logcat for memory warnings
adb logcat | grep -E "StrictMode|GC"
```

---

### Issue: "ANR (Application Not Responding)"

**Symptoms**:
```
App freezes
"App isn't responding" dialog
```

**Solutions**:

**Check ANR traces**:
```bash
# Pull ANR traces
adb pull /data/anr/traces.txt

# View recent ANRs
adb shell ls -lt /data/anr/
```

**Common causes**:
- Long operations on main thread
- Deadlocks
- Slow I/O operations

**Monitor thread activity**:
```bash
adb shell ps -T | grep togai
```

---

## Performance Issues

### Issue: "Slow app startup"

**Symptoms**:
- App takes >10 seconds to start
- Splash screen hangs

**Solutions**:

**Profile startup time**:
```bash
# Measure cold start
adb shell am force-stop org.ninelym.togai
adb shell am start -W -n org.ninelym.togai/.MainActivity

# Look for: TotalTime value
```

**Optimize**:
- Reduce library loading (app auto-optimizes based on memory)
- Use lazy initialization
- Profile with Android Profiler

---

### Issue: "UI lag/jank"

**Symptoms**:
- Stuttering animations
- Slow scrolling
- Frame drops

**Solutions**:

**Enable GPU profiling**:
```bash
# Show GPU rendering bars
adb shell setprop debug.hwui.profile visual_bars

# Restart app
adb shell am force-stop org.ninelym.togai
adb shell am start -n org.ninelym.togai/.MainActivity
```

**Check frame rate**:
```bash
# Monitor frame stats
adb shell dumpsys gfxinfo org.ninelym.togai framestats
```

**Enable layout bounds**:
```bash
adb shell setprop debug.layout true
```

---

### Issue: "High battery drain"

**Symptoms**:
- Battery drains quickly
- Device gets hot

**Solutions**:

**Check battery usage**:
```bash
adb shell dumpsys batterystats org.ninelym.togai
```

**Monitor CPU usage**:
```bash
adb shell top | grep togai
```

**Check wake locks**:
```bash
adb shell dumpsys power | grep -A 5 "Wake Locks"
```

---

## Device-Specific Issues

### Samsung Devices

**Issue**: Installation blocked by Samsung Knox
**Solution**: Disable Knox security or use developer mode

**Issue**: Battery optimization kills app
**Solution**: 
```
Settings → Battery → App power management → Togai → Unrestricted
```

### Xiaomi/MIUI Devices

**Issue**: App permissions not working
**Solution**:
```
Settings → Apps → Permissions → Togai → Enable all required permissions
Settings → Additional settings → Privacy → Special permissions → Autostart → Enable Togai
```

### Huawei Devices

**Issue**: App killed in background
**Solution**:
```
Settings → Battery → App launch → Togai → Manage manually → Enable all
```

### OnePlus Devices

**Issue**: Aggressive battery optimization
**Solution**:
```
Settings → Battery → Battery optimization → Togai → Don't optimize
```

---

## Diagnostic Commands

### System Information
```bash
# Device model
adb shell getprop ro.product.model

# Android version
adb shell getprop ro.build.version.release

# CPU architecture
adb shell getprop ro.product.cpu.abi

# Memory info
adb shell cat /proc/meminfo

# Storage info
adb shell df -h
```

### Application Information
```bash
# Package info
adb shell dumpsys package org.ninelym.togai | grep version

# Permissions
adb shell dumpsys package org.ninelym.togai | grep permission

# Process info
adb shell ps | grep togai

# Memory usage
adb shell dumpsys meminfo org.ninelym.togai

# CPU usage
adb shell top -n 1 | grep togai
```

### Logging
```bash
# View all logs
adb logcat

# Filter by tag
adb logcat -s Togai:V

# Filter by priority
adb logcat *:E  # Errors only
adb logcat *:W  # Warnings and above

# Save to file
adb logcat -d > logcat.txt

# Clear logs
adb logcat -c
```

### Screenshots & Screen Recording
```bash
# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png

# Record screen (max 180 seconds)
adb shell screenrecord /sdcard/demo.mp4
# Press Ctrl+C to stop
adb pull /sdcard/demo.mp4
```

### Network
```bash
# Check network connectivity
adb shell ping -c 4 google.com

# View network stats
adb shell dumpsys netstats

# Check WiFi info
adb shell dumpsys wifi
```

---

## Getting Help

If issues persist:

1. **Check logs**: `adb logcat | grep Togai`
2. **Collect diagnostics**: Run diagnostic commands above
3. **Report issue**: https://github.com/o9nn/togai/issues
4. **Include**:
   - Device model and Android version
   - Error messages from logcat
   - Steps to reproduce
   - Screenshots if applicable

---

## Quick Fixes Checklist

- [ ] Restart ADB: `adb kill-server && adb start-server`
- [ ] Restart device
- [ ] Clear app data: `adb shell pm clear org.ninelym.togai`
- [ ] Reinstall app: `./install_apk.sh`
- [ ] Check device storage
- [ ] Check USB debugging enabled
- [ ] Try different USB cable/port
- [ ] Update Android SDK tools
- [ ] Clear Gradle cache: `rm -rf ~/.gradle/caches`
- [ ] Rebuild: `./gradlew clean assembleDebug`

---

*For more help, see [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md)*
