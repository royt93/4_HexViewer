# Test Cases — SplashActivity isTaskRoot() Fix

**Fix:** `SplashActivity.onCreate()` gọi `finish()` ngay khi `!isTaskRoot()` để tránh tạo SplashActivity thừa khi user tap icon trong lúc app đang chạy.

**File thay đổi:** `app/src/main/java/com/galaxyjoy/hexviewer/ui/act/SplashActivity.java`

**Device test:** SM-F731B (Z Flip 5) — `R5CX626GNXL`

---

## TC-01 — Cold start bình thường

**Mục tiêu:** Đảm bảo fix không ảnh hưởng cold start.

**Precondition:** App chưa chạy (force stop hoặc reboot).

**Steps:**
1. Force stop app: `adb shell am force-stop com.galaxyjoy.hexviewer`
2. Tap icon app từ launcher
3. Quan sát màn hình splash

**Expected:**
- Splash screen hiện đầy đủ (animation, logo)
- Chuyển sang ActMain sau khi consent/ad flow hoàn thành
- `isTaskRoot()` = true → KHÔNG vào nhánh finish()

**Log cần có:**
```
PROCESS STARTED for package com.galaxyjoy.hexviewer
[Application] Default system locale
AdManager initialize ✅
[ActMain] onResume
```

**Log KHÔNG được có:**
```
lifecycle onActivityCreated: SplashActivity  (xuất hiện lần 2 trong cùng session)
```

---

## TC-02 — Tap icon khi ActMain đang foreground

**Mục tiêu:** Fix chính — SplashActivity không được hiện khi app đã chạy.

**Precondition:** App đang chạy, ActMain đang ở foreground.

**Steps:**
1. Mở app → chờ vào ActMain
2. Nhấn Home
3. Tap icon app từ launcher

**Expected:**
- ActMain trở lại foreground ngay, **không có màn hình splash**
- Không thấy animation splash, không thấy logo xuất hiện

**Log cần có:**
```
lifecycle onActivityStarted: ActMain
lifecycle onActivityResumed: ActMain
```

**Log KHÔNG được có:**
```
lifecycle onActivityCreated: SplashActivity
lifecycle onActivityStarted: SplashActivity
```

---

## TC-03 — Tap icon khi ActMain đang background (có file mở)

**Mục tiêu:** App đang background với file đang xem, resume về đúng file.

**Precondition:** App đang chạy với file hex đang mở, app ở background.

**Steps:**
1. Mở file bất kỳ (vd: zf.png)
2. Nhấn Home → mở app khác
3. Tap icon app từ launcher

**Expected:**
- ActMain resume lại với đúng file đang xem
- File state không bị reset (hexVisible, plainVisible giữ nguyên)
- Không có SplashActivity

**Log cần có:**
```
[ActMain] onResume | file=zf.png | hexVisible=true
```

---

## TC-04 — Tap icon từ Recent Apps

**Mục tiêu:** Recent app switcher vẫn hoạt động đúng.

**Precondition:** App đang background.

**Steps:**
1. Nhấn Home
2. Mở Recent Apps (vuốt lên giữ)
3. Tap vào HexViewer trong recent list

**Expected:**
- ActMain resume bình thường
- Không tạo SplashActivity mới

---

## TC-05 — Multiple tap icon liên tiếp

**Mục tiêu:** Spam tap icon không gây crash hay state lạ.

**Precondition:** App đang chạy.

**Steps:**
1. Nhấn Home
2. Tap icon app 5 lần nhanh liên tiếp

**Expected:**
- App ổn định, không crash
- ActMain chỉ resume 1 lần (hoặc focus lại đúng)
- Không có 5 SplashActivity trong back stack

---

## TC-06 — Deep link khi app đang chạy

**Mục tiêu:** Deep link / intent từ bên ngoài vẫn xử lý đúng.

**Precondition:** App đang chạy ở ActMain.

**Steps:**
1. Dùng ADB gửi intent mở file:
   ```
   adb shell am start -a android.intent.action.VIEW \
     -d "content://..." \
     com.galaxyjoy.hexviewer
   ```

**Expected:**
- File được mở trong ActMain (qua `handleIntent`)
- Không tạo SplashActivity thừa

---

## TC-07 — Tap icon sau khi mở ActVipManagement

**Mục tiêu:** SplashActivity không xuất hiện khi VIP screen đang foreground.

**Precondition:** App đang chạy, ActVipManagement đang mở.

**Steps:**
1. Vào ActMain → tap VIP button
2. Nhấn Home
3. Tap icon app từ launcher

**Expected:**
- App resume về ActVipManagement (hoặc ActMain, tùy back stack)
- Không có SplashActivity

---

## Kết quả

| TC | Mô tả | Expected | Kết quả | Ghi chú |
|---|---|---|---|---|
| TC-01 | Cold start bình thường | PASS | ✅ PASS | SplashActivity duy nhất, không tạo lần 2 |
| TC-02 | Tap icon khi ActMain foreground | PASS | ✅ PASS | Splash created→destroyed, không có onStart/onResume |
| TC-03 | Tap icon khi có file mở | PASS | ✅ PASS | ActMain resume đúng state, không có Splash |
| TC-04 | Tap từ Recent Apps | PASS | ✅ PASS | ActMain started→resumed trực tiếp |
| TC-05 | Spam tap icon 5 lần | PASS | ✅ PASS | 5× created/destroyed ~400ms/cycle, không crash |
| TC-06 | Deep link khi app đang chạy | PASS | ✅ PASS | ActMain pause→resume, không tạo Splash |
| TC-07 | Tap icon sau VIP screen | PASS | ✅ PASS | Splash created→destroyed, ActMain lên đúng |
