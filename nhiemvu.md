# Nhiệm Vụ & Quy Tắc Dự Án (DoAnMonHoc)

Tài liệu này lưu trữ các ngữ cảnh và quy tắc quan trọng của dự án để AI (Assistant) đọc và nạp vào bộ nhớ ngữ cảnh trong các phiên làm việc tiếp theo.

## 1. Ngữ Cảnh Dự Án (Project Context)
- **Backend Framework**: Java Spring Boot 4.x (Quản lý build bằng Gradle, dùng Java 25).
- **Database**: MySQL (tương tác thông qua Spring Data JPA).
- **Authentication**: Xác thực JWT (JSON Web Tokens) + Spring Security.
- **Kiến trúc (Pattern)**: Controller -> Service -> Repository.
- **Hệ sinh thái liên quan**: Backend lập ra làm REST API cung cấp cho ứng dụng điểm danh (Flutter App - có tính năng quét NFC và nhận diện khuôn mặt).

## 2. Quy Tắc Lập Trình Nhất Quán (Coding Rules)
Nhằm đảm bảo dự án hoạt động ổn định, AI phải tuân thủ nghiêm ngặt các quy tắc sau:
- **Kiến trúc tầng**: Các class `Controller` phải cực kỳ mỏng (thin controller). Mọi logic tính toán, xử lý nghiệp vụ (business logic) bắt buộc phải nằm ở tầng `Service`.
- **Database Schema**: **Tuyệt đối KHÔNG ĐƯỢC** tự ý thay đổi cấu trúc bảng, cấu trúc cơ sở dữ liệu trừ khi được yêu cầu rõ ràng.
- **Files/Folders**: **Tuyệt đối KHÔNG ĐƯỢC** đổi tên các file hoặc thư mục hiện có của dự án.
- **Validation & Response**: Luôn thực hiện *validate input* dữ liệu đầu vào của các API. Phải luôn trả về các **HTTP status codes chuẩn và chính xác** (200, 201, 400, 401, 403, 404, 500...).
-Tôi có 1 model nhận diện khuôn mặt ở E:\Baitaptrentruong\Java\FaceRecognition_With_FaceNet_Android
-Tôi có 1 frontend flutter ở
E:\Baitaptrentruong\Java\FaceRecognition_With_FaceNet_Android\do_an_java
Hãy viết sao cho backend có thể giao tiếp với frontend và model nhận diện khuôn mặt
Hoặc viết flutter sao để kết nối với backend và model nhận diện khuôn mặt


## 3. Quy Trình Làm Việc (Workflow Rules)
- Luôn phải tóm tắt hoặc đưa ra **kế hoạch (plan)** rõ ràng trước khi bắt tay vào sinh code.
- Hiển thị chi tiết sự thay đổi của các đoạn code (show diffs) trước khi áp dụng chỉnh sửa.
- Giải thích rõ ràng nguyên nhân/lý do cho mọi thao tác hoặc thay đổi phần thiết kế lớn.

## 4. Quy Tắc An Toàn (Safety Rules)
- Không bao giờ chạy các lệnh mang tính chất phá hoại hoặc xóa bỏ (VD: `rm -rf`, format, drop database).
- Không được thao tác trên/thay đổi môi trường production (môi trường vận hành thực tế).