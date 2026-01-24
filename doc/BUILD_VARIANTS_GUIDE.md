# Build Variants Guide - HexViewer App

## 🔴 QUAN TRỌNG: LeakCanary Warning

### ⚠️ Vấn đề bạn gặp:
```
hprof: heap dump "/storage/emulated/0/Download/leakcanary-com.galaxyjoy.hexviewer/..."
```

**Nguyên nhân:** Đang chạy **dev build** với LeakCanary enabled

**Fix:** Build đúng variant cho production!

---

## 📦 Build Variants Explained

Project có **4 build variants:**

### 1. **devDebug** ❌ (Không dùng cho production)
- Flavor: `dev`
- Build type: `debug`
- LeakCanary: ✅ ENABLED
- Minify: ❌ Disabled
- Shrink: ❌ Disabled
- Use case: Development & debugging memory leaks

### 2. **devRelease** ⚠️ (Vẫn có LeakCanary!)
- Flavor: `dev`
- Build type: `release`
- LeakCanary: ✅ ENABLED (vì dev flavor)
- Minify: ✅ Enabled
- Shrink: ✅ Enabled
- Use case: Testing release build với leak detection

### 3. **productionDebug** ❌ (Không dùng)
- Flavor: `production`
- Build type: `debug`
- LeakCanary: ❌ DISABLED
- Minify: ❌ Disabled
- Use case: Debug production flavor (rare)

### 4. **productionRelease** ✅ (DÙNG CÁI NÀY!)
- Flavor: `production`
- Build type: `release`
- LeakCanary: ❌ DISABLED
- Minify: ✅ Enabled
- Shrink: ✅ Enabled
- Optimizations: ✅ Full R8 + ProGuard
- **Use case: PRODUCTION BUILD cho Play Store**

---

## 🚀 Cách Build Đúng

### Option 1: Command Line (Recommended)

```bash
# 1. Clean project
./gradlew clean

# 2. Build production release AAB (cho Play Store)
./gradlew bundleProductionRelease

# 3. Hoặc build APK (cho testing)
./gradlew assembleProductionRelease

# Output locations:
# AAB: app/build/outputs/bundle/productionRelease/app-production-release.aab
# APK: app/build/outputs/apk/production/release/app-production-release.apk
```

### Option 2: Android Studio

1. **Select Build Variant:**
   - View → Tool Windows → Build Variants
   - Chọn: **productionRelease**

2. **Build:**
   - Build → Generate Signed Bundle / APK
   - Chọn: Android App Bundle
   - Build variant: productionRelease

---

## 🔍 Verify LeakCanary Disabled

### Check 1: Logcat
```bash
# Nếu thấy dòng này → SAI variant!
"leakcanary"
"heap dump"

# Production build KHÔNG có dòng này
```

### Check 2: APK Analyzer
```bash
# Build productionRelease
./gradlew assembleProductionRelease

# Analyze APK
# Build → Analyze APK → Select APK

# Check dependencies → KHÔNG thấy "leakcanary"
```

### Check 3: App size
```bash
# Dev build (có LeakCanary): ~18-25 MB
# Production build: ~8-15 MB (nhỏ hơn 40-60%)
```

---

## ⚙️ Current Build Configuration

### LeakCanary setup (CORRECT ✅):
```gradle
dependencies {
    // LeakCanary CHỈ trong debug builds
    debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.14'

    // Không có trong release builds → OK!
}
```

### Build Types:
```gradle
buildTypes {
    release {
        minifyEnabled true          // ✅ Shrink code
        shrinkResources true        // ✅ Remove unused resources
        debuggable false            // ✅ No debug
        // LeakCanary auto-disabled ✅
    }
    debug {
        minifyEnabled false
        // LeakCanary auto-enabled ✅
    }
}
```

### Product Flavors:
```gradle
productFlavors {
    dev {
        // For development
        buildConfigField "Boolean", "build_debug", "true"
    }
    production {
        // For Play Store
        buildConfigField "Boolean", "build_debug", "false"
    }
}
```

---

## 🎯 Checklist Before Publishing

### Pre-build checks:
- [ ] Select variant: **productionRelease**
- [ ] Clean project: `./gradlew clean`
- [ ] Update version code/name

### Build:
- [ ] Run: `./gradlew bundleProductionRelease`
- [ ] Verify output: `app/build/outputs/bundle/productionRelease/`

### Post-build verification:
- [ ] Check AAB size (~8-15 MB expected)
- [ ] Install and run on device
- [ ] Check logcat - NO "leakcanary" messages
- [ ] Verify all features work
- [ ] Check memory usage (should be lower)

---

## 🐛 Common Issues

### Issue 1: "Still seeing LeakCanary in production"
**Solution:**
- Verify build variant: `./gradlew tasks --all | grep assemble`
- Must build: `assembleProductionRelease` NOT `assembleDevRelease`

### Issue 2: "APK too large"
**Solution:**
- Check variant: Must be `productionRelease`
- Check LeakCanary disabled: Should NOT be in dependencies
- Run APK Analyzer: Build → Analyze APK

### Issue 3: "App crashes in production"
**Solution:**
- Test with: `productionDebug` first (no LeakCanary, yes debug)
- Check ProGuard rules if crashes
- Review crash logs

---

## 📊 Expected Sizes

| Variant | AAB Size | APK Size | LeakCanary |
|---------|----------|----------|------------|
| devDebug | N/A | ~25 MB | ✅ Yes |
| devRelease | ~20 MB | ~18 MB | ✅ Yes |
| productionDebug | N/A | ~15 MB | ❌ No |
| **productionRelease** | **~12 MB** | **~10 MB** | ❌ No |

---

## 🔧 Quick Fix Commands

```bash
# 1. CORRECT: Build production release
./gradlew clean bundleProductionRelease

# 2. WRONG: Don't use these for production
# ./gradlew bundleDevRelease        ← Has LeakCanary!
# ./gradlew assembleDevDebug        ← Has LeakCanary!

# 3. Install production APK for testing
./gradlew installProductionRelease

# 4. Verify no LeakCanary
adb logcat | grep -i leakcanary
# Should show: (nothing) or (empty)
```

---

## ✅ Summary

### ❌ Đang làm SAI:
- Build variant: `devRelease` or `devDebug`
- LeakCanary: ✅ Enabled
- APK size: ~18-25 MB
- Heap dumps: ✅ Creating files

### ✅ Cần làm ĐÚNG:
- Build variant: `productionRelease`
- LeakCanary: ❌ Disabled
- APK size: ~8-15 MB
- Heap dumps: ❌ None

### 🎯 Action Required:
```bash
# Execute this NOW:
./gradlew clean
./gradlew bundleProductionRelease

# Then verify:
ls -lh app/build/outputs/bundle/productionRelease/
# Expected: ~10-15 MB (NOT 20+ MB)
```

---

**Remember:**
- 🟢 **productionRelease** = Play Store build (no LeakCanary)
- 🔴 **devDebug/devRelease** = Development only (has LeakCanary)
