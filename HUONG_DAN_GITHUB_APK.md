# HƯỚNG DẪN TẠO APK TRÊN GITHUB

## Cách 1: Tải ZIP lên GitHub qua trình duyệt

1. Vào https://github.com/new và tạo repository mới, ví dụ `quang-thien-car-rgb`. Chọn **Private** nếu muốn giữ mã nguồn riêng. Có thể tạo repo rỗng (không tích chọn tạo sẵn README).
2. Giải nén file `QuangThienCarVisualizer_GitHub_APK_v0.1.zip` trên máy tính.
3. Trong repository, chọn **Add file → Upload files** (hoặc liên kết upload trên trang repository rỗng). Upload **toàn bộ nội dung bên trong** thư mục `QuangThienCarVisualizer`, không upload file ZIP làm một tệp duy nhất. Quan trọng: thư mục `.github/workflows` có thể bị ẩn trên Windows; cần bảo đảm file `.github/workflows/build-apk.yml` được đưa lên đúng cấu trúc. Nếu giao diện web không giữ thư mục ẩn, dùng cách 2 hoặc tạo thủ công workflow trong tab Actions.
4. Bấm **Commit changes** lên nhánh `main`.
5. Mở tab **Actions**, chọn workflow **Build QUANG THIEN Android APK**. Workflow có thể tự chạy khi commit. Nếu chưa chạy, chọn **Run workflow**.
6. Khi trạng thái hoàn tất thành công (dấu tích xanh), mở lần chạy đó, kéo tới phần **Artifacts**, tải `QuangThienCarVisualizer-debug-APK`.
7. Giải nén artifact để lấy file `app-debug.apk`, chép sang màn hình Android ô tô để cài đặt.

## Cách 2: Upload bằng Git từ máy tính (đáng tin cậy hơn cho thư mục ẩn)

Mở terminal trong thư mục vừa giải nén `QuangThienCarVisualizer`:

```bash
git init
git add .
git commit -m "Initial Android app with GitHub APK build"
git branch -M main
git remote add origin https://github.com/USERNAME/REPO.git
git push -u origin main
```

Thay USERNAME/REPO bằng tài khoản và tên repository của bạn. Cần đăng nhập GitHub trên máy.

## Lưu ý

- Workflow dùng **Java 17**, **Gradle 8.9**, **Android SDK 35** và build `:app:assembleDebug`.
- **Không cần tạo Gradle Wrapper** để chạy GitHub Actions vì workflow tự thiết lập Gradle 8.9.
- APK là bản **Debug** dùng để cài và kiểm thử nội bộ, không phải bản Release ký chính thức dành cho Play Store.
- Phiên bản Android tối thiểu là **Android 8.0 (API 26)**.
- File source chưa được build/test trong môi trường tạo file này; nếu Actions báo lỗi, mở job `Build Debug APK`, sao chép phần log màu đỏ để tiếp tục sửa.
- Quyền ghi âm, hiển thị nổi và khả năng bắt nhạc thực tế vẫn phụ thuộc đầu Android ô tô.
