> **GitHub Actions:** Xem [`HUONG_DAN_GITHUB_APK.md`](HUONG_DAN_GITHUB_APK.md). Repo đã có workflow `.github/workflows/build-apk.yml` để tự build Debug APK trên GitHub, không cần Gradle Wrapper.

# QUANG THIỆN – CAR RGB VISUALIZER (Android Studio source)

## Build
1. Mở thư mục `QuangThienCarVisualizer` bằng Android Studio (JDK 17).
2. Cần Gradle 8.9 (dự án chưa kèm `gradlew`/wrapper) và JDK 17. Trong Android Studio, chọn phiên bản Gradle đã cài hoặc tạo Gradle wrapper 8.9 trước khi Sync. Android Studio sẽ tải AGP 8.7.3, Kotlin 2.0.21 và Android SDK 35.
3. Sync Gradle thành công, sau đó chọn **Build > Build APK(s)** và cài APK lên đầu Android ô tô.
4. Mở app, cấp quyền **Record audio**, sau đó thử **BẮT ÂM THANH THẬT**.
5. Chọn **BẬT NỔI** và cấp quyền **Display over other apps**. App sẽ tạo thanh LED dưới màn hình và bong bóng QT kéo được. Kích đúp QT thử mở/đóng chế độ đồng hồ.

## Tính năng bản 0.1
- Giao diện đồng hồ thể thao 4 màu, chạy kim theo giờ hệ thống.
- Hai ngựa RGB tĩnh trích từ bản minh họa đã duyệt (`horse_left.png` / `horse_right.png`).
- Chữ QUANG THIỆN có 5 họ font Android cơ bản (thiết bị thiếu font sẽ fallback).
- 12 hiệu ứng LED, 7 bảng màu, tùy chỉnh chiều cao, độ rộng, độ sáng, glow, số cột, tốc độ, độ nhạy BASS/MID/TREBLE.
- Real audio: Android `Visualizer(0)` FFT với `RECORD_AUDIO`, phân tích bands. Trên một số firmware âm thanh Bluetooth/USB/Radio/FM không vào output mix hoặc bị chặn; không đảm bảo hoạt động mọi nguồn.
- Demo rõ trạng thái, không mạo nhận âm thanh thật.
- Floating bubble + bottom strip, chạy qua Android `TYPE_APPLICATION_OVERLAY` và foreground service; cần quyền người dùng.
- Cài đặt lưu tại SharedPreferences.

## Giới hạn quan trọng
- Chưa build/kiểm thử thực tế vì môi trường tạo file không có Android SDK/Gradle. Cần kiểm chứng và sửa nếu firmware đầu xe yêu cầu thay đổi.
- Font thư pháp phụ thuộc font có trên đầu xe. Để bảo đảm chữ thư pháp giống thiết kế trên mọi xe: cần tích hợp font Việt hóa được cấp phép vào `assets/fonts` trong bước kế tiếp.
- Hình ngựa dùng ảnh tĩnh cắt từ minh họa do chính dự án tạo; không phải sprite/vector chỉnh sửa được.
- `Visualizer` phiên bản hệ thống có thể từ chối audio session 0, hoặc chỉ hỗ trợ một số ứng dụng. Lộ trình tiếp theo có thể thêm MediaProjection/AudioPlaybackCapture cho Android 10+, theo cơ chế xin phép thích hợp.
- Một số Android 12+ hạn chế touch-through đối với overlay; đã dùng opacity 0.7 nhưng hành vi vẫn phụ thuộc hệ điều hành và vùng chồng lấn. Thanh LED nên để ngoài vùng chạm quan trọng.
- Android có thể chặn mở Activity từ nền ngay cả khi chạm bong bóng tùy phiên bản / firmware. Nút mở app trong notification là phương án dự phòng.
- Vui lòng không dùng app để che bản đồ hoặc tăng độ chói gây mất tập trung khi xe đang chạy.

## Cấu trúc
- `MainActivity.kt`: màn hình chính và thiết lập
- `ClockActivity.kt`: đồng hồ toàn màn hình
- `VisualizerView.kt`: render Canvas cho đồng hồ, ngựa, chữ, dải LED
- `AudioEngine.kt`: FFT nguồn audio session 0
- `OverlayService.kt`: overlay nền và bong bóng
- `Config.kt`: cấu hình lưu trên máy
