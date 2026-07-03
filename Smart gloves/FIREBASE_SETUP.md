# Hướng dẫn cấu hình Firebase Realtime Database

## Thông tin Database hiện tại
- **URL Database**: `https://smart-glove-1769e-default-rtdb.asia-southeast1.firebasedatabase.app`
- **Project ID**: `smart-glove-1769e`

## Các bước cấu hình trong Firebase Console

### Bước 1: Bật Realtime Database
1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Chọn project: **smart-glove-1769e**
3. Vào **Realtime Database** ở menu bên trái
4. Nếu chưa tạo database:
   - Click **Create Database**
   - Chọn location: **asia-southeast1** (Singapore)
   - Chọn mode: **Start in test mode** (hoặc production mode với rules phù hợp)
   - Click **Enable**

### Bước 2: Cấu hình Database Rules (QUAN TRỌNG - BẮT BUỘC)

**⚠️ LỖI "Permission denied" xảy ra nếu chưa cấu hình Rules này!**

Vào tab **Rules** trong Realtime Database và cấu hình như sau:

#### Option 1: Test mode (KHUYẾN NGHỊ cho phát triển)
Cho phép đọc/ghi không cần authentication:
```json
{
  "rules": {
    "gestures": {
      ".read": true,
      ".write": true
    },
    "history": {
      ".read": true,
      ".write": true
    },
    "speechHistory": {
      ".read": true,
      ".write": true
    },
    "users": {
      "$userId": {
        ".read": true,
        ".write": true,
        "profile": {
          ".read": true,
          ".write": true
        },
        "gestures": {
          ".read": true,
          ".write": true
        },
        "categories": {
          ".read": true,
          ".write": true
        },
        "history": {
          ".read": true,
          ".write": true
        },
        "speechHistory": {
          ".read": true,
          ".write": true
        }
      }
    }
  }
}
```

#### Option 2: Cho phép tất cả (test nhanh)
```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

**Sau khi dán rules:**
1. Click **Publish** (nút ở trên cùng)
2. Đợi vài giây để rules được áp dụng
3. Thử lại trong app

**Lưu ý QUAN TRỌNG**: 
- Rules mặc định của Firebase là **DENY ALL** (từ chối tất cả)
- Nếu không cấu hình rules, sẽ luôn bị lỗi "Permission denied"
- Phải click **Publish** thì rules mới có hiệu lực

### Bước 3: Kiểm tra kết nối

1. Chạy app trên thiết bị/emulator
2. Thử thêm một cử chỉ mới trong màn hình "Thêm cử chỉ"
3. Kiểm tra trong Firebase Console → Realtime Database → xem có dữ liệu mới không

### Bước 4: Xem dữ liệu trong Firebase Console

1. Vào **Realtime Database**
2. Sẽ thấy cấu trúc:
   ```
   {
     "gestures": {
       "key1": {
         "name": "Tên cử chỉ",
         "sensorValues": "Giá trị cảm biến",
         "text": "Câu nói",
         "timestamp": 1234567890
       }
     },
     "history": {
       "key1": {
         "gestureName": "Tên cử chỉ",
         "sentence": "Câu nói",
         "timestamp": 1234567890
       }
     },
     "speechHistory": {
       "key1": {
         "text": "Văn bản được nhận diện từ giọng nói",
         "timestamp": 1234567890
       }
     }
   }
   ```

## Xử lý lỗi thường gặp

### Lỗi: "Permission denied"
- **Nguyên nhân**: Database Rules chưa được cấu hình đúng
- **Giải pháp**: Cập nhật Rules như ở Bước 2

### Lỗi: "Database not found" hoặc "Failed to connect"
- **Nguyên nhân**: Realtime Database chưa được bật
- **Giải pháp**: Làm theo Bước 1

### Lỗi: "Network error"
- **Nguyên nhân**: Thiết bị không có internet hoặc URL database sai
- **Giải pháp**: Kiểm tra kết nối internet và đảm bảo file `google-services.json` đúng

## Thông tin cần cung cấp khi có vấn đề

Nếu vẫn gặp lỗi, cần cung cấp:
1. **URL Database** từ Firebase Console (tab Realtime Database → Data → URL)
2. **Database Rules** hiện tại
3. **Thông báo lỗi cụ thể** từ Logcat
4. **Screenshot** màn hình Firebase Console (tab Database và Rules)




