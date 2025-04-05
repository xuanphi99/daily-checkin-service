# daily-checkin-service

# 🎮 Daily Check-in Rewards Service (Điểm danh hàng ngày)

## 📝 Mô tả dự án

Hệ thống backend cho tính năng "Điểm danh hàng ngày" của ứng dụng Gami App, cho phép người dùng điểm danh trong các khung giờ quy định để nhận thưởng điểm Lotus+. Mỗi người dùng có thể điểm danh tối đa 7 ngày trong 1 tháng, điểm cộng mỗi ngày theo một quy tắc tăng dần.

---

## 🚀 Tính năng chính

| API                           | Mô tả |
|-------------------------------|------|
| `POST /api/v1/users`          | Tạo người dùng mới |
| `GET /api/v1/users/{id}`      | Lấy thông tin người dùng |
| `GET /api/v1/check-in/status` | Lấy danh sách trạng thái điểm danh trong tháng |
| `POST /api/v1/checkin`        | Điểm danh hàng ngày (có kiểm tra giờ và lock Redis) |
| `GET /api/v1/points/history`  | Lấy lịch sử cộng điểm (hỗ trợ phân trang) |
| `POST /api/v1/points/deduct`  | API trừ điểm |

---

## 🧠 Quy tắc nghiệp vụ

- Cho phép điểm danh tối đa 1 lần/ngày.
- Chỉ có thể điểm danh trong khung giờ: `9h-11h` và `19h-21h`.
- Mỗi tháng điểm danh tối đa **7 ngày**, qua tháng sẽ reset lại.
- Không yêu cầu điểm danh liên tục.
- Điểm cộng theo ngày:

| Lần điểm danh trong tháng | Số điểm cộng |
|---------------------------|--------------|
| Ngày 1                    | +1 điểm      |
| Ngày 2                    | +2 điểm      |
| Ngày 3                    | +3 điểm      |
| Ngày 4                    | +5 điểm      |
| Ngày 5                    | +8 điểm      |
| Ngày 6                    | +13 điểm     |
| Ngày 7                    | +21 điểm     |

---

## 🧰 Tech Stack

- 💻 **Java 8**, **Spring Boot 2.x**
- 🐘 **MYSQL**
- ⚡ **Redis** (Redisson client)
- 📘 JPA / Hibernate
- 📦 Gradle

---

## 📦 Cài đặt & Chạy dự án

```bash
# Clone repo
git clone https://github.com/xuanphi99/daily-checkin-service/
cd checkin-service

# Build project với Gradle wrapper
./gradlew build

# Run app
./gradlew bootRun