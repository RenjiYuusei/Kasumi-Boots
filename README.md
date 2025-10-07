# KasumiBoots (Kotlin + libsu)

Ứng dụng Android 1-click Boost hiệu năng CPU/GPU/RAM dành cho thiết bị đã root, tích hợp `Topjohnwu/libsu` để thực thi lệnh đặc quyền. Tuân thủ chính sách CI-only: build APK chỉ qua GitHub Actions.

## Tính năng
- 1-click Boost: chạy script tối ưu trong `res/raw/boost` (mở tất cả CPU core, governor performance, GPU perf nếu KGSL, drop caches, kill cached processes).
- Tự preload root shell (libsu) để thao tác nhanh, UI đơn giản (1 nút).

## Cấu trúc chính
- Mã nguồn app: `app/`
- Script boost: `app/src/main/res/raw/boost`
- Entry UI: `app/src/main/java/com/cloudphone/tool/ui/MainActivity.kt`
- Khởi tạo libsu: `app/src/main/java/com/cloudphone/tool/App.kt`
- Cấu hình Gradle/AGP: `settings.gradle`, `build.gradle`, `app/build.gradle`
- CI workflow: `.github/workflows/build-release.yml`

## Build theo CI-only (không build cục bộ)
1. Commit & push vào `main/master/develop` để tự động kích hoạt workflow.
2. Hoặc vào GitHub > Actions > chọn workflow "Build Release APK" > Run workflow.
3. Sau khi build xong, APK nằm ở mục Artifacts: `KasumiBoots-<timestamp>` (bên trong chứa `KasumiBoots-<sha>.apk`).

Lưu ý: `keystore.properties` + file keystore (`ci/keystore/Yuusei.jks`) cần tồn tại trong repo hoặc cấu hình qua GitHub Secrets (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD) theo workflow.

## Yêu cầu
- Thiết bị đã root (Magisk) và cấp quyền root cho app.
- `libsu` phiên bản `6.0.0` từ JitPack.

## Tùy biến script boost
Nội dung `res/raw/boost` là shell script an toàn theo kiểu best-effort. Có thể chỉnh sửa cho từng thiết bị:
- CPU: governor, max freq.
- GPU: KGSL devfreq.
- Dọn bộ nhớ: drop_caches, kill cached apps.

Cảnh báo: Tác động hiệu năng có thể tiêu hao pin, tăng nhiệt. Dùng khi cần thiết.
