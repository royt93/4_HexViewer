# 📋 Deprecated API Migration Plan - From Easy to Hard

**Date:** 2025-10-05
**Project:** HexViewer Android App
**Target SDK:** 36 (Android 16)
**Min SDK:** 23 (Android 6.0)

---

## 🎯 Summary

Total deprecated APIs found: **3 major items**

| Priority | Item | Difficulty | Impact | Files Affected |
|----------|------|-----------|--------|----------------|
| 1 | Network connectivity check | ⭐ Easy | Low | AdMobManager.kt |
| 2 | Legacy storage permissions (READ/WRITE_EXTERNAL_STORAGE) | ⭐⭐⭐ Medium-Hard | High | ActAbstractBaseMain.java, AndroidManifest.xml |
| 3 | requestLegacyExternalStorage flag | ⭐⭐⭐⭐ Hard | Critical | AndroidManifest.xml, Storage logic |

---

## ✅ Migration Tasks (Ordered by Difficulty)

### 1. ⭐ **EASY** - Network Connectivity Check (activeNetworkInfo)

**Current Deprecated Code:**
```kotlin
// AdMobManager.kt lines 662-663
val activeNetworkInfo = connectivityManager.activeNetworkInfo
return activeNetworkInfo != null && activeNetworkInfo.isConnected
```

**Deprecation Warning:**
```
'val activeNetworkInfo: NetworkInfo?' is deprecated. Deprecated in Java.
'val isConnected: Boolean' is deprecated. Deprecated in Java.
```

**Status:** ⚠️ Already partially migrated!
The code already has the correct modern implementation for API 23+ (lines 656-660), but falls back to deprecated API for API < 23.

**Issue:** The fallback is unnecessary because minSdk = 23!

**Migration (Already Done):**
```kotlin
// Lines 655-665 - Current code
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
} else {
    // ❌ This branch is NEVER executed (minSdk = 23 = API 23 = M)
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && activeNetworkInfo.isConnected
}
```

**✅ Fix Required:**
```kotlin
// Remove the unnecessary else branch
fun isConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // minSdk = 23 (Android M), so this is always true
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}
```

**Complexity:** ⭐ Very Easy
**Risk:** Very Low (code already uses modern API)
**Impact:** Removes deprecation warnings
**Testing:** Test network detection on WiFi/cellular/offline
**Files to modify:** `AdMobManager.kt` (lines 655-665)

---

### 2. ⭐⭐⭐ **MEDIUM-HARD** - Legacy Storage Permissions

**Current Deprecated Implementation:**

**AndroidManifest.xml:**
```xml
<!-- Lines 9-14 - Deprecated for targetSdk 36 -->
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
<uses-permission
    android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="29"
    tools:ignore="ScopedStorage" />

<!-- Line 23 - Deprecated flag -->
android:requestLegacyExternalStorage="true"
```

**ActAbstractBaseMain.java:**
```java
// Lines 73-75 - Deprecated permission requests
boolean requestPermissions = Build.VERSION.SDK_INT > Build.VERSION_CODES.Q ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
if (requestPermissions)
    ActivityCompat.requestPermissions(this, new String[]{
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    }, 1);
```

**Why Deprecated:**
- Android 10 (Q/API 29): Scoped Storage introduced, WRITE_EXTERNAL_STORAGE deprecated
- Android 11 (R/API 30): READ_EXTERNAL_STORAGE partially deprecated
- Android 13 (T/API 33): READ_EXTERNAL_STORAGE fully deprecated, replaced by granular media permissions
- Android 14+ (U/API 34+): Legacy storage completely removed

**Modern Replacement:**
```xml
<!-- AndroidManifest.xml - Use granular media permissions -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

<!-- For Android 12 and below compatibility -->
<uses-permission
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

**Migration Strategy:**

**Step 1:** Determine what files the app needs to access
- ✅ HexViewer reads binary files (any file type)
- ❓ Does it need to write files or only read?
- ❓ Does it need to access user media (images/video/audio) or general documents?

**Step 2:** Choose appropriate modern permissions
```kotlin
// Option A: If app only reads user-selected files (RECOMMENDED)
// Use Storage Access Framework (SAF) - NO permissions needed!
val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
    addCategory(Intent.CATEGORY_OPENABLE)
    type = "*/*"
}
startActivityForResult(intent, REQUEST_CODE)

// Option B: If app needs to read media files
// Use granular media permissions (Android 13+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    requestPermissions(arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO
    ), REQUEST_CODE)
} else {
    requestPermissions(arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    ), REQUEST_CODE)
}

// Option C: If app needs to manage all files (AVOID if possible)
// Requires special Play Store approval
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    if (!Environment.isExternalStorageManager()) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
}
```

**Step 3:** Update permission logic in ActAbstractBaseMain.java
```java
// Replace lines 73-75 with modern approach
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    // Android 13+: Use granular media permissions
    String[] permissions = new String[]{
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_AUDIO
    };
    boolean needsPermission = Arrays.stream(permissions)
        .anyMatch(p -> ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED);
    if (needsPermission) {
        ActivityCompat.requestPermissions(this, permissions, 1);
    }
} else {
    // Android 12 and below: Use legacy READ_EXTERNAL_STORAGE
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }
}

// RECOMMENDED: Consider using Storage Access Framework instead
// This requires NO permissions and works on all Android versions
```

**Complexity:** ⭐⭐⭐ Medium-Hard
**Risk:** High (affects core file access functionality)
**Impact:** Required for Android 14+ compatibility
**Testing Required:**
- Test file opening on Android 10, 11, 12, 13, 14
- Test different file types (binary, text, media)
- Test permission denial scenarios
- Verify Recently Opened files still work

**Files to modify:**
1. `AndroidManifest.xml` (lines 9-14, 23)
2. `ActAbstractBaseMain.java` (lines 73-75)
3. Potentially file picker logic

---

### 3. ⭐⭐⭐⭐ **HARD** - requestLegacyExternalStorage Flag

**Current Deprecated Code:**
```xml
<!-- AndroidManifest.xml line 23 -->
android:requestLegacyExternalStorage="true"
```

**Why Deprecated:**
- Android 10 (Q/API 29): Opt-in to legacy storage via this flag
- Android 11 (R/API 30): Flag still works for apps targeting API 29
- Android 11+ targeting API 30+: **Flag is IGNORED**
- Your app targets API 36: **This flag does NOTHING**

**What This Flag Was For:**
Allowed apps to bypass Scoped Storage and use old-style direct file paths like:
```java
// ❌ Deprecated approach (only worked with requestLegacyExternalStorage)
File file = new File("/storage/emulated/0/Documents/file.bin");
```

**Modern Replacement:**
Must use Scoped Storage or Storage Access Framework:
```java
// ✅ Option 1: Storage Access Framework (SAF) - RECOMMENDED
Uri uri = // from ACTION_OPEN_DOCUMENT intent
InputStream inputStream = getContentResolver().openInputStream(uri);

// ✅ Option 2: App-specific directory (no permission needed)
File appDir = getExternalFilesDir(null); // /Android/data/com.galaxyjoy.hexviewer/files/
File file = new File(appDir, "temp.bin");

// ✅ Option 3: MediaStore (for media files)
ContentResolver resolver = getContentResolver();
Uri collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
```

**Migration Strategy:**

**Step 1:** Audit all file operations in the codebase
```bash
# Search for direct File() usage with absolute paths
grep -r "new File(\"/storage" app/src/main/java/
grep -r "new File(Environment.getExternalStorageDirectory" app/src/main/java/
grep -r "getExternalStoragePublicDirectory" app/src/main/java/
```

**Step 2:** Replace with SAF or app-specific storage
```java
// OLD (doesn't work on Android 11+ targetSdk 30+):
File file = new File("/storage/emulated/0/Download/file.bin");
FileInputStream fis = new FileInputStream(file);

// NEW (works on all versions):
// Use ACTION_OPEN_DOCUMENT to let user pick file
Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
intent.addCategory(Intent.CATEGORY_OPENABLE);
intent.setType("*/*");
startActivityForResult(intent, PICK_FILE_REQUEST);

// In onActivityResult:
Uri uri = data.getData();
InputStream inputStream = getContentResolver().openInputStream(uri);
```

**Step 3:** Update file access in HexViewer logic
- ✅ Check how files are currently opened
- ✅ Check how Recently Opened files are stored/accessed
- ✅ Migrate to Uri-based file access instead of File paths
- ✅ Use DocumentFile API for file metadata

**Step 4:** Remove the flag
```xml
<!-- Remove this line from AndroidManifest.xml -->
<!-- android:requestLegacyExternalStorage="true" -->
```

**Complexity:** ⭐⭐⭐⭐ Hard
**Risk:** Very High (core functionality change)
**Impact:** Critical - app won't work on Android 11+ without this migration
**Testing Required:**
- Full regression testing on Android 10, 11, 12, 13, 14
- Test all file operations: open, save, recently opened
- Test with files from different locations (Downloads, Documents, external SD card)
- Performance testing (Uri-based access can be slower)

**Files to potentially modify:**
1. `AndroidManifest.xml` (line 23)
2. All file opening/saving logic
3. Recently Opened file tracking
4. File picker implementation

---

## 📊 Migration Priority Order

### Phase 1 - Quick Wins (1-2 hours)
1. ✅ Remove unnecessary network API fallback (⭐ Easy)
   - Fix: Remove else branch in AdMobManager.kt
   - Testing: 30 minutes

### Phase 2 - Permission Updates (4-8 hours)
2. ⚠️ Migrate to granular media permissions (⭐⭐⭐ Medium-Hard)
   - Analysis: Determine exact file access needs (1-2 hours)
   - Implementation: Update permissions + logic (2-3 hours)
   - Testing: Multi-version testing (2-3 hours)

### Phase 3 - Storage Overhaul (16-24 hours)
3. ⚠️ Remove requestLegacyExternalStorage + migrate to Scoped Storage (⭐⭐⭐⭐ Hard)
   - Code audit: Find all file operations (2-4 hours)
   - Architecture: Redesign file access layer (4-6 hours)
   - Implementation: Migrate to SAF/DocumentFile (6-8 hours)
   - Testing: Full regression testing (4-6 hours)

---

## ⚠️ Breaking Changes & Risks

### High Risk Items:
1. **Legacy Storage Migration** (Item #3)
   - **Risk:** App may not open files on Android 11+
   - **Mitigation:** Thorough testing on Android 10-14
   - **Rollback Plan:** Keep SAF implementation optional initially

2. **Permission Changes** (Item #2)
   - **Risk:** Users may deny new permissions
   - **Mitigation:** Explain why permissions are needed
   - **Fallback:** Graceful degradation (use SAF if permissions denied)

### Low Risk Items:
1. **Network API Cleanup** (Item #1)
   - **Risk:** Minimal (code already uses modern API)
   - **Mitigation:** Test on WiFi/cellular/offline

---

## ✅ Testing Checklist

### For Each Migration:
- [ ] Test on Android 6.0 (API 23 - minSdk)
- [ ] Test on Android 10 (API 29 - Scoped Storage introduced)
- [ ] Test on Android 11 (API 30 - requestLegacyExternalStorage ignored)
- [ ] Test on Android 13 (API 33 - Granular media permissions)
- [ ] Test on Android 14+ (API 34+ - Legacy storage removed)

### Specific Test Cases:
- [ ] Network detection (WiFi/cellular/offline/airplane mode)
- [ ] File opening from different sources (Downloads, Documents, SD card)
- [ ] Recently Opened files still accessible
- [ ] Permission grant/deny scenarios
- [ ] App upgrade from old version (data migration)

---

## 🎓 Recommendations

### Immediate Action (Ship Blocker):
1. **Fix Item #1** (Network API) - 1 hour work
   - Low risk, removes deprecation warnings
   - Deploy immediately

### Short Term (Next Release):
2. **Fix Item #2** (Permissions) - 1 week work
   - Medium risk, required for Android 13+ best practices
   - Test thoroughly before release

### Long Term (Major Version):
3. **Fix Item #3** (Storage) - 2-3 weeks work
   - High risk, fundamental architecture change
   - Consider as part of major version update
   - May require UX changes (file picker flow)

### Alternative Approach:
Consider using Storage Access Framework (SAF) for everything:
- ✅ No permissions needed
- ✅ Works on all Android versions
- ✅ User has full control
- ❌ Slightly different UX (system file picker)
- ❌ Uri-based instead of File paths (code changes needed)

---

## 📝 Notes

### Current Status:
- ✅ App builds successfully with deprecation warnings
- ⚠️ Deprecated APIs still work but may break in future Android versions
- ⚠️ requestLegacyExternalStorage flag **already does nothing** (targetSdk 36)

### Why These APIs Were Deprecated:
1. **Network API:** Better suited for modern network types (WiFi, cellular, VPN, Ethernet)
2. **Storage Permissions:** Privacy & security (apps shouldn't access all files)
3. **Legacy Storage:** User data protection (Scoped Storage prevents data leaks)

### Migration Philosophy:
**"Fix what's broken first, optimize later"**
- Item #1: Fix now (easy, safe)
- Item #2: Fix next release (medium difficulty, important)
- Item #3: Plan major refactor (hard, but critical for long-term compatibility)

---

**Report Status:** ✅ ANALYSIS COMPLETE
**Next Step:** Start with Phase 1 (Network API fix - 1 hour)
**Estimated Total Migration Time:** 24-35 hours
**Risk Level:** Medium-High (storage migration has risks)
**Recommendation:** Migrate incrementally, test thoroughly, consider SAF migration ✅

---

**Report generated:** 2025-10-05
**Analysis Depth:** Full codebase scan + Android API audit
**Deprecated APIs Found:** 3 items (1 easy, 1 medium-hard, 1 hard)
**Action Required:** Yes - at least fix Item #1 immediately ⚠️
