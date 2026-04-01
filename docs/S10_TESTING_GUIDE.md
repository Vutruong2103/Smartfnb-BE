## 🧪 S-10: ORDER REALTIME WEBSOCKET — GUIDE TEST & TEST DATA

### 📍 I. SWAGGER UI ACCESS

**URL:** http://localhost:8080/swagger-ui.html

```
Steps:
1. Mở browser → http://localhost:8080/swagger-ui.html
2. Swagger UI tự động load tất cả endpoints từ OpenAPI config
3. Tìm section "Order - Đơn hàng" (5 endpoints)
4. Authorized: Click "Authorize" button trên top-right
   - Chọn "Bearer Token"
   - Paste access_token (xem section II để lấy token)
```

---

### 📖 II. PREREQUISITE: LẤY JWT TOKEN

Trước tiên phải đăng nhập để lấy access token.

#### **API: Login**

```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "owner@smartfnb.vn",
  "password": "Password123!"
}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ...",
    "user": {
      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "fullName": "Chủ Quán SmartF&B",
      "role": "OWNER"
    }
  }
}
```

**➡️ Copy value `data.accessToken` để dùng cho các API khác**

---

### 👤 III. STEP 1: SELECT BRANCH (CẤP QUYỀN CHI NHÁNH)

Sau khi login, phải select chi nhánh để có branchId trong token.

#### **API: Select Branch**

```http
POST http://localhost:8080/api/v1/auth/select-branch
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "branchId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ...(token mới có branchId)",
    "refreshToken": "..."
  }
}
```

**➡️ Copy token mới này — đây là token có branchId, dùng cho Order APIs**

---

### 🎯 IV. TEST DATA SAMPLE (ĐỂ COPY-PASTE)

#### **Test Data UUIDs:**

```
Tenant ID:    550e8400-e29b-41d4-a716-446655440001
Branch ID:    550e8400-e29b-41d4-a716-446655440002
Table ID 1:   550e8400-e29b-41d4-a716-446655440003
Table ID 2:   550e8400-e29b-41d4-a716-446655440004
Staff ID:     550e8400-e29b-41d4-a716-446655440005
Item ID 1:    550e8400-e29b-41d4-a716-446655440100 (Cà phê)
Item ID 2:    550e8400-e29b-41d4-a716-446655440101 (Trà sữa)
```

---

### 📋 V. SWAGGER TEST — ENDPOINT-BY-ENDPOINT

#### **1️⃣ POST /api/v1/orders — Tạo Đơn Hàng Mới**

**Swagger Path:**

```
Orders - Đơn hàng section
  ↓
POST /api/v1/orders (có luôn status 201 Created)
  ↓ Click "Try it out"
```

**Request Body (Copy-Paste):**

```json
{
  "tableId": "550e8400-e29b-41d4-a716-446655440003",
  "source": "IN_STORE",
  "notes": "Không dá, ít đường",
  "items": [
    {
      "itemId": "550e8400-e29b-41d4-a716-446655440100",
      "itemName": "Cà phê Sữa Đá",
      "quantity": 2,
      "unitPrice": 35000,
      "addons": ["Trân châu đen"],
      "notes": "Ít dá"
    },
    {
      "itemId": "550e8400-e29b-41d4-a716-446655440101",
      "itemName": "Trà Sữa",
      "quantity": 1,
      "unitPrice": 30000,
      "addons": ["Trân châu trắng"],
      "notes": null
    }
  ]
}
```

**Expected Response (201):**

```json
{
  "success": true,
  "data": {
    "id": "7a8b9c0d-e1f2-4a5b-8c9d-0e1f2a3b4c5d",
    "orderNumber": "ORD-550E-260331200000",
    "tableId": "550e8400-e29b-41d4-a716-446655440003",
    "source": "IN_STORE",
    "status": "PENDING",
    "subtotal": 100000,
    "discountAmount": 0,
    "taxAmount": 0,
    "totalAmount": 100000,
    "notes": "Không dá, ít đường",
    "completedAt": null,
    "items": [
      {
        "id": "item-uuid-1",
        "itemId": "550e8400-e29b-41d4-a716-446655440100",
        "itemName": "Cà phê Sữa Đá",
        "quantity": 2,
        "unitPrice": 35000,
        "totalPrice": 70000,
        "addons": "Trân châu đen",
        "notes": "Ít dá",
        "status": "PENDING"
      },
      {
        "id": "item-uuid-2",
        "itemId": "550e8400-e29b-41d4-a716-446655440101",
        "itemName": "Trà Sữa",
        "quantity": 1,
        "unitPrice": 30000,
        "totalPrice": 30000,
        "addons": "Trân châu trắng",
        "notes": null,
        "status": "PENDING"
      }
    ]
  },
  "error": null,
  "timestamp": "2026-03-31T20:15:00Z"
}
```

**🔔 WEBSOCKET EVENT:** Lúc này server phát `OrderCreatedEvent` → broadcast tới `/topic/orders/{branchId}`

**➡️ COPY `data.id` (order ID) để dùng cho các API tiếp theo**

---

#### **2️⃣ GET /api/v1/orders/{id} — Xem Chi Tiết Đơn**

**Swagger Path:**

```
Orders section
  ↓
GET /api/v1/orders/{id}
  ↓ Click "Try it out"
  ↓ Paste order ID vào
```

**URL:**

```
http://localhost:8080/api/v1/orders/7a8b9c0d-e1f2-4a5b-8c9d-0e1f2a3b4c5d
```

**Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "7a8b9c0d-e1f2-4a5b-8c9d-0e1f2a3b4c5d",
    "orderNumber": "ORD-550E-260331200000",
    "tableId": "550e8400-e29b-41d4-a716-446655440003",
    "source": "IN_STORE",
    "status": "PENDING",
    "subtotal": 100000,
    "discountAmount": 0,
    "taxAmount": 0,
    "totalAmount": 100000,
    "notes": "Không dá, ít đường",
    "completedAt": null,
    "items": [
      {
        "id": "item-uuid-1",
        "itemId": "550e8400-e29b-41d4-a716-446655440100",
        "itemName": "Cà phê Sữa Đá",
        "quantity": 2,
        "unitPrice": 35000,
        "totalPrice": 70000,
        "status": "PENDING"
      },
      {
        "id": "item-uuid-2",
        "itemId": "550e8400-e29b-41d4-a716-446655440101",
        "itemName": "Trà Sữa",
        "quantity": 1,
        "unitPrice": 30000,
        "totalPrice": 30000,
        "status": "PENDING"
      }
    ]
  }
}
```

---

#### **3️⃣ PUT /api/v1/orders/{id}/status — Cập Nhật Trạng Thái**

**Swagger Path:**

```
Orders section
  ↓
PUT /api/v1/orders/{id}/status
  ↓ Click "Try it out"
```

**URL:**

```
http://localhost:8080/api/v1/orders/7a8b9c0d-e1f2-4a5b-8c9d-0e1f2a3b4c5d/status
```

**Request Body — Change to PROCESSING:**

```json
{
  "newStatus": "PROCESSING",
  "reason": "Bếp đang pha chế"
}
```

**Response (200):**

```json
{
  "success": true,
  "data": {
    "id": "7a8b9c0d-e1f2-4a5b-8c9d-0e1f2a3b4c5d",
    "orderNumber": "ORD-550E-260331200000",
    "status": "PROCESSING", // ← CHANGED
    "items": [
      {
        "status": "PROCESSING" // ← Items cũng update
      }
    ]
  }
}
```

**🔔 WEBSOCKET EVENT:** Phát `OrderStatusChangedEvent` → broadcast `/topic/orders/{branchId}`

---

**Request Body 2 — Change to COMPLETED:**

```json
{
  "newStatus": "COMPLETED",
  "reason": "Đã pha chế xong"
}
```

**Response (200):**

```json
{
  "success": true,
  "data": {
    "status": "COMPLETED", // ← COMPLETED
    "completedAt": "2026-03-31T20:20:00Z"
  }
}
```

**🔔 WEBSOCKET EVENT:** Phát `OrderCompletedEvent` → broadcast `/topic/orders/{branchId}`

---

#### **4️⃣ POST /api/v1/orders/{id}/cancel — Hủy Đơn**

**Swagger Path:**

```
Orders section
  ↓
POST /api/v1/orders/{id}/cancel
  ↓ Click "Try it out"
```

**URL:**

```
http://localhost:8080/api/v1/orders/7a8b9c0d-e1f2-4a5b-8c9d-0e1f2a3b4c5d/cancel
```

**Request Body:**

```json
{
  "reason": "Khách không muốn"
}
```

**Response (200):**

```json
{
  "success": true,
  "data": {
    "status": "CANCELLED",
    "items": [
      {
        "status": "CANCELLED"
      }
    ]
  }
}
```

**🔔 WEBSOCKET EVENT:** Phát `OrderCancelledEvent` → broadcast `/topic/orders/{branchId}`

---

#### **5️⃣ GET /api/v1/orders — Danh Sách Đơn (Phân Trang, Lọc)**

**Swagger Path:**

```
Orders section
  ↓
GET /api/v1/orders
  ↓ Click "Try it out"
  ↓ Điền query params
```

**Query Params (Tùy Chọn):**

```
?status=PENDING&page=0&size=20&from=2026-03-31T00:00:00Z&to=2026-03-31T23:59:59Z&tableId=550e8400-e29b-41d4-a716-446655440003
```

**Response (200):**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid1",
        "orderNumber": "ORD-550E-260331200000",
        "tableId": "550e8400-e29b-41d4-a716-446655440003",
        "tableName": "Bàn 550E",
        "status": "PENDING",
        "totalAmount": 100000,
        "createdAt": "2026-03-31T20:15:00Z",
        "staffName": "Staff 550E"
      },
      {
        "id": "uuid2",
        "orderNumber": "ORD-550E-260331200010",
        "tableId": "550e8400-e29b-41d4-a716-446655440004",
        "tableName": "Bàn 550F",
        "status": "PROCESSING",
        "totalAmount": 150000,
        "createdAt": "2026-03-31T20:16:00Z",
        "staffName": "Staff 550E"
      }
    ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 2,
    "totalPages": 1,
    "isFirst": true,
    "isLast": true
  }
}
```

---

### 🌐 VI. WEBSOCKET TEST — MÔ PHỎNG 2 CLIENT ĐỒNG THỜI

#### **Cách 1: Dùng WebSocket Client Browser Console**

```javascript
// Tab 1: Kitchen Screen
const ws1 = new SockJS("http://localhost:8080/ws");
const stomp1 = Stomp.over(ws1);
stomp1.connect({}, () => {
  console.log("✅ Client 1 (Kitchen) connected");
  stomp1.subscribe(
    `/topic/orders/550e8400-e29b-41d4-a716-446655440002`,
    (msg) => {
      console.log("📡 [Kitchen] Nhận order:", JSON.parse(msg.body));
      // Update Kitchen Display System (KDS)
    },
  );
});

// Tab 2: Waiter Screen
const ws2 = new SockJS("http://localhost:8080/ws");
const stomp2 = Stomp.over(ws2);
stomp2.connect({}, () => {
  console.log("✅ Client 2 (Waiter) connected");
  stomp2.subscribe(
    `/topic/orders/550e8400-e29b-41d4-a716-446655440002`,
    (msg) => {
      console.log("📡 [Waiter] Nhận order:", JSON.parse(msg.body));
      // Update Waiter app
    },
  );
});
```

**Steps:**

1. Mở 2 tab browser
2. Mỗi tab paste code trên vào Developer Console (F12)
3. Cả 2 tab sẽ hiện "✅ Client connected"
4. Tạo order từ Swagger (step V.1) → **Cả 2 tab sẽ nhận broadcast**

---

#### **Cách 2: Dùng Postman WebSocket Collection**

File: `S10_WebSocket_Postman_Collection.json` (create file này)

```json
{
  "info": {
    "name": "S-10 Order Realtime WebSocket",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "1. Auth Login",
      "request": {
        "method": "POST",
        "header": [{ "key": "Content-Type", "value": "application/json" }],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"email\": \"owner@smartfnb.vn\",\n  \"password\": \"Password123!\"\n}"
        },
        "url": "http://localhost:8080/api/v1/auth/login"
      }
    },
    {
      "name": "2. Select Branch",
      "request": {
        "method": "POST",
        "header": [
          { "key": "Authorization", "value": "Bearer {{accessToken}}" },
          { "key": "Content-Type", "value": "application/json" }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"branchId\": \"550e8400-e29b-41d4-a716-446655440002\"\n}"
        },
        "url": "http://localhost:8080/api/v1/auth/select-branch"
      }
    },
    {
      "name": "3. Create Order (Trigger WebSocket)",
      "request": {
        "method": "POST",
        "header": [
          { "key": "Authorization", "value": "Bearer {{branchToken}}" },
          { "key": "Content-Type", "value": "application/json" }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"tableId\": \"550e8400-e29b-41d4-a716-446655440003\",\n  \"source\": \"IN_STORE\",\n  \"notes\": \"Test S-10 realtime\",\n  \"items\": [\n    {\n      \"itemId\": \"550e8400-e29b-41d4-a716-446655440100\",\n      \"itemName\": \"Cà phê\",\n      \"quantity\": 2,\n      \"unitPrice\": 35000,\n      \"addons\": null,\n      \"notes\": null\n    }\n  ]\n}"
        },
        "url": "http://localhost:8080/api/v1/orders"
      }
    }
  ]
}
```

---

### 🚀 VII. CHẠY APPLICATION ĐỂ TEST

#### **Step 1: Start Application**

```bash
cd e:\Smart_web\SmartF&B
mvn spring-boot:run
```

**Hoặc chạy JAR:**

```bash
cd target
java -jar smartfnb-1.0.0-SNAPSHOT.jar
```

**Chờ tới khi thấy:**

```
Started SmartFnbApplication in X seconds
Tomcat started on port(s) 8080 (http)
```

#### **Step 2: Ensure Database Ready**

```sql
-- PostgreSQL 16
CREATE DATABASE smartfnb;
CREATE USER smartfnb WITH PASSWORD 'smartfnb123';
GRANT ALL PRIVILEGES ON DATABASE smartfnb TO smartfnb;
```

#### **Step 3: Access Swagger**

```
http://localhost:8080/swagger-ui.html
```

**Nếu thấy "Whitelabel Error Page" → nghĩa là app chưa start hoàn toàn, chờ thêm 5 giây**

---

### 📊 VIII. TEST FLOW (THEO THỨ TỰ)

**Test Folder: `S10_Test_Scenarios.md`**

```
┌─────────────────────────────────────────────────────────────┐
│ SCENARIO 1: Single Order Flow                               │
├─────────────────────────────────────────────────────────────┤
│ 1. Login → Copy accessToken                                 │
│ 2. Select Branch → Copy branchToken                         │
│ 3. Create Order (POST /orders) → Copy orderId               │
│    ✅ Both WebSocket clients receive OrderCreatedEvent      │
│ 4. Get Order Details (GET /orders/{id})                     │
│ 5. Update Status → PROCESSING (PUT /orders/{id}/status)     │
│    ✅ Both WebSocket clients receive OrderStatusChangedEvent│
│ 6. Update Status → COMPLETED (PUT /orders/{id}/status)      │
│    ✅ Both WebSocket clients receive OrderCompletedEvent    │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ SCENARIO 2: Multiple Orders (Kitchen Load)                  │
├─────────────────────────────────────────────────────────────┤
│ 1. Create 5 orders rapidly                                  │
│    ✅ Kitchen screen receives all 5 OrderCreatedEvent      │
│ 2. List Orders (GET /orders) → See all 5                    │
│ 3. Update status of order #1                                │
│    ✅ Only order #1 status changes                          │
│ 4. Update status of order #2, #3                            │
│    ✅ Waiter screen see different items ready              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ SCENARIO 3: Cancel Order Flow                               │
├─────────────────────────────────────────────────────────────┤
│ 1. Create Order → PENDING                                   │
│ 2. Cancel Order (POST /orders/{id}/cancel)                  │
│    ✅ WebSocket clients receive OrderCancelledEvent         │
│ 3. Verify status = CANCELLED                                │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ SCENARIO 4: WebSocket Real-time Sync (2 Clients)           │
├─────────────────────────────────────────────────────────────┤
│ Browser Tab 1 (Kitchen):                                    │
│ - Open WebSocket console                                    │
│ - Subscribe /topic/orders/{branchId}                        │
│                                                              │
│ Browser Tab 2 (Swagger + Waiter):                           │
│ - Create order via Swagger                                  │
│                                                              │
│ Result:                                                      │
│ ✅ Tab 1 receives broadcast realtime (< 100ms)             │
│ ✅ Tab 2 also receives broadcast                            │
└─────────────────────────────────────────────────────────────┘
```

---

### 🔍 IX. TROUBLESHOOT

| Lỗi                         | Nguyên nhân                      | Fix                                                         |
| --------------------------- | -------------------------------- | ----------------------------------------------------------- |
| 401 Unauthorized            | Token không hợp lệ hoặc hết hạn  | Login lại, copy token mới                                   |
| 403 Forbidden               | Không có quyền với chi nhánh này | Chạy Select Branch step                                     |
| 404 Order Not Found         | OrderId không tồn tại            | Copy orderId chính xác từ Create order response             |
| WebSocket connection failed | Firewall chặn ws://              | Kiểm tra firewall, disable tạm thời                         |
| No broadcast received       | Topic branchId sai               | Verify `branchId` từ token match `/topic/orders/{branchId}` |

---

### 💾 X. CURL COMMAND (FULL FLOW)

```bash
#!/bin/bash

# 1. Login
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@smartfnb.vn","password":"Password123!"}')
TOKEN=$(echo $LOGIN_RESPONSE | jq -r '.data.accessToken')
echo "✅ Token: $TOKEN"

# 2. Select Branch
BRANCH_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/select-branch \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"branchId":"550e8400-e29b-41d4-a716-446655440002"}')
BRANCH_TOKEN=$(echo $BRANCH_RESPONSE | jq -r '.data.accessToken')
echo "✅ Branch Token: $BRANCH_TOKEN"

# 3. Create Order
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $BRANCH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tableId":"550e8400-e29b-41d4-a716-446655440003",
    "source":"IN_STORE",
    "notes":"Test",
    "items":[{
      "itemId":"550e8400-e29b-41d4-a716-446655440100",
      "itemName":"Cà phê",
      "quantity":2,
      "unitPrice":35000
    }]
  }')
ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.data.id')
echo "✅ Order ID: $ORDER_ID"

# 4. Update Status to PROCESSING
curl -s -X PUT http://localhost:8080/api/v1/orders/$ORDER_ID/status \
  -H "Authorization: Bearer $BRANCH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newStatus":"PROCESSING","reason":"Bếp đang làm"}' | jq '.data.status'
echo "✅ Updated to PROCESSING"

# 5. Update Status to COMPLETED
curl -s -X PUT http://localhost:8080/api/v1/orders/$ORDER_ID/status \
  -H "Authorization: Bearer $BRANCH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newStatus":"COMPLETED","reason":"Xong"}' | jq '.data.status'
echo "✅ Updated to COMPLETED"
```

---

### 📱 XI. REAL-TIME MONITORING TOOL

**Dùng `wscat` để monitor WebSocket realtime:**

```bash
# Install wscat
npm install -g wscat

# Connect to WebSocket
wscat -c ws://localhost:8080/ws

# Sau khi connect, type STOMP CONNECT frame:
CONNECT
accept-version:1.2
destination:/topic/orders/550e8400-e29b-41d4-a716-446655440002

# Bây giờ sẽ nhận tất cả broadcast từ channel này
# Mỗi khi có order update → sẽ thấy message
```

---

### ✨ XII. EXPECTED OUTPUTS

**1. Create Order → WebSocket:**

```json
{
  "id": "7a8b9c0d-e1f2-4a5b-8c9d-0e1f2a3b4c5d",
  "orderNumber": "ORD-550E-260331200000",
  "status": "PENDING",
  "totalAmount": 100000
}
```

**2. Change Status → WebSocket:**

```json
{
  "id": "7a8b9c0d-e1f2-4a5b-8c9d-0e1f2a3b4c5d",
  "orderNumber": "ORD-550E-260331200000",
  "status": "PROCESSING",
  "totalAmount": 100000
}
```

**3. Complete Order → WebSocket:**

```json
{
  "id": "7a8b9c0d-e1f2-4a5b-8c9d-0e1f2a3b4c5d",
  "orderNumber": "ORD-550E-260331200000",
  "status": "COMPLETED",
  "completedAt": "2026-03-31T20:25:00Z",
  "totalAmount": 100000
}
```

---

## 🎯 NEXT: QUÁ TRÌNH TEST CHI TIẾT

1. **Test endpoint nào trước?** → Chạy theo thứ tự ở section VIII
2. **Data nào dùng?** → Copy-paste từ section IV
3. **Có lỗi gì?** → Check section IX
4. **Muốn test WebSocket?** → Dùng curl + wscat hoặc browser console (section VI)

**Bạn hãy bắt đầu từ:**

```bash
1. Chạy app: mvn spring-boot:run
2. Mở Swagger: http://localhost:8080/swagger-ui.html
3. Test Login (section II)
4. Test Select Branch (section III)
5. Test Create Order (section V.1)
6. Mở 2 tab + WebSocket console (section VI)
7. Tạo order → Xem cả 2 tab nhận broadcast realtime ✅
```

Bạn cần help gì thêm không?
