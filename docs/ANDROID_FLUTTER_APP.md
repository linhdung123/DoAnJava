# Ứng dụng Android / Flutter (NFC + FaceNet)

## Đường dẫn project

| Mô tả | Đường dẫn |
|--------|------------|
| **Flutter app** (chấm công NFC + nhận diện khuôn mặt) | `E:\Baitaptrentruong\Java\FaceRecognition_With_FaceNet_Android\do_an_java` |
| **Backend Spring Boot** (API) | `E:\Baitaptrentruong\Java\DoAnMonHoc` |

## URL backend trên Android

- **Emulator**: `http://10.0.2.2:8080` → trỏ về `localhost:8080` trên máy PC chạy Java.
- **Điện thoại thật** (cùng Wi‑Fi với PC): `http://<IP-LAN-PC>:8080` (ví dụ `http://192.168.1.50:8080`).

Cấu hình trong Flutter: `do_an_java/lib/config/backend_config.dart`  
(`useAndroidEmulator` + `physicalDeviceLanUrl`).

## API backend (mặc định)

- Context: `http://<host>:8080`
- REST: `/api/...` (đăng nhập: `POST /api/auth/login`, chấm công: `/api/attendance/...`)

## Ghi chú

- `AndroidManifest.xml` của app Flutter đã bật `usesCleartextTraffic` cho HTTP (debug).
- Production nên dùng HTTPS.
