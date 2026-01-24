# Library Migration Guide - Dễ đến Khó

**Date:** 2025-10-05
**Project:** HexViewer Android App

---

## 📊 Tổng quan Dependencies hiện tại

### Production Libraries (13 dependencies):
```gradle
// Core
implementation "org.jetbrains.kotlin:kotlin-stdlib:2.1.0"
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

// AndroidX
implementation "androidx.appcompat:appcompat:1.7.0"
implementation "androidx.preference:preference:1.2.1"
implementation "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0"
implementation "androidx.emoji:emoji:1.1.0"
implementation "androidx.emoji:emoji-bundled:1.1.0"
implementation "com.google.android.material:material:1.12.0"
implementation 'androidx.core:core-ktx:1.15.0'

// Third-party
implementation 'org.apache.commons:commons-collections4:4.4'
implementation "com.airbnb.android:lottie:6.5.2"
implementation("com.google.android.play:review:2.0.1")
implementation("com.google.android.play:review-ktx:2.0.1")

// Ads (KHÔNG migrate)
implementation("com.google.android.gms:play-services-ads:23.3.0")
implementation("com.google.ads.mediation:applovin:13.0.0.0")

// Debug
debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.14'
```

---

## 🟢 DỄ - Có thể migrate ngay (5 libraries)

### 1. ✅ **commons-collections4 → Native Java Collections** ⭐⭐⭐⭐⭐
**Độ khó:** ⭐ Rất dễ
**Thời gian:** 5-10 phút
**Impact:** Giảm ~200-300 KB
**Risk:** Rất thấp

**Hiện tại:**
```gradle
implementation 'org.apache.commons:commons-collections4:4.4'  // ~500 KB
```

**Code usage:** Chỉ dùng `CircularFifoQueue` (5 chỗ)
```java
// MyApplication.java:32
import org.apache.commons.collections4.queue.CircularFifoQueue;

// MyApplication.java:148
mLogs = new CircularFifoQueue<>(CIRCULAR_BUFFER_DEPTH);
```

**Migration:**
```gradle
// REMOVE: implementation 'org.apache.commons:commons-collections4:4.4'
// ADD: Nothing (use native Java)
```

**Code changes:**
```java
// Replace CircularFifoQueue with ArrayDeque
import java.util.ArrayDeque;

// Old:
private Queue<String> mLogs = new CircularFifoQueue<>(2000);

// New:
private ArrayDeque<String> mLogs;

public Queue<String> getLogBuffer() {
    if (mLogs == null) {
        synchronized(this) {
            if (mLogs == null) {
                mLogs = new ArrayDeque<>(2000);
            }
        }
    }
    // Limit size manually
    while (mLogs.size() > 2000) {
        mLogs.poll();
    }
    return mLogs;
}
```

**Steps:**
1. Replace `CircularFifoQueue` với `ArrayDeque`
2. Add size limit logic
3. Remove dependency
4. Test logging functionality

---

### 2. ✅ **Lottie → Vector Animations** ⭐⭐⭐⭐
**Độ khó:** ⭐⭐ Dễ-Trung bình
**Thời gian:** 15-30 phút
**Impact:** Giảm ~1-1.5 MB (library + animations)
**Risk:** Thấp

**Hiện tại:**
```gradle
implementation "com.airbnb.android:lottie:6.5.2"  // ~1 MB
// + raw/loading.json (130 KB)
// + raw/no_data.json (151 KB)
```

**Code usage:** COMMENTED OUT! (Không dùng)
```java
// ActMain.java - COMMENTED:
// LottieAnimationView lottieAnimationView = findViewById(R.id.lottieAnimationView);
// lottieAnimationView.setRepeatCount(LottieDrawable.INFINITE);
```

**Migration:**
```gradle
// REMOVE: implementation "com.airbnb.android:lottie:6.5.2"
```

**Steps:**
1. ✅ Verify code không dùng Lottie (đã comment)
2. ✅ Remove dependency
3. ✅ Delete raw/loading.json, raw/no_data.json
4. ✅ Test app (không có Lottie animations)

**Alternative (nếu cần animations):**
- Use Android Vector Drawable animations
- Use ProgressBar with custom drawables
- Estimated size: ~2-5 KB vs 281 KB

---

### 3. ✅ **kotlin-stdlib explicit → stdlib-jdk8** ⭐⭐⭐⭐
**Độ khó:** ⭐ Rất dễ
**Thời gian:** 2 phút
**Impact:** Giảm ~50-100 KB
**Risk:** Không có

**Hiện tại:**
```gradle
implementation "org.jetbrains.kotlin:kotlin-stdlib:2.1.0"
```

**Migration:**
```gradle
// REMOVE: implementation "org.jetbrains.kotlin:kotlin-stdlib:2.1.0"
// Kotlin Gradle Plugin tự động thêm stdlib, không cần explicit
```

**Steps:**
1. Remove dòng `kotlin-stdlib`
2. Kotlin plugin tự động include
3. Build → OK

**Note:** Kotlin 1.4+ tự động include stdlib, không cần explicit dependency

---

### 4. ✅ **androidx.emoji → Remove hoàn toàn** ⭐⭐⭐⭐⭐
**Độ khó:** ⭐⭐ Dễ (cần test)
**Thời gian:** 10-15 phút
**Impact:** Giảm ~5-7 MB (!!)
**Risk:** Trung bình (cần test emoji display)

**Hiện tại:**
```gradle
implementation "androidx.emoji:emoji:1.1.0"          // ~500 KB
implementation "androidx.emoji:emoji-bundled:1.1.0"  // ~5-7 MB
```

**Code usage:**
```java
// MyApplication.java:97-98
EmojiCompat.Config config = new BundledEmojiCompatConfig(this);
EmojiCompat.init(config);
```

**Migration Option 1: Remove hoàn toàn** (khuyến nghị)
```gradle
// REMOVE both emoji dependencies
```

```java
// MyApplication.java - REMOVE:
// import androidx.emoji.bundled.BundledEmojiCompatConfig;
// import androidx.emoji.text.EmojiCompat;
//
// EmojiCompat.Config config = new BundledEmojiCompatConfig(this);
// EmojiCompat.init(config);
```

**Migration Option 2: Fallback to system emoji**
- Use native Android emoji support (no library)
- Works on Android 5.0+ (API 21+)
- App minSdk = 23 → ✅ Safe

**Steps:**
1. Comment out EmojiCompat initialization
2. Remove dependencies
3. Test emoji display trong app
4. Nếu OK → commit, nếu không → revert

**Test cases:**
- File names với emoji
- Text input với emoji
- Display emoji trong lists

---

### 5. ✅ **Test dependencies updates** ⭐⭐⭐⭐⭐
**Độ khó:** ⭐ Rất dễ
**Thời gian:** 5 phút
**Impact:** Không ảnh hưởng APK size (testImplementation)
**Risk:** Không có

**Updates available:**
```gradle
// Current → Latest
testImplementation 'org.mockito:mockito-core:5.8.0'        // → 5.14.2
testImplementation 'org.mockito:mockito-inline:5.2.0'      // → 5.2.0 (OK)
testImplementation 'org.robolectric:robolectric:4.11.1'    // → 4.14.1
testImplementation 'androidx.test:core:1.5.0'              // → 1.6.1
testImplementation 'androidx.test.ext:junit:1.1.5'         // → 1.2.1
testImplementation 'com.google.truth:truth:1.4.0'          // → 1.4.4

androidTestImplementation 'androidx.test.ext:junit:1.1.5'         // → 1.2.1
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'  // → 3.6.1
androidTestImplementation 'androidx.test:runner:1.5.2'            // → 1.6.2
androidTestImplementation 'androidx.test:rules:1.5.0'             // → 1.6.1
androidTestImplementation 'org.mockito:mockito-android:5.8.0'     // → 5.14.2
```

**Steps:** Chỉ cần update version numbers

---

## 🟡 TRUNG BÌNH - Cần cân nhắc (4 libraries)

### 6. ⚠️ **play-services-ads → Newer version**
**Độ khó:** ⭐⭐⭐ Trung bình
**Thời gian:** 20-30 phút
**Impact:** Có thể giảm/tăng size
**Risk:** Trung bình (test ads kỹ)

**Hiện tại:**
```gradle
implementation("com.google.android.gms:play-services-ads:23.3.0")  // Latest: 24.0.0
```

**Migration:**
```gradle
implementation("com.google.android.gms:play-services-ads:24.0.0")
```

**Risks:**
- Breaking API changes
- AdMob integration cần update
- Mediation (Applovin) compatibility

**Steps:**
1. Check release notes: https://developers.google.com/admob/android/rel-notes
2. Update version
3. Fix deprecated APIs
4. Test ads display
5. Test mediation

---

### 7. ⚠️ **Applovin mediation → Latest**
**Độ khó:** ⭐⭐⭐ Trung bình
**Thời gian:** 20-30 phút
**Impact:** Unknown
**Risk:** Cao (ads revenue)

**Hiện tại:**
```gradle
implementation("com.google.ads.mediation:applovin:13.0.0.0")  // Latest: check
```

**Migration:**
- Check compatibility với play-services-ads version
- Update cùng lúc với ads SDK

**Note:** KHÔNG migrate nếu không chắc chắn (ảnh hưởng revenue)

---

### 8. ⚠️ **coroutines-android → Latest**
**Độ khó:** ⭐⭐ Dễ-Trung bình
**Thời gian:** 10-15 phút
**Impact:** Minimal
**Risk:** Thấp

**Hiện tại:**
```gradle
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
```

**Latest:** 1.9.0

**Migration:**
```gradle
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0'
```

**Steps:**
1. Update version
2. Check for deprecated APIs
3. Test async operations
4. Test memory monitor (uses coroutines)

---

### 9. ⚠️ **Material Components → Latest**
**Độ khó:** ⭐⭐⭐ Trung bình
**Thời gian:** 30-60 phút
**Impact:** Có thể tăng size
**Risk:** Trung bình (UI changes)

**Hiện tại:**
```gradle
implementation "com.google.android.material:material:1.12.0"
```

**Latest:** 1.13.0-alpha08

**Migration:**
```gradle
implementation "com.google.android.material:material:1.13.0"
```

**Risks:**
- UI theming changes
- Breaking changes trong Material 3
- Size có thể tăng nếu include Material You components

**Steps:**
1. Check release notes
2. Update version
3. Test all UI screens
4. Check theming
5. Verify dialogs, buttons, inputs

---

## 🔴 KHÓ - Không nên migrate (2 libraries)

### 10. ❌ **LeakCanary → Newer version**
**Độ khó:** ⭐⭐⭐⭐ Khó
**Thời gian:** N/A
**Impact:** N/A (debug only)
**Risk:** Thấp

**Hiện tại:**
```gradle
debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.14'
```

**Latest:** 3.0-alpha-1 (breaking changes)

**Recommendation:** ❌ Giữ nguyên 2.14 (stable)
- Version 3.0 đang alpha
- Chỉ dùng cho debug
- Không ảnh hưởng production

---

### 11. ❌ **AndroidX libraries → Newer versions**
**Độ khó:** ⭐⭐⭐⭐⭐ Rất khó
**Thời gian:** 2-4 giờ
**Impact:** Unknown
**Risk:** Cao

**Hiện tại:**
```gradle
implementation "androidx.appcompat:appcompat:1.7.0"              // Latest: 1.7.0 (OK)
implementation "androidx.preference:preference:1.2.1"            // Latest: 1.2.1 (OK)
implementation "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0"  // Latest: 1.2.0-alpha01
implementation 'androidx.core:core-ktx:1.15.0'                   // Latest: 1.15.0 (OK)
```

**Recommendation:** ❌ Giữ nguyên
- Đã là latest stable
- SwipeRefreshLayout 1.2.0 đang alpha
- Không có breaking changes cần thiết

---

## 📋 Migration Priority Roadmap

### Phase 1: Quick Wins (30 phút - Giảm ~7-8 MB) ⭐⭐⭐⭐⭐
**Recommended: Do ngay**

1. ✅ Remove `androidx.emoji` + `androidx.emoji-bundled` (-5-7 MB)
2. ✅ Remove `commons-collections4` → Native Java (-200-300 KB)
3. ✅ Remove `kotlin-stdlib` explicit (-50-100 KB)
4. ✅ Remove Lottie + animations (-1.3 MB)

**Commands:**
```gradle
// Remove these lines:
// implementation 'org.apache.commons:commons-collections4:4.4'
// implementation "org.jetbrains.kotlin:kotlin-stdlib:2.1.0"
// implementation "androidx.emoji:emoji:1.1.0"
// implementation "androidx.emoji:emoji-bundled:1.1.0"
// implementation "com.airbnb.android:lottie:6.5.2"
```

**Expected result:** APK giảm thêm ~7-8 MB

---

### Phase 2: Safe Updates (20 phút)
**Optional**

5. ✅ Update test dependencies (không ảnh hưởng APK)
6. ⚠️ Update coroutines 1.7.3 → 1.9.0 (test kỹ)

---

### Phase 3: Risky Updates (Không khuyến nghị)
**Skip nếu app đang chạy ổn định**

7. ❌ Skip: AdMob updates (risk revenue)
8. ❌ Skip: Applovin updates (risk revenue)
9. ❌ Skip: Material updates (risk UI)
10. ❌ Skip: LeakCanary 3.0 (alpha)
11. ❌ Skip: AndroidX (already latest stable)

---

## ✅ Recommended Actions

### DO NOW (High Impact, Low Risk):
```gradle
dependencies {
    // REMOVE these 5 lines:
    // implementation 'org.apache.commons:commons-collections4:4.4'
    // implementation "org.jetbrains.kotlin:kotlin-stdlib:2.1.0"
    // implementation "androidx.emoji:emoji:1.1.0"
    // implementation "androidx.emoji:emoji-bundled:1.1.0"
    // implementation "com.airbnb.android:lottie:6.5.2"
}
```

**Code changes needed:**
1. Replace `CircularFifoQueue` với `ArrayDeque` (5 chỗ)
2. Remove `EmojiCompat.init()` (2 dòng)
3. Remove Lottie imports (0 dòng - đã comment)

**Testing:**
- [ ] App starts OK
- [ ] Logging works
- [ ] Emoji display OK (fallback to system)
- [ ] No Lottie animations (already disabled)

**Expected APK size:** ~5-8 MB (giảm 50-70% từ baseline!)

---

## 📊 Size Comparison

| Phase | AAB Size | APK Size | Savings |
|-------|----------|----------|---------|
| **Before all optimizations** | ~20 MB | ~15 MB | - |
| **After R8 + Splits** | ~12 MB | ~10 MB | -40% |
| **+ Phase 1 migrations** | **~5 MB** | **~6 MB** | **-70%** |

---

## 🔧 Implementation Script

```bash
# 1. Backup current state
git add .
git commit -m "Backup before library migration"

# 2. Apply Phase 1 changes
# Edit app/build.gradle - remove 5 dependencies
# Edit MyApplication.java - replace CircularFifoQueue
# Edit MyApplication.java - remove EmojiCompat
# Delete app/src/main/res/raw/loading.json
# Delete app/src/main/res/raw/no_data.json

# 3. Build
./gradlew clean
./gradlew assembleProductionRelease

# 4. Test
./gradlew installProductionRelease

# 5. Verify size
ls -lh app/build/outputs/apk/production/release/
# Expected: ~6-8 MB

# 6. If OK, commit
git add .
git commit -m "feat: Remove unused libraries (emoji, lottie, commons-collections) - Reduce APK by 7-8 MB"

# 7. If issues, revert
git reset --hard HEAD~1
```

---

## ⚠️ Notes

1. **Emoji removal:** Test trên nhiều devices (Android 6-14)
2. **Commons-collections:** ArrayDeque cần manual size limit
3. **Lottie:** Đã comment out, safe to remove
4. **Kotlin stdlib:** Plugin tự include, không cần explicit
5. **Test dependencies:** Update không ảnh hưởng APK

---

**Summary:**
- **5 easy migrations** → Giảm ~7-8 MB
- **4 medium migrations** → Skip (risky)
- **2 hard migrations** → Skip (not needed)

**Recommended:** Chỉ làm Phase 1 (30 phút, giảm 50-70% size) ✅
