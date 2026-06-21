# FULL TEST PLAN — HexViewer

> **App:** HexViewer | **Package:** `com.galaxyjoy.hexviewer`
> **Cập nhật:** 2026-06-21 | **Dành cho:** Automation Test (Espresso / UI Automator)

---

## MỤC LỤC

1. [Splash Screen](#1-splash-screen)
2. [Main Screen — ActMain](#2-main-screen--actmain)
3. [Mở File — Open Flow](#3-mở-file--open-flow)
4. [Hex Viewer](#4-hex-viewer)
5. [Hex Editor — Line Update](#5-hex-editor--line-update)
6. [Partial Open (Mở theo byte range)](#6-partial-open-mở-theo-byte-range)
7. [Recently Opened (Mở gần đây)](#7-recently-opened-mở-gần-đây)
8. [Hash Calculator](#8-hash-calculator)
9. [File Info (Phát hiện loại file)](#9-file-info-phát-hiện-loại-file)
10. [Settings (Cài đặt)](#10-settings-cài-đặt)
11. [VIP Management](#11-vip-management)
12. [GoTo Dialog](#12-goto-dialog)
13. [Save File](#13-save-file)
14. [Search (Tìm kiếm)](#14-search-tìm-kiếm)
15. [Undo / Redo](#15-undo--redo)
16. [Multi-select (Chọn nhiều dòng)](#16-multi-select-chọn-nhiều-dòng)
17. [About Screen](#17-about-screen)
18. [Lifecycle & Memory](#18-lifecycle--memory)
19. [Permissions](#19-permissions)
20. [Orientation & Configuration Changes](#20-orientation--configuration-changes)

---

## LEGEND

| Ký hiệu | Nghĩa |
|---------|-------|
| ✅ Pass | Test đạt yêu cầu |
| ❌ Fail | Test thất bại cần fix |
| 🔵 Manual | Cần test thủ công trên device |
| 🤖 Auto | Espresso / Robolectric / UI Automator |
| `[FREE]` | User chưa có VIP |
| `[VIP]` | User đã kích hoạt VIP |
| `[OFFLINE]` | Tắt WiFi + mobile data |
| `[ONLINE]` | Bật mạng bình thường |

---

## 1. SPLASH SCREEN

**Activity:** `SplashActivity` | **Layout:** `activity_splash.xml`

### TC-SPLASH-001 — Launch lần đầu → hiển thị Splash 🤖
```
Precondition: App chưa chạy (fresh launch)
Act:          Launch app từ launcher icon
Assert:       - SplashActivity hiển thị
              - App name có animation zoom-in + fade
              - Progress bar container fade-in
              - Loading text có pulse animation
              - Ad notice card slide-up từ bottom
```

### TC-SPLASH-002 — Splash không bị hiện lại khi app đang chạy 🔵
```
Precondition: ActMain đang mở
Act:          Nhấn Home → tap lại launcher icon
Assert:       - SplashActivity KHÔNG hiển thị
              - ActMain được đưa lên foreground trực tiếp
              - isTaskRoot() == false → SplashActivity finish() ngay lập tức
```

### TC-SPLASH-003 — Offline: Skip consent → vào thẳng ActMain 🔵
```
Precondition: [OFFLINE] Tắt mạng
Act:          Launch app
Assert:       - SplashActivity hiển thị ngắn
              - Không hiện dialog consent
              - Chuyển sang ActMain
              - Không crash
```

### TC-SPLASH-004 — Online: Consent flow → App Open Ad (nếu có) 🔵
```
Precondition: [ONLINE] Bật mạng, lần đầu dùng app
Act:          Launch app
Assert:       - requestConsentInfoUpdate được gọi
              - Nếu canRequestAds=true → runSplashAdFlow() → hiện App Open Ad
              - Sau ad → chuyển ActMain
```

### TC-SPLASH-005 — Timeout 5s: consent stall → fallback vào ActMain 🔵
```
Precondition: [ONLINE] Mạng yếu, consent callback không về
Act:          Launch app, đợi 5 giây
Assert:       - consentTimeoutRunnable kích hoạt sau 5s
              - App chuyển sang ActMain (không bị hang)
```

### TC-SPLASH-006 — Animation hủy khi back/destroy 🤖
```
Precondition: SplashActivity đang chạy animation
Act:          Gọi finish() hoặc press Back
Assert:       - Tất cả ViewPropertyAnimator bị cancel()
              - Không có memory leak
              - Không crash "View already has animation"
```

---

## 2. MAIN SCREEN — ACTMAIN

**Activity:** `ActMain` | **Layout:** `act_main.xml`

### TC-MAIN-001 — Màn hình chính hiển thị đủ thành phần [FREE] 🤖
```
Precondition: App vừa launch, chưa mở file
Act:          Quan sát ActMain
Assert:       - Toolbar hiển thị tên app
              - Button "Open File" hiển thị
              - Button "Hash Calculator" hiển thị
              - Button "File Info" hiển thị
              - Button "Recently Open" hiển thị (có thể disabled nếu list rỗng)
              - VIP badge/pill hiển thị ở toolbar
              - Banner ad hiển thị ở bottom (nếu FREE)
```

### TC-MAIN-002 — VIP badge animation khi FREE 🤖
```
Precondition: [FREE] User chưa VIP
Act:          Quan sát VIP badge pill
Assert:       - Pill có animation pulse (scale 1.0 → 1.04 liên tục)
              - Crown icon hiển thị
              - Label VIP hiển thị
```

### TC-MAIN-003 — VIP badge animation khi VIP active 🤖
```
Precondition: [VIP] User đã kích hoạt VIP
Act:          Quan sát VIP badge pill
Assert:       - Pill vẫn có animation pulse (post-fix: không dừng khi VIP)
              - VIP label thay đổi theo trạng thái
              - Banner ad ẩn
```

### TC-MAIN-004 — Tap VIP badge → mở ActVipManagement 🔵
```
Precondition: ActMain đang hiển thị
Act:          Tap vào VIP pill/badge
Assert:       - ActVipManagement được launch
```

### TC-MAIN-005 — Bottom menu (bottom sheet) 🔵
```
Precondition: Có file đang mở
Act:          Mở bottom sheet menu
Assert:       - Các option phù hợp hiển thị
              - Mỗi option hoạt động đúng
```

### TC-MAIN-006 — onResume refresh UI 🔵
```
Precondition: User về ActMain từ ActVipManagement sau khi kích hoạt VIP
Act:          Press Back từ ActVipManagement
Assert:       - ActMain onResume() được gọi
              - VIP badge cập nhật trạng thái mới
              - Banner ad ẩn nếu VIP active
```

### TC-MAIN-007 — Toolbar menu items đúng trạng thái 🔵
```
Precondition: Không có file mở
Act:          Mở menu từ toolbar (3 dots / overflow)
Assert:       - "Open" luôn enabled
              - "Save" disabled khi chưa có file
              - "Save as" disabled khi chưa có file
              - "Undo" disabled khi không có thay đổi
              - "Redo" disabled khi không có thay đổi
              - "Search" disabled khi chưa có file
              - "Go to" disabled khi chưa có file
              - "Settings" luôn enabled
              - "About" luôn enabled
```

---

## 3. MỞ FILE — OPEN FLOW

**Launcher:** `LauncherOpen` | **Activity:** `ActMain`

### TC-OPEN-001 — Mở file binary (.bin) qua file picker 🔵
```
Precondition: ActMain, chưa mở file
Act:          Tap "Open File" → chọn file binary nhỏ (<1MB)
Assert:       - File được tải và hiển thị trong hex view
              - Title bar hiển thị tên file
              - Hex data hiển thị đúng (columns: offset | hex bytes | ASCII)
              - Không crash
```

### TC-OPEN-002 — Mở file text (.txt) qua file picker 🔵
```
Precondition: ActMain, chưa mở file
Act:          Tap "Open File" → chọn file .txt
Assert:       - File hiển thị ở cả hex tab và plain text tab
              - Nội dung text đúng
```

### TC-OPEN-003 — Mở file từ external intent (View action) 🔵
```
Precondition: File manager hoặc email app có file đính kèm
Act:          Tap "Open with" → chọn HexViewer
Assert:       - ActMain nhận intent URI
              - File được load và hiển thị
              - Không crash
```

### TC-OPEN-004 — Mở file rất lớn (>50MB) → kiểm tra memory 🔵
```
Precondition: Có file lớn trên thiết bị
Act:          Open file >50MB
Assert:       - App không crash với OOM
              - Progress indicator hiển thị khi load
              - Sau load: hex data hiển thị
              - Hoặc hiện dialog gợi ý dùng Partial Open
```

### TC-OPEN-005 — Mở file 0 bytes (empty file) 🔵
```
Precondition: Tạo file trống
Act:          Open empty file
Assert:       - Không crash
              - Hiển thị thông báo file rỗng hoặc hex view trống
```

### TC-OPEN-006 — Cancel file picker 🔵
```
Precondition: File picker đang mở
Act:          Nhấn Back / Cancel
Assert:       - Trở về ActMain
              - Không có file nào được load
              - UI không thay đổi
```

### TC-OPEN-007 — File không có quyền đọc 🔵
```
Precondition: File với quyền restricted
Act:          Cố gắng mở file
Assert:       - Hiển thị thông báo lỗi phù hợp
              - Không crash
```

---

## 4. HEX VIEWER

**Adapter:** `AdtHexTextArray`, `AdtSearchableListArray` | **Layout:** `v_listview_hex_row.xml`

### TC-HEXVIEW-001 — Hiển thị dữ liệu hex đúng format 🤖
```
Precondition: File binary đơn giản đã được mở
Act:          Quan sát hex list view
Assert:       - Mỗi row có: offset (hex) | hex bytes | ASCII representation
              - Offset bắt đầu từ 00000000
              - Bytes per line theo settings (default: 16)
              - Non-printable chars hiển thị là "."
```

### TC-HEXVIEW-002 — Số bytes per line = 8 🔵
```
Precondition: Settings → NBytesPerLine = 8
Act:          Open file
Assert:       - Mỗi row hiển thị 8 bytes hex
              - Offset increment = 8 mỗi dòng
```

### TC-HEXVIEW-003 — Scroll xuống cuối file 🔵
```
Precondition: File nhiều dòng (>100 dòng)
Act:          Scroll xuống cuối
Assert:       - Scroll mượt, không lag
              - Dòng cuối là eof của file
              - Không có dữ liệu giả
```

### TC-HEXVIEW-004 — Chuyển tab Hex ↔ Plain Text 🔵
```
Precondition: File text đang mở
Act:          Swipe left/right hoặc tap tab
Assert:       - Hex tab và Plain Text tab hoạt động
              - Nội dung đồng bộ nhau
              - Không crash khi chuyển tab
```

### TC-HEXVIEW-005 — Tap dòng hex → mở ActLineUpdate 🔵
```
Precondition: File đang mở trong hex view
Act:          Tap vào 1 dòng
Assert:       - ActLineUpdate được launch
              - Source hex array hiển thị đúng bytes của dòng đó
              - Tên file và vị trí dòng truyền đúng
```

---

## 5. HEX EDITOR — LINE UPDATE

**Activity:** `ActLineUpdate` | **Layout:** `act_line_update.xml`

### TC-LINEUPDATE-001 — Hiển thị source bytes của dòng 🤖
```
Precondition: Tap vào dòng có 16 bytes trong hex view
Act:          Quan sát ActLineUpdate
Assert:       - Source list view hiển thị 16 ô hex (1 byte mỗi ô)
              - Offset của dòng hiển thị đúng
              - Input field (TextInputEditText) cho phép nhập hex
```

### TC-LINEUPDATE-002 — Nhập hex hợp lệ → result cập nhật 🤖
```
Precondition: ActLineUpdate đang mở
Act:          Nhập "FF AA BB CC" vào input field
Assert:       - TextWatcher xử lý input
              - Result list view hiển thị bytes mới
              - Không có lỗi validation
```

### TC-LINEUPDATE-003 — Nhập hex không hợp lệ → báo lỗi 🤖
```
Precondition: ActLineUpdate đang mở
Act:          Nhập "ZZ GG" (không phải hex)
Assert:       - Error message hiển thị ở TextInputLayout
              - Nút OK/Save bị vô hiệu hóa
              - Không crash
```

### TC-LINEUPDATE-004 — Smart Input checkbox 🔵
```
Precondition: ActLineUpdate đang mở
Act:          Bật/tắt checkbox "Smart Input"
Assert:       - Smart Input ON: tự format input theo bytes
              - Smart Input OFF: nhập thô không format
```

### TC-LINEUPDATE-005 — Overwrite checkbox 🔵
```
Precondition: ActLineUpdate đang mở, đã có input
Act:          Bật Overwrite checkbox → save
Assert:       - Chế độ overwrite: chỉ ghi đè bytes hiện tại (không thêm)
              - File size không thay đổi sau save
```

### TC-LINEUPDATE-006 — Toggle visibility Source / Result panels 🤖
```
Precondition: ActLineUpdate đang mở
Act:          Tap vào label "Source" hoặc icon arrow để thu/mở panel
Assert:       - Animation expand/collapse hoạt động
              - Trạng thái lưu trong preferences (isLineEditSrcExpanded)
```

### TC-LINEUPDATE-007 — Save thay đổi → quay về ActMain 🔵
```
Precondition: Đã nhập hex mới hợp lệ
Act:          Tap confirm/save
Assert:       - ActLineUpdate kết thúc với result OK
              - ActMain nhận result
              - Hex view cập nhật với bytes mới
              - Undo action có thể undo thay đổi này
```

### TC-LINEUPDATE-008 — Sequential mode (nhiều dòng liên tiếp) 🔵
```
Precondition: File sequential đang mở
Act:          Tap dòng và vào ActLineUpdate sequential mode
Assert:       - Hiển thị ACTIVITY_EXTRA_SEQUENTIAL=true
              - NbLines > 1
              - ShiftOffset truyền đúng
```

### TC-LINEUPDATE-009 — EmojiCompat chưa init → không crash 🤖
```
Precondition: EmojiCompat chưa được init (Robolectric)
Act:          LineUpdateTextWatcher.normalizeForEmoji(charSequence)
Assert:       - Không ném IllegalStateException
              - Trả về cùng text gốc
```

### TC-LINEUPDATE-010 — Memory Monitor cảnh báo khi RAM thấp 🔵
```
Precondition: Device RAM gần cạn (hoặc simulate)
Act:          Mở ActLineUpdate với file lớn
Assert:       - MemoryMonitor phát hiện ngưỡng
              - Hiển thị cảnh báo cho user
              - Không crash OOM
```

---

## 6. PARTIAL OPEN (MỞ THEO BYTE RANGE)

**Activity:** `ActPartialOpen` | **Launcher:** `LauncherPartialOpen`

### TC-PARTIAL-001 — Mở Partial Open dialog 🔵
```
Precondition: File lớn đang được mở hoặc từ main menu
Act:          Chọn "Partial Open" từ menu
Assert:       - ActPartialOpen được launch
              - File size hiển thị đúng
              - Start offset field và End offset field hiển thị
```

### TC-PARTIAL-002 — Nhập offset hợp lệ (decimal) → mở đoạn file 🔵
```
Precondition: ActPartialOpen mở, file 10KB
Act:          Start = 0, End = 1024
Assert:       - Chỉ 1024 bytes đầu được load
              - Hex view hiển thị đúng bytes của đoạn đó
              - Offset trong hex view phản ánh start offset thực tế
```

### TC-PARTIAL-003 — Nhập offset hex → parse đúng 🔵
```
Precondition: ActPartialOpen, spinner = Hex mode
Act:          Nhập "0x100" đến "0x200"
Assert:       - Parse hex thành byte range 256-512
              - File load đúng đoạn
```

### TC-PARTIAL-004 — End offset > file size → báo lỗi 🔵
```
Precondition: ActPartialOpen, file 1KB
Act:          Nhập End = 999999
Assert:       - Validation fail
              - Error message "Cannot exceed file size"
              - OK button bị disabled
```

### TC-PARTIAL-005 — Start offset > End offset → báo lỗi 🔵
```
Precondition: ActPartialOpen
Act:          Nhập Start = 500, End = 100
Assert:       - Validation fail
              - Error message hiển thị
```

### TC-PARTIAL-006 — Sequential file mode 🔵
```
Precondition: File sequential (nhiều file ghép)
Act:          Mở Partial Open cho sequential file
Assert:       - Real size và logical size hiển thị đúng
              - Sequential flag được truyền qua intent
```

---

## 7. RECENTLY OPENED (MỞ GẦN ĐÂY)

**Activity:** `ActRecentlyOpen` | **Adapter:** `AdtRecentlyOpenRecycler`

### TC-RECENT-001 — Danh sách file gần đây hiển thị đúng 🔵
```
Precondition: Đã mở ít nhất 3 file trước đó
Act:          Tap "Recently Open" từ ActMain
Assert:       - Danh sách hiển thị theo thứ tự mới nhất → cũ nhất
              - Mỗi item hiển thị: tên file, path, size, số thứ tự
```

### TC-RECENT-002 — Danh sách rỗng khi chưa mở file nào 🔵
```
Precondition: App cài mới / xóa data
Act:          Mở ActRecentlyOpen
Assert:       - Hiển thị empty state (list trống)
              - Không crash
```

### TC-RECENT-003 — Tap file → mở file đó 🔵
```
Precondition: Có file trong danh sách gần đây
Act:          Tap vào một file trong danh sách
Assert:       - ActMain nhận result với start/end offset
              - File được reload và hiển thị
```

### TC-RECENT-004 — Swipe to delete item 🔵
```
Precondition: Có ít nhất 1 file trong danh sách
Act:          Swipe right/left trên 1 item
Assert:       - Item bị xóa khỏi danh sách
              - Danh sách refresh
              - File vẫn còn trên storage (chỉ xóa khỏi recent list)
```

### TC-RECENT-005 — Long press → Context menu 🔵
```
Precondition: Có file trong danh sách
Act:          Long press vào item
Assert:       - Hiện context menu (delete, open, etc.)
              - Các action hoạt động đúng
```

### TC-RECENT-006 — Select All → Delete All 🔵
```
Precondition: Có nhiều file trong danh sách
Act:          Tap "Select All" → xóa
Assert:       - Tất cả item được đánh dấu selected
              - Sau xóa: danh sách trống
```

---

## 8. HASH CALCULATOR

**Activity:** `ActHashCalculator` | **Layout:** `act_hash_calculator.xml`
**Task:** `TaskHash`

### TC-HASH-001 — Mở Hash Calculator 🔵
```
Precondition: ActMain
Act:          Tap "Hash Calculator" → sau khi interstitial (nếu có)
Assert:       - ActHashCalculator được launch
              - Chưa có file nào: hiển thị empty state / pick file card
              - Progress bar ẩn
```

### TC-HASH-002 — Pick file và tính hash 🔵
```
Precondition: ActHashCalculator đang mở
Act:          Tap card "Select File" → chọn file nhỏ (<1MB)
Assert:       - File name hiển thị
              - File size hiển thị (human readable: KB/MB)
              - Progress bar xuất hiện trong khi tính
              - Sau khi xong: danh sách hash hiển thị
                - MD5: 32 ký tự hex
                - SHA-1: 40 ký tự hex
                - SHA-256: 64 ký tự hex
                - SHA-512: 128 ký tự hex
```

### TC-HASH-003 — Copy hash value 🔵
```
Precondition: Hash đã được tính
Act:          Tap nút copy bên cạnh một hash value
Assert:       - Hash được copy vào clipboard
              - Toast/Snackbar thông báo "Copied"
```

### TC-HASH-004 — Compare hash với input 🔵
```
Precondition: Hash đã được tính, biết trước MD5 của file
Act:          Nhập MD5 đúng vào ô Compare
Assert:       - tvMatchResult hiển thị "MATCH" (màu xanh)
```

### TC-HASH-005 — Compare hash sai 🔵
```
Precondition: Hash đã được tính
Act:          Nhập hash sai vào ô Compare
Assert:       - tvMatchResult hiển thị "NO MATCH" (màu đỏ)
```

### TC-HASH-006 — Task bị cancel khi activity destroy 🤖
```
Precondition: TaskHash đang chạy (file lớn)
Act:          Xoay màn hình hoặc nhấn Back khi đang hash
Assert:       - TaskHash.cancel() được gọi
              - Không crash
              - Executor shutdown gracefully
```

### TC-HASH-007 — File rỗng (0 bytes) 🔵
```
Precondition: ActHashCalculator
Act:          Chọn file 0 bytes
Assert:       - Hiển thị thông báo lỗi hoặc hash của file rỗng (hợp lệ)
              - Không crash
```

### TC-HASH-008 — File rất lớn (>100MB) 🔵
```
Precondition: ActHashCalculator
Act:          Chọn file >100MB
Assert:       - Progress bar hiển thị
              - Hash tính xong (có thể mất vài giây)
              - Không OOM
```

---

## 9. FILE INFO (PHÁT HIỆN LOẠI FILE)

**Activity:** `ActFileInfo` | **Layout:** `act_file_info.xml`

### TC-FILEINFO-001 — Mở File Info 🔵
```
Precondition: ActMain
Act:          Tap "File Info" button → sau interstitial (nếu có)
Assert:       - ActFileInfo được launch
              - Giao diện hiển thị đúng (chưa có file)
```

### TC-FILEINFO-002 — Phát hiện PNG file 🔵
```
Precondition: ActFileInfo
Act:          Chọn file .png
Assert:       - File name, size hiển thị
              - Detected type: "PNG Image"
              - Magic bytes: "89 50 4E 47"
              - Icon PNG hiển thị
```

### TC-FILEINFO-003 — Phát hiện JPEG file 🔵
```
Act:    Chọn file .jpg
Assert: - Detected type: "JPEG Image"
        - Magic bytes: "FF D8 FF E0" hoặc variant
```

### TC-FILEINFO-004 — Phát hiện PDF file 🔵
```
Act:    Chọn file .pdf
Assert: - Detected type: "PDF Document"
        - Magic bytes: "25 50 44 46"
```

### TC-FILEINFO-005 — Phát hiện ZIP / APK / JAR 🔵
```
Act:    Chọn file .zip hoặc .apk
Assert: - Detected type: "ZIP / APK / JAR / Office Archive"
        - Magic bytes: "50 4B 03 04"
```

### TC-FILEINFO-006 — Phát hiện SQLite database 🔵
```
Act:    Chọn file .db (SQLite3)
Assert: - Detected type: "SQLite 3 Database"
        - Magic bytes bắt đầu với "53 51 4C 69 74 65"
```

### TC-FILEINFO-007 — Phát hiện Android DEX 🔵
```
Act:    Chọn file classes.dex
Assert: - Detected type: "Android DEX"
        - Magic bytes: "64 65 78 0A"
```

### TC-FILEINFO-008 — File không nhận ra → Unknown 🔵
```
Act:    Chọn file với extension lạ / magic bytes không có trong map
Assert: - Detected type: "Unknown" hoặc không phát hiện được
        - Không crash
```

### TC-FILEINFO-009 — Hủy executor khi destroy 🤖
```
Precondition: Đang tính toán file lớn
Act:          Xoay màn hình
Assert:       - Executor shutdownNow() được gọi
              - Không crash
```

---

## 10. SETTINGS (CÀI ĐẶT)

**Activity:** `ActSettings` | **Fragment:** `FrmSettings`

### TC-SETTINGS-001 — Mở Settings 🔵
```
Precondition: ActMain
Act:          Tap Settings từ toolbar menu
Assert:       - ActSettings được launch
              - Danh sách preferences hiển thị đủ
```

### TC-SETTINGS-002 — Đổi ngôn ngữ 🔵
```
Precondition: App đang dùng English (hoặc tiếng khác)
Act:          Settings → Language → chọn ngôn ngữ khác
Assert:       - App restart (finishAffinity + relaunch)
              - Toàn bộ UI được dịch sang ngôn ngữ mới
              - Settings lưu lại sau restart
```

### TC-SETTINGS-003 — Đổi ngôn ngữ khi có file đang mở 🔵
```
Precondition: File đang mở và có thay đổi (isChanged=true)
Act:          Settings → Language → chọn ngôn ngữ khác
Assert:       - Hiển thị thông báo lỗi "Unsaved changes exist"
              - Không đổi ngôn ngữ
              - User phải save/discard trước
```

### TC-SETTINGS-004 — Đổi Screen Orientation 🔵
```
Act:          Settings → Screen Orientation → Portrait / Landscape / Sensor
Assert:       - App lock orientation theo setting
              - Landscape setting: mSettingsListsLandscape enabled, Portrait disabled
              - Portrait setting: ngược lại
              - Sensor: cả hai enabled
```

### TC-SETTINGS-005 — Đổi Bytes Per Line 🔵
```
Act:          Settings → Bytes Per Line → chọn 8 hoặc 16
Assert:       - Hex view hiển thị đúng số bytes mỗi dòng
              - Offset tăng đúng
```

### TC-SETTINGS-006 — Memory Threshold setting 🔵
```
Act:          Settings → Memory Threshold → chọn giá trị
Assert:       - Threshold lưu vào preferences
              - MemoryMonitor sử dụng threshold mới
```

### TC-SETTINGS-007 — Restore Default 🔵
```
Precondition: Đã thay đổi nhiều settings
Act:          Settings → Restore Default → confirm
Assert:       - Tất cả settings reset về mặc định
              - Hex view cập nhật theo settings mặc định
```

### TC-SETTINGS-008 — Restore Default khi có file đang thay đổi 🔵
```
Precondition: File đang mở, có thay đổi chưa save
Act:          Settings → Restore Default
Assert:       - Hiển thị warning dialog
              - Không reset nếu user chưa save
```

### TC-SETTINGS-009 — Portrait List Settings 🔵
```
Act:          Settings → Portrait List Settings
Assert:       - ActSettingsListsPortraitAct launch
              - Font size, bytes per line cho portrait mode
```

### TC-SETTINGS-010 — Landscape List Settings 🔵
```
Act:          Settings → Landscape List Settings (chỉ enabled khi landscape mode)
Assert:       - ActSettingsListsLandscape launch
              - Font size, bytes per line cho landscape mode
```

### TC-SETTINGS-011 — License link 🔵
```
Act:          Settings → License
Assert:       - Browser mở link GitHub license
              - URL: https://github.com/royt93/4_HexViewer/blob/dev/license.txt
```

### TC-SETTINGS-012 — Version info 🔵
```
Act:          Quan sát Version preference
Assert:       - Hiển thị version name đúng (từ BuildConfig.VERSION_NAME)
```

---

## 11. VIP MANAGEMENT

**Activity:** `ActVipManagement` | **Layout:** `f_vip_management.xml`
**Prefs:** `VipPrefs` | **Keys:** `VipKeys`

### TC-VIP-001 — Màn hình VIP khi FREE user 🔵
```
Precondition: [FREE] User chưa có VIP
Act:          Mở ActVipManagement
Assert:       - Header background: bg_vip_status_header_free
              - tvStatusTitle: "Free User" (hoặc bản dịch)
              - Button "Watch Ad" visible và enabled
              - Button "Activate Key" visible
              - cardVipDetails hidden
              - Slide-in animation chạy khi mở
```

### TC-VIP-002 — Màn hình VIP khi VIP active 🔵
```
Precondition: [VIP] User đã có VIP còn hạn
Act:          Mở ActVipManagement
Assert:       - Header background: bg_vip_status_header_active
              - tvStatusTitle: "VIP Active"
              - Countdown timer đang chạy
              - cardVipDetails visible (thời gian kích hoạt, hết hạn)
              - btnWatchAd disabled
              - Konfetti animation chạy (celebration effect)
```

### TC-VIP-003 — Kích hoạt VIP bằng key hợp lệ [ONLINE] 🔵
```
Precondition: [FREE, ONLINE] Có VIP key hợp lệ
Act:          Nhập key vào etVipKey → tap btnActivateKey
Assert:       - Loading dialog hiển thị 1s
              - activateVipByKey() thành công
              - Dialog thành công với số ngày VIP
              - VipPrefs.saveGrantedAtMs() được gọi
              - vipPrefs.markUserRedeemed() được gọi
              - UI refresh: header VIP active
              - Konfetti celebration effect
              - Haptic feedback (vibrate)
```

### TC-VIP-004 — Kích hoạt VIP bằng key sai 🔵
```
Precondition: [FREE] Có key sai
Act:          Nhập key sai → tap btnActivateKey
Assert:       - Dialog "Activation Failed"
              - UI vẫn ở trạng thái FREE
```

### TC-VIP-005 — Kích hoạt VIP bằng key [OFFLINE] 🔵
```
Precondition: [FREE, OFFLINE]
Act:          Nhập key → tap Activate
Assert:       - key validation vẫn hoạt động (local check)
              - Kết quả success/fail tùy theo key
```

### TC-VIP-006 — Watch Ad để nhận VIP 3 ngày [ONLINE] 🔵
```
Precondition: [FREE, ONLINE] Rewarded ad available
Act:          Tap "Watch Ad" → xem hết ad
Assert:       - grantVipFromAd() được gọi
              - VIP 3 ngày được cấp
              - UI cập nhật sang VIP state
```

### TC-VIP-007 — Watch Ad → ad không load [OFFLINE] 🔵
```
Precondition: [FREE, OFFLINE]
Act:          Tap "Watch Ad"
Assert:       - isNetworkAvailable() == false
              - showNoAdDialog() được gọi ngay lập tức
              - Dialog "No Ad Available" hiển thị
              - VIP KHÔNG được cấp
```

### TC-VIP-008 — Watch Ad → Rewarded fail → fallback Interstitial 🔵
```
Precondition: [ONLINE] Rewarded ad không fill, Interstitial fill
Act:          Tap "Watch Ad" → earned=false
Assert:       - showInterstitial() được gọi
              - Nếu Interstitial hiện: grantVipFromAd()
              - Nếu cả hai fail: showNoAdDialog()
```

### TC-VIP-009 — Revoke VIP 🔵
```
Precondition: [VIP] User đang có VIP
Act:          Tap "Revoke VIP" → confirm
Assert:       - AdManager.clearVipByKey() được gọi
              - vipPrefs.clearGrantedAtMs() được gọi
              - UI reset sang FREE state
              - Toast "VIP Revoked"
```

### TC-VIP-010 — Countdown timer VIP 🔵
```
Precondition: [VIP] 30 phút còn lại
Act:          Quan sát tvCountdown
Assert:       - Countdown đếm ngược real-time
              - Format: "XD HH:MM:SS" hoặc tương tự
              - Khi hết hạn: UI chuyển sang FREE state
```

### TC-VIP-011 — VIP first install grace period 🔵
```
Precondition: App cài lần đầu, AppPreferences.isAddVIPMemberFirstInitSuccess()==true
Act:          Mở ActVipManagement
Assert:       - tvActiveVipLabel: "First Install VIP" (hoặc bản dịch)
              - Không phải "Redeemed X days"
```

### TC-VIP-012 — Pulse animation nút Watch Ad 🤖
```
Precondition: [FREE] ActVipManagement mở
Act:          Quan sát btnWatchAd
Assert:       - scaleX và scaleY pulse 1.0 → 1.04 → 1.0 liên tục
              - Duration: 1200ms per cycle
```

### TC-VIP-013 — Crown shimmer animation 🤖
```
Precondition: ActVipManagement mở
Act:          Quan sát imgCrown
Assert:       - Rotation -8f ↔ +8f liên tục
              - Duration: 2000ms per cycle
```

### TC-VIP-014 — Privacy Policy link 🔵
```
Act:          Tap tvPrivacyPolicy
Assert:       - Browser mở đúng URL Privacy Policy
```

---

## 12. GOTO DIALOG

**Class:** `GoToDialog` | **Layout:** `dlg_content_dialog_go_to.xml`

### TC-GOTO-001 — Mở GoTo từ menu (Address mode) 🔵
```
Precondition: File đang mở trong hex view
Act:          Menu → "Go To Address"
Assert:       - Dialog mở với title "Go To Address (Hexadecimal)"
              - Input field cho hex address
              - Keyboard tự động mở
```

### TC-GOTO-002 — GoTo địa chỉ hex hợp lệ 🔵
```
Precondition: GoTo dialog mở, file 256 bytes
Act:          Nhập "10" (hex = byte 16) → OK
Assert:       - List view scroll tới dòng chứa byte 0x10
              - Dòng đó blink (animate background)
              - Dialog đóng
```

### TC-GOTO-003 — GoTo địa chỉ vượt quá file size 🔵
```
Precondition: GoTo dialog, file 100 bytes
Act:          Nhập "FFFF" (hex > file size) → OK
Assert:       - Error: "Cannot exceed 0x63" (hoặc tương tự)
              - Dialog không đóng
              - Shake animation ở input field
```

### TC-GOTO-004 — GoTo địa chỉ không phải hex 🔵
```
Act:    Nhập "XYZ" → OK
Assert: - Shake animation
        - Error tô đỏ TextInputLayout
```

### TC-GOTO-005 — GoTo Line (Hex mode) 🔵
```
Precondition: Hex view đang active
Act:          Menu → "Go To Line" → nhập số dòng decimal
Assert:       - Scroll tới đúng dòng đó
              - Blink animation
```

### TC-GOTO-006 — GoTo Line (Plain Text mode) 🔵
```
Precondition: Plain Text tab đang active
Act:          Menu → "Go To Line" → nhập số dòng
Assert:       - Plain text list view scroll tới đúng dòng
```

### TC-GOTO-007 — GoTo nhớ giá trị trước 🔵
```
Precondition: Đã GoTo "10" trước đó
Act:          Mở GoTo dialog lần 2
Assert:       - Input field pre-fill với "10" (giá trị cũ)
```

---

## 13. SAVE FILE

**Dialog:** `SaveDialog` | **Launcher:** `LauncherSave`

### TC-SAVE-001 — Save sau khi edit 🔵
```
Precondition: File đang mở, đã thay đổi ít nhất 1 byte
Act:          Menu → Save
Assert:       - SaveDialog hiển thị
              - Option "Save" (overwrite) và "Save As" (file mới)
```

### TC-SAVE-002 — Save As → chọn destination 🔵
```
Act:    Save As → chọn folder và tên file → confirm
Assert: - File mới được tạo với nội dung đã sửa
        - File gốc không thay đổi
        - ActMain title cập nhật với tên file mới
```

### TC-SAVE-003 — Save (overwrite) 🔵
```
Act:    Save (không phải Save As)
Assert: - File gốc được ghi đè với nội dung mới
        - Undo stack bị clear sau khi save
        - isChanged() == false sau save
```

### TC-SAVE-004 — Cancel Save dialog 🔵
```
Act:    Mở Save dialog → Cancel/Back
Assert: - Dialog đóng
        - File không được lưu
        - isChanged() vẫn == true
```

### TC-SAVE-005 — Save khi không có quyền ghi 🔵
```
Precondition: File ở vị trí không có write permission
Act:          Save
Assert:       - Error dialog hiển thị
              - Không crash
```

---

## 14. SEARCH (TÌM KIẾM)

**Adapter:** `AdtSearchableListArray` | **Filter:** `EntryFilter`

### TC-SEARCH-001 — Mở Search từ menu 🔵
```
Precondition: File đang mở
Act:          Menu → Search (hoặc Android SearchView)
Assert:       - Search bar hiển thị trong toolbar
              - Keyboard mở
```

### TC-SEARCH-002 — Tìm kiếm chuỗi hex hợp lệ 🔵
```
Act:    Nhập "FF 00" vào search bar
Assert: - List filter hiển thị các dòng chứa pattern đó
        - Highlight các byte match
```

### TC-SEARCH-003 — Tìm kiếm text (plain text tab) 🔵
```
Precondition: Plain text tab active
Act:          Tìm "Hello"
Assert:       - Filter danh sách theo text match
              - Kết quả đúng
```

### TC-SEARCH-004 — Tìm không có kết quả 🔵
```
Act:    Nhập chuỗi không tồn tại trong file
Assert: - List trống hoặc empty state
        - Không crash
```

### TC-SEARCH-005 — Clear search 🔵
```
Precondition: Đang filter kết quả
Act:          Xóa text search / tap X
Assert:       - Toàn bộ list hiển thị lại
              - Scroll về vị trí ban đầu hoặc top
```

---

## 15. UNDO / REDO

**Class:** `UnDoRedo` | **Commands:** `DeleteCommand`, các command khác

### TC-UNDO-001 — Undo sau khi edit 1 dòng 🔵
```
Precondition: Đã sửa 1 dòng hex
Act:          Menu → Undo
Assert:       - Dòng đó trở về bytes gốc
              - isChanged() == false nếu chỉ 1 thay đổi
              - Undo menu item bị disable sau khi undo hết
```

### TC-UNDO-002 — Undo nhiều lần 🔵
```
Precondition: Đã sửa 5 dòng khác nhau
Act:          Undo × 5
Assert:       - Mỗi lần undo hoàn tác đúng thay đổi tương ứng
              - Thứ tự: LIFO (Last In First Out)
```

### TC-UNDO-003 — Redo sau Undo 🔵
```
Precondition: Đã Undo 1 thay đổi
Act:          Redo
Assert:       - Thay đổi được áp dụng lại
              - Redo disable sau khi redo hết stack
```

### TC-UNDO-004 — Undo stack clear sau Save 🔵
```
Precondition: Có 3 thay đổi, save xong
Act:          Save → kiểm tra Undo menu item
Assert:       - Undo disable (stack đã clear)
              - isChanged() == false
```

### TC-UNDO-005 — Undo không có gì → nút disable 🤖
```
Precondition: File mới mở, chưa sửa
Act:          Kiểm tra menu
Assert:       - Undo disabled
              - Redo disabled
```

---

## 16. MULTI-SELECT (CHỌN NHIỀU DÒNG)

**Callback:** `GenericMultiChoiceCallback`, `HexMultiChoiceCallback`

### TC-MULTI-001 — Long press → vào chế độ multi-select 🔵
```
Precondition: Hex view đang hiển thị file
Act:          Long press vào 1 dòng
Assert:       - Action mode (contextual action bar) kích hoạt
              - Dòng đó được select (highlighted)
              - "1 item selected" hiển thị
```

### TC-MULTI-002 — Select thêm nhiều dòng 🔵
```
Precondition: Đang ở multi-select mode
Act:          Tap thêm 4 dòng
Assert:       - 5 dòng được select
              - "5 items selected" hiển thị
```

### TC-MULTI-003 — Select All 🔵
```
Act:    Tap "Select All" trong action mode
Assert: - Toàn bộ dòng được select
        - Count hiển thị đúng
```

### TC-MULTI-004 — Delete selected rows 🔵
```
Precondition: 3 dòng đang được select
Act:          Tap "Delete" trong action mode
Assert:       - 3 dòng bị xóa khỏi file data
              - List refresh
              - isChanged() == true
              - Action mode đóng
```

### TC-MULTI-005 — Exit multi-select mode 🔵
```
Act:    Nhấn Back hoặc tap X trong action mode
Assert: - Action mode đóng
        - Tất cả selection được xóa
        - List trở về bình thường
```

---

## 17. ABOUT SCREEN

**Activity:** `AboutActivity` | **Layout:** `activity_about.xml`

### TC-ABOUT-001 — Mở About Screen 🔵
```
Act:    Menu → About
Assert: - AboutActivity launch
        - App name, version hiển thị
        - License thông tin
        - Author info
```

### TC-ABOUT-002 — Các link clickable 🔵
```
Act:    Tap các link trong About
Assert: - Browser mở đúng URL
        - Không crash
```

---

## 18. LIFECYCLE & MEMORY

### TC-LIFECYCLE-001 — Xoay màn hình khi đang xem file 🔵
```
Precondition: File đang hiển thị trong hex view, scroll giữa
Act:          Xoay device 90°
Assert:       - File vẫn hiển thị (không mất data)
              - Scroll position được restore (hoặc về top)
              - Không crash
              - Orientation lock theo setting
```

### TC-LIFECYCLE-002 — Vào background → trở lại 🔵
```
Precondition: File đang mở với một số thay đổi
Act:          Nhấn Home → đợi 1-2 phút → mở lại app
Assert:       - File vẫn hiển thị
              - Thay đổi chưa save vẫn còn
              - isChanged() == true
```

### TC-LIFECYCLE-003 — Hệ thống kill process → mở lại 🔵
```
Precondition: App bị kill bởi OS (low memory)
Act:          Mở lại app
Assert:       - App khởi động lại từ SplashActivity
              - Không crash với saved instance state
```

### TC-LIFECYCLE-004 — Không OOM khi mở file nhiều lần 🔵
```
Act:    Open file → back → open file khác × 10
Assert: - Memory không tăng vô hạn
        - Không OOM crash
        - GC hoạt động bình thường
```

### TC-LIFECYCLE-005 — Memory Monitor ngưỡng cảnh báo 🔵
```
Precondition: Memory threshold = 80%, RAM thực sự gần đầy
Act:          Mở file lớn
Assert:       - MemoryMonitor trigger
              - Cảnh báo hoặc auto-close file
```

---

## 19. PERMISSIONS

### TC-PERM-001 — Android 12 trở xuống: yêu cầu READ_EXTERNAL_STORAGE 🔵
```
Precondition: Android API ≤ 32, permission chưa cấp
Act:          Tap "Open File"
Assert:       - Permission dialog hiển thị
              - Sau khi cấp: file picker mở
```

### TC-PERM-002 — Android 13+: không yêu cầu storage permission 🔵
```
Precondition: Android API ≥ 33
Act:          Tap "Open File"
Assert:       - Không có permission dialog
              - SAF file picker mở trực tiếp
```

### TC-PERM-003 — POST_NOTIFICATIONS (Android 13+) 🔵
```
Precondition: Android API ≥ 33, notification permission chưa cấp
Act:          Lần đầu launch app
Assert:       - Permission dialog cho notification
              - Từ chối: app vẫn hoạt động bình thường
```

### TC-PERM-004 — AD_ID permission 🔵
```
Precondition: Android 13+
Act:          Launch app
Assert:       - Ad ID được sử dụng đúng
              - Từ chối: ad vẫn hiện (non-personalized)
```

---

## 20. ORIENTATION & CONFIGURATION CHANGES

### TC-ORIENT-001 — Portrait mode: layout hexview dọc 🔵
```
Act:    Xoay device portrait
Assert: - Hex view full width
        - Font size theo portrait settings
        - Bytes per line theo portrait config
```

### TC-ORIENT-002 — Landscape mode: layout hexview ngang 🔵
```
Act:    Xoay device landscape
Assert: - Hex view có thể hiển thị nhiều bytes hơn
        - Font size theo landscape settings
        - Keyboard không che hex view (edge-to-edge)
```

### TC-ORIENT-003 — Locale change (Language setting) → rebuild UI 🔵
```
Act:    Đổi ngôn ngữ trong Settings
Assert: - Tất cả string trong app đổi ngôn ngữ
        - Datetime format đổi theo locale
        - RTL layout nếu Arabic/Hebrew
```

### TC-ORIENT-004 — Font scale (Accessibility) không làm vỡ layout 🔵
```
Precondition: Device accessibility → font size = Large
Act:          Mở app
Assert:       - Text không bị crop
              - Layout không bị overlap
              - Hex data vẫn đọc được
```

---

## PHỤ LỤC A — TEST FILE CHUẨN BỊ

| File | Mô tả | Dùng cho |
|------|--------|---------|
| `test_1kb.bin` | 1024 bytes random binary | TC-OPEN-001, TC-HEXVIEW-*, TC-GOTO-* |
| `test_empty.bin` | 0 bytes | TC-OPEN-005, TC-HASH-007 |
| `test_10mb.bin` | 10MB binary | TC-OPEN-004, TC-PARTIAL-* |
| `test_100mb.bin` | 100MB binary | TC-HASH-008 |
| `test_text.txt` | UTF-8 text file | TC-OPEN-002, TC-HEXVIEW-004 |
| `test_png.png` | Valid PNG | TC-FILEINFO-002 |
| `test_jpg.jpg` | Valid JPEG | TC-FILEINFO-003 |
| `test_pdf.pdf` | Valid PDF | TC-FILEINFO-004 |
| `test_archive.zip` | Valid ZIP | TC-FILEINFO-005 |
| `test_database.db` | SQLite3 DB | TC-FILEINFO-006 |
| `test_classes.dex` | Android DEX | TC-FILEINFO-007 |
| `test_unknown.xyz` | Unknown format | TC-FILEINFO-008 |

---

## PHỤ LỤC B — AUTOMATION SETUP

### Framework khuyến nghị
```
- Unit Tests: JUnit4 + Robolectric
- UI/Integration Tests: Espresso + UI Automator
- CI: GitHub Actions / Bitrise
```

### Gradle commands
```bash
# Unit tests
./gradlew test

# Instrumented tests (yêu cầu connected device/emulator)
./gradlew connectedAndroidTest

# Specific test class
./gradlew testDebugUnitTest --tests "*.ActHashCalculatorTest"

# Coverage report
./gradlew testDebugUnitTest jacocoTestReport
```

### Test data setup (Espresso)
```kotlin
// Inject test file via AssetManager
val inputStream = InstrumentationRegistry.getInstrumentation()
    .context.assets.open("test_1kb.bin")
// Copy to device storage before test
```

---

## PHỤ LỤC C — PRIORITY MATRIX

| Priority | Test Cases | Lý do |
|----------|-----------|-------|
| **P0 - Critical** | TC-SPLASH-001/002/003, TC-OPEN-001/002/003, TC-HEXVIEW-001/004/005, TC-LINEUPDATE-001/002/003/007, TC-VIP-001/002/003/005/007/009 | Core user flow, crash scenarios |
| **P1 - High** | TC-HASH-001..006, TC-FILEINFO-001..007, TC-GOTO-001..005, TC-SAVE-001..003, TC-SETTINGS-001..005 | Key features |
| **P2 - Medium** | TC-PARTIAL-001..005, TC-RECENT-001..005, TC-UNDO-001..004, TC-MULTI-001..004, TC-PERM-001..003 | Secondary features |
| **P3 - Low** | TC-ABOUT-001/002, TC-ORIENT-001..004, TC-LIFECYCLE-001..003, TC-SEARCH-001..005 | Edge cases, UI polish |

---

*Tổng số test cases: ~140 TCs | Nguồn: scan source code ngày 2026-06-21*

---

## PHỤ LỤC D — KẾT QUẢ VERIFY THỰC TẾ TRÊN SAMSUNG S24 ULTRA

> **Device:** Samsung Galaxy S24 Ultra (SM-S928B) | **Android:** 16 (API 36)
> **Ngày verify:** 2026-06-21 | **Build:** dev-debug

### Automated Tests (Instrumented — 35 TCs)

| Test Suite | Tests | Pass | Fail | Thời gian |
|-----------|-------|------|------|-----------|
| ActVipManagementInstrumentationTest | 2 | 2 | 0 | 3.5s |
| LineEntriesMemoryIntegrationTest | 5 | 5 | 0 | 0.4s |
| ActMainFileValidationWidgetTest | 1 | 1 | 0 | 4.1s |
| ActMainOomFixWidgetTest | 4 | 4 | 0 | 9.0s |
| RealFileOomIntegrationTest | 8 | 8 | 0 | 41.4s |
| ActHashCalculatorTest | 2 | 2 | 0 | 5.6s |
| ActLineUpdateInstrumentationTest | 1 | 1 | 0 | 46.9s |
| ActMainPillAnimationInstrumentationTest | 4 | 4 | 0 | 0.0s |
| WebViewOomFixIntegrationTest | 8 | 8 | 0 | 0.0s |
| **TỔNG** | **35** | **35** | **0** | **2m 23s** |

**Kết quả: BUILD SUCCESSFUL — 35/35 PASS ✅**

### Manual Spot-check Results

| TC | Kết quả | Ghi chú |
|----|---------|---------|
| TC-SPLASH-002 | ✅ PASS | isTaskRoot() skip splash khi app đang chạy |
| TC-MAIN-001 | ✅ PASS | 5 button hiển thị đúng, tiêu đề "Hex Viewer" |
| TC-MAIN-002 | ✅ PASS | VIP badge "NHẬN VIP" hiển thị ở toolbar [FREE] |
| TC-MAIN-004 | ✅ PASS | Tap VIP badge → ActVipManagement mở |
| TC-VIP-001 | ✅ PASS | FREE state: "Người dùng miễn phí", btn Watch Ad enabled |
| TC-VIP-007 | 🔵 MANUAL | "Xem quảng cáo → 3 ngày VIP" button visible |
| Banner Ad | ✅ PASS | AppLovin MAX test banner hiển thị ở bottom main screen |
| Interstitial Ad | ✅ PASS | Xuất hiện khi tap Hash Calculator, có countdown 9s |
| TC-HASH-001 | ✅ PASS | Hash Calculator: empty state "Chạm để chọn tệp cần kiểm tra" |
| TC-HASH-002 | ✅ PASS | MD5/SHA-1/SHA-256/SHA-512 tính đúng với test_1kb.bin |
| TC-HASH-004 | 🔵 MANUAL | Ô compare "Dán mã hash vào để đối chiếu" hiển thị |

### Phát hiện bổ sung (cần cập nhật test plan)

| # | Phát hiện | Ảnh hưởng TC |
|---|-----------|-------------|
| 1 | Interstitial có **countdown timer 9 giây** (không close ngay) — test automation cần `waitFor` 10s | TC-HASH-001, TC-FILEINFO-001 |
| 2 | Sau khi close interstitial bằng nút X, **interstitial thứ 2** có thể hiện khi chọn file trong SAF picker | TC-HASH-002 — cần mock ad trong automation |
| 3 | Nút X close ad ở bounds `[28,112][132,216]` (device pixels) — không phải góc trên phải mà là **góc trên trái** | Tất cả TC có interstitial |
| 4 | App ngôn ngữ **Tiếng Việt** trên device này (Settings → Language = vi) | TC-SETTINGS-002 cần test đổi về English |
| 5 | File picker SAF mở **"Tệp đã tải xuống"** (Downloads) ở UIAutomator bounds `[181,935][765,988]` | TC-HASH-002, TC-FILEINFO-* |
