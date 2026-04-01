# TÀI LIỆU TÍCH HỢP API FRONTEND (SMARTF&B S-01 -> S-10)

Tài liệu cung cấp chi tiết toàn bộ Endpoints hiện có trong dự án, bao gồm Method, Request Body, Query Params, Headers và cấu trúc Response chuẩn.

> **💡 Quy tắc chung:**
>
> - Toàn bộ API (ngoại trừ `/register`, `/login`, và `/pin-login`, quên mật khẩu) đều yêu cầu Header: `Authorization: Bearer <access_token>`.
> - Format Response chuẩn của Backend (áp dụng cho mọi API):
>   ```json
>   {
>     "success": true, // hoặc false
>     "data": { ... }, // Dữ liệu trả về khi success = true
>     "error": null,   // Hoặc object { "code": "...", "message": "..." } khi success = false
>     "timestamp": "2026-03-30T10:00:00Z"
>   }
>   ```

---

## 🔐 1. MODULE XÁC THỰC (AUTH) - Prefix: `/api/v1/auth`

### 1.1 Đăng ký Tenant mới (Chủ quán)

- **Method:** `POST /register`
- **Request Body:**
  ```json
  {
    "tenantName": "Cà phê Phúc Long", // Bắt buộc, max 255 char
    "email": "owner@phuclong.vn", // Bắt buộc, valid email
    "password": "Password123!", // Bắt buộc, min 8 char
    "ownerName": "Nguyễn Văn A", // Bắt buộc, max 255 char
    "phone": "0987654321", // Tùy chọn
    "planSlug": "standard" // Bắt buộc (basic | standard | premium)
  }
  ```
- **Response `data`:**
  ```json
  {
    "accessToken": "ey...",
    "refreshToken": "ey...",
    "user": { "id": "uuid", "fullName": "Nguyễn Văn A", "role": "OWNER" }
  }
  ```

### 1.2 Đăng nhập (Email/Password)

- **Method:** `POST /login`
- **Request Body:**
  ```json
  {
    "email": "owner@phuclong.vn",
    "password": "Password123!"
  }
  ```
- **Response `data`:** Tương tự `register`, trả về Access Token + Refresh Token.

### 1.3 Đăng nhập POS (Bằng PIN cho Cashier/Barista)

- **Method:** `POST /pin-login`
- **Request Body:**
  ```json
  {
    "tenantId": "uuid-cua-tenant",
    "userId": "uuid-cua-nhan-vien",
    "pin": "123456" // PIN 4-6 số
  }
  ```
- **Response `data`:** Tương tự `login`.

### 1.4 Làm mới Token (Refresh Token)

- **Method:** `POST /refresh`
- **Request Body:**
  ```json
  {
    "refreshToken": "ey..." // Token được lấy từ lúc login
  }
  ```
- **Response `data`:** Cấp lại Access Token và Refresh Token mới.

### 1.5 Đổi Scope làm việc sang 1 Chi nhánh

- **Method:** `POST /select-branch`
- **Headers:** Bắt buộc có Access Token hiện tại
- **Request Body:**
  ```json
  {
    "branchId": "uuid-cua-chi-nhanh"
  }
  ```
- **Response `data`:** Trả về Token mới có chứa `branchId`. FE lưu lại token này để gọi các API POS tiếp theo.

### 1.6 Quên mật khẩu (Flow 3 bước)

- `POST /forgot-password`
  - **Body:** `{ "email": "user@email.com" }` -> Server gửi mã OTP 6 số.
- `POST /verify-otp`
  - **Body:** `{ "email": "user@email.com", "otp": "123456" }`
  - **Response `data`:** `{ "resetToken": "token-tam-thoi-15-phut" }`
- `POST /reset-password`
  - **Body:** `{ "email": "user@email.com", "resetToken": "token-tam-thoi...", "newPassword": "NewPassword123!" }`

---

## 💳 2. MODULE KẾ HOẠCH & SUBSCRIPTION - Prefix: `/api/v1/plans` & `/api/v1/subscriptions`

### 2.1 Lấy danh sách Gói cước (Dùng ở trang Đăng ký)

- **Method:** `GET /api/v1/plans`
- **Response `data`:** Array các Object:
  `[{ "id": "uuid", "name": "Standard", "slug": "standard", "priceMonthly": 500000, "maxBranches": 3, "features": {"POS": true, "INVENTORY": true} }]`

### 2.2 Xem thông tin gói cước đang dùng

- **Method:** `GET /api/v1/subscriptions/current`
- **Headers:** Bắt buộc có Token (của Owner)
- **Response `data`:**
  `{ "id": "uuid", "plan": {...}, "status": "ACTIVE", "startDate": "...", "endDate": "..." }`

---

## 🏢 3. MODULE CHI NHÁNH (BRANCH) - Prefix: `/api/v1/branches`

### 3.1 Lấy danh sách Chi nhánh của Tenant

- **Method:** `GET /`
- **Response `data`:** `[{ "id": "uuid", "name": "Chi nhánh 1", "code": "CN01", "address": "...", "status": "ACTIVE" }]`

### 3.2 Tạo Chi nhánh mới

- **Method:** `POST /`
- **Request Body:**
  ```json
  {
    "name": "Chi nhánh Bùi Viện", // Bắt buộc
    "code": "BV01", // Bắt buộc, unique
    "address": "Phố Bùi Viện, Q1",
    "phone": "0123456789",
    "latitude": 10.767, // Tùy chọn
    "longitude": 106.693, // Tùy chọn
    "managerUserId": "uuid-quan-ly" // Tùy chọn
  }
  ```
- **Response `data`:** Object Branch vừa tạo. `PUT /{branchId}` dùng chung Body.

### 3.3 Gán nhân viên vào Chi nhánh

- **Method:** `POST /{branchId}/users`
- **Request Body:** `{ "userId": "uuid-nhan-vien" }`

---

## 🍔 4. MODULE THỰC ĐƠN (MENU) - Prefix: `/api/v1/menu`

### 4.1 Danh mục (Categories)

- `GET /categories` - Lấy danh sách (Query Params: `?page=0&size=20&keyword=...`) -> Trả về JSON chứa `content`, `totalPages`, `totalElements`.
- `GET /categories/active` - Lấy toàn bộ không phân trang (chỉ Active).
- `POST /categories` - Tạo mới:
  ```json
  {
    "name": "Cà Phê", // Bắt buộc
    "description": "Các loại cafe pha máy",
    "displayOrder": 1,
    "isActive": true
  }
  ```
- `PUT /categories/{id}` & `DELETE /categories/{id}`

### 4.2 Món Ăn / Đồ Uống (Items)

- `GET /items` - List phân trang (có query `keyword` tìm theo pg_trgm).
- `GET /items/active` - Lấy ds món đang bán cho màn hình POS.
- `POST /items` - Tạo món mới:
  ```json
  {
    "categoryId": "uuid", // Bắt buộc (hoặc null nếu chưa phân loại)
    "name": "Cà phê Sữa Đá", // Bắt buộc
    "basePrice": 35000, // Bắt buộc, >= 0
    "unit": "Ly", // Tùy chọn (Ly, Cốc, Phần)
    "imageUrl": "https://...", // Tùy chọn
    "isSyncDelivery": false // Tùy chọn
  }
  ```
- `PUT /items/{id}` & `DELETE /items/{id}`
- `GET /items/{id}/recipe` - Xem công thức của món ăn.

### 4.3 Giá món riêng cho từng Chi Nhánh (Branch Items)

- `GET /branches/{branchId}/items/{itemId}` - Lấy giá cụ thể tại một chi nhánh.
- `PUT /branches/{branchId}/items/{itemId}/price` - Thiết lập giá Over-ride:
  ```json
  {
    "branchPrice": 45000, // Nếu null -> Sẽ lấy lại basePrice từ bảng Items
    "isAvailable": true // Tắt bật món này tại riêng chi nhánh đó
  }
  ```

### 4.4 Addons (Topping)

- `GET /addons` & `GET /addons/active`.
- `POST /addons` - Tạo topping:
  ```json
  {
    "name": "Trân châu đen", // Bắt buộc
    "extraPrice": 5000, // Bắt buộc
    "isActive": true
  }
  ```
- `PUT /addons/{id}` & `DELETE /addons/{id}`.

### 4.5 Công thức Pha chế (Recipe)

- `POST /recipes` - Thêm nguyên liệu định lượng:
  ```json
  {
    "targetItemId": "uuid-mon-ban",
    "ingredientItemId": "uuid-nguyen-lieu",
    "quantity": 25.5, // Số lượng (vd: lấy 25.5 gram đường)
    "unit": "gram"
  }
  ```
- `PUT /recipes/{recipeId}` - Sửa số lượng `{"quantity": 30.0, "unit": "gram"}`
- `DELETE /recipes/{recipeId}` - Xóa liên kết công thức.

---

## 🪑 5. SƠ ĐỒ BÀN (TABLES & ZONES) - Prefix: `/api/v1/branches/{branchId}`

_(Yêu cầu URL luôn chứa `branchId` hiện tại của người thao tác)_

### 5.1 Quản lý Khu Vực (Zones)

- `GET /zones` - Lấy danh sách Zone.
- `POST /zones`
  ```json
  {
    "name": "Tầng 1", // Bắt buộc (Tên khu vực)
    "floorNumber": 1 // Số định danh tầng (để sắp xếp)
  }
  ```
- `PUT /zones/{zoneId}` & `DELETE /zones/{zoneId}`

### 5.2 Quản lý Sơ đồ Bàn (Tables)

- `GET /tables` - Lấy array List trải phẳng các bàn (chứa `id`, `name`, `status`, `zoneId`, `positionX`, `positionY`, `shape`) -> Render giao diện Grid kéo thả.
- `GET /tables/stats/occupied-count` - Widget: Đếm số bàn có khách (OCCUPIED).
- `POST /tables` - Tạo Bàn (Thả bàn mới vào giao diện):
  ```json
  {
    "zoneId": "uuid-khu-vuc", // Có thể null
    "name": "Bàn A1", // Bắt buộc, max 50 char
    "capacity": 4, // Sức chứa, > 0
    "shape": "square" // Bắt buộc: "square" (Vuông) hoặc "round" (Tròn)
  }
  ```

### 5.3 Batch Update Layout (Drag & Drop Realtime) - RẤT QUAN TRỌNG

- **Method:** `PUT /tables/positions`
- **Mô tả:** Sau khi User kéo thẻ bàn tới vị trí mới trên sơ đồ và thả tay, FE gửi mảng tọa độ lên endpoint này. Backend sẽ lưu lại và kích hoạt event WebSocket `/topic/tables/{branchId}` để các màn hình khác cập nhật ngay lập tức.
- **Request Body:**
  ```json
  {
    "tables": [
      {
        "tableId": "uuid-ban-a1",
        "positionX": 150.5,
        "positionY": 200.0
      },
      {
        "tableId": "uuid-ban-a2",
        "positionX": 300.25,
        "positionY": 200.0
      }
    ]
  }
  ```
- **Response:** Không trả data (`200 OK` rỗng). Listen trên WebSocket để update UI local.

---

## 🍽️ 6. MODULE ĐƠN HÀNG (ORDER) - S-10: Order Realtime WebSocket - Prefix: `/api/v1/orders`

### 6.1 Tạo Đơn Hàng mới

- **Method:** `POST /`
- **Headers:** Bearer Token
- **Request Body:**
  ```json
  {
    "tableId": "uuid-ban-a1", // Bàn hiện tại (hoặc null nếu delivery)
    "source": "IN_STORE", // Enum: IN_STORE | DELIVERY | TAKEAWAY
    "notes": "Không dá, ít đường",
    "items": [
      {
        "itemId": "uuid-item-cafe",
        "itemName": "Cà phê Sữa Đá",
        "quantity": 2,
        "unitPrice": 35000,
        "addons": ["Trân châu đen"], // Tùy chọn, array tên addons
        "notes": "Ít dá"
      }
    ]
  }
  ```
- **Response `data`:**
  ```json
  {
    "id": "uuid-order",
    "orderNumber": "ORD-ABC1-260331153042", // Auto generate
    "tableId": "uuid-ban-a1",
    "status": "PENDING", // Trạng thái mới
    "subtotal": 70000,
    "discountAmount": 0,
    "totalAmount": 70000,
    "items": [
      {
        "id": "uuid-item",
        "itemId": "uuid-item-cafe",
        "itemName": "Cà phê Sữa Đá",
        "quantity": 2,
        "unitPrice": 35000,
        "totalPrice": 70000,
        "status": "PENDING"
      }
    ]
  }
  ```

### 6.2 Cập nhật Trạng thái Đơn Hàng (REST API)

- **Method:** `PUT /{orderId}/status`
- **Headers:** Bearer Token
- **Request Body:**
  ```json
  {
    "newStatus": "PROCESSING", // Enum: PENDING | PROCESSING | COMPLETED | CANCELLED
    "reason": "Bếp đang pha chế" // Tùy chọn, ghi log lý do
  }
  ```
- **Response `data`:** Object OrderResponse như trên, status đã cập nhật.
- **Broadcast WebSocket:** Sau khi API trả about, Backend auto-triggerOrderStatusChangedEvent → broadcast tới `/topic/orders/{branchId}` để tất cả client nghe được realtime.

### 6.3 Hủy Đơn Hàng

- **Method:** `POST /{orderId}/cancel`
- **Headers:** Bearer Token
- **Request Body:**
  ```json
  {
    "reason": "Khách không đủ tiền"
  }
  ```
- **Response `data`:** OrderResponse với status = CANCELLED.

### 6.4 Xem Chi tiết Đơn Hàng

- **Method:** `GET /{orderId}`
- **Headers:** Bearer Token
- **Response `data`:** OrderResponse chi tiết.

### 6.5 Danh sách Đơn Hàng (Lọc & Phân trang)

- **Method:** `GET /`
- **Query Params:**
  - `status=PENDING` → Lọc theo trạng thái
  - `from=2026-03-31T00:00:00Z` → Từ ngày
  - `to=2026-03-31T23:59:59Z` → Đến ngày
  - `tableId=uuid` → Lọc theo bàn
  - `page=0&size=20` → Phân trang
- **Response `data`:** Array Object + pagination metadata.

### 6.6 WebSocket Realtime: Subscribe Broadcast

> **✅ Bắt BUỘC là Feature S-10 — Realtime WebSocket cho tất cả client**

#### Kết nối WebSocket:

```javascript
// Frontend JavaScript (ví dụ)
// Endpoint: ws://localhost:8080/ws
const url = `ws://${window.location.hostname}:${window.location.port || 8080}/ws`;
const sockJS = new SockJS(url);
const stompClient = Stomp.over(sockJS);

stompClient.connect(
  {},
  function () {
    console.log("✅ WebSocket connected");

    // Subscribe broadcast mới trạng thái đơn hàng
    stompClient.subscribe(`/topic/orders/${branchId}`, function (message) {
      const orderUpdate = JSON.parse(message.body);
      console.log("📡 Nhận cập nhật đơn:", orderUpdate);
      // Update UI ngay lập tức
      updateOrderOnUI(orderUpdate);
    });

    // (Optional) Gửi message để cập nhật status qua WebSocket
    // Có thể dùng HTTP PUT /orders/{id}/status là đủ
    // const updateMsg = {
    //   orderId: "...",
    //   newStatus: "PROCESSING",
    //   reason: "Bếp vạn pha chế"
    // };
    // stompClient.send("/app/orders/change-status", {}, JSON.stringify(updateMsg));
  },
  function (error) {
    console.error("WebSocket error:", error);
  },
);
```

#### Topic broadcast:

- **Topic:** `/topic/orders/{branchId}`
  - Khi bất kỳ đơn hàng nào tại `{branchId}` thay đổi, backend publish OrderResponse mới tới topic này.
  - Tất cả client subscribe topic này đều nhận được update realtime.

#### Events trigger broadcast:

| Event                   | Trigger                                              | Broadcast Content                  |
| ----------------------- | ---------------------------------------------------- | ---------------------------------- |
| OrderCreatedEvent       | API POST /api/v1/orders → tạo đơn mới                | OrderResponse (status = PENDING)   |
| OrderStatusChangedEvent | API PUT /api/v1/orders/{id}/status → cập nhật status | OrderResponse (status đã thay đổi) |
| OrderCompletedEvent     | API cập nhật sang COMPLETED                          | OrderResponse (status = COMPLETED) |
| OrderCancelledEvent     | API POST /api/v1/orders/{id}/cancel                  | OrderResponse (status = CANCELLED) |

#### Ghi chú quan trọng:

1. **Bếp (Kitchen Display System):** Subscribe `/topic/orders/{branchId}` + show những order có status=PENDING hoặc PROCESSING.
2. **Quầy (Checkout):** Subscribe `/topic/orders/{branchId}` + show hoàn tất → tính tiền → thanh toán.
3. **Waiter:** Subscribe `/topic/orders/{branchId}` + show từng item trong order → phục vụ từng phần.
4. **Quản lý:** Có thể không subscribe, chỉ query API `GET /api/v1/orders` định kỳ hoặc theo nhu cầu.

---

## 🧾 7. MODULE THANH TOÁN (PAYMENT) - _Coming Soon (S-11)_

---

## 📊 🚀 TÓMLỊCH PHÁT HÀNH (TIMELINE)

| Sprint       | Module                                  | Status           |
| ------------ | --------------------------------------- | ---------------- |
| S-01 to S-08 | Auth, Branch, Menu, Table, Subscription | ✅ Hoàn thành    |
| **S-09**     | **Inventory Module**                    | 🔄 Đang làm      |
| **S-10**     | **Order Realtime WebSocket**            | ✅ **COMPLETED** |
| S-11         | Payment & Invoice                       | 📅 Sắp tới       |
| S-12         | Report & Analytics                      | 📅 Sắp tới       |
| S-13+        | Advanced Features                       | Tùy hoàn cảnh    |
