## ⚡ S-10 ORDER QUICK REFERENCE — TEST DATA & CURL COMMANDS

---

## 📌 TEST DATA (Copy-Paste)

### ✅ UUIDs (Dùng được ngay lập tức)

```
Tenant ID:          550e8400-e29b-41d4-a716-446655440001
Branch ID:          550e8400-e29b-41d4-a716-446655440002
Table ID 1:         550e8400-e29b-41d4-a716-446655440003
Table ID 2:         550e8400-e29b-41d4-a716-446655440004
Staff ID:           550e8400-e29b-41d4-a716-446655440005
Item ID (Cà phê):   550e8400-e29b-41d4-a716-446655440100
Item ID (Trà sữa):  550e8400-e29b-41d4-a716-446655440101
```

### 👤 Account Test

```
Email:    owner@smartfnb.vn
Password: Password123!
```

---

## 🔗 API ENDPOINTS

```
POST   /api/v1/auth/login                    → Get accessToken
POST   /api/v1/auth/select-branch            → Get branchToken (with branchId)
POST   /api/v1/orders                        → Create order
GET    /api/v1/orders                        → List orders (paginated)
GET    /api/v1/orders/{id}                   → Get order detail
PUT    /api/v1/orders/{id}/status            → Update status
POST   /api/v1/orders/{id}/cancel            → Cancel order
```

---

## 🌐 BASE URL

```
http://localhost:8080
```

---

## 📋 REQUEST TEMPLATES (Copy-Paste)

### 1️⃣ LOGIN

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "owner@smartfnb.vn",
    "password": "Password123!"
  }' | jq '.data.accessToken'
```

**Store value:** `accessToken`

---

### 2️⃣ SELECT BRANCH

```bash
curl -X POST http://localhost:8080/api/v1/auth/select-branch \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "branchId": "550e8400-e29b-41d4-a716-446655440002"
  }' | jq '.data.accessToken'
```

**Store value:** `branchToken` (replace YOUR_ACCESS_TOKEN with token từ step 1)

---

### 3️⃣ CREATE ORDER (Main test — triggers WebSocket broadcast)

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer YOUR_BRANCH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tableId": "550e8400-e29b-41d4-a716-446655440003",
    "source": "IN_STORE",
    "notes": "Test S-10 Realtime WebSocket",
    "items": [
      {
        "itemId": "550e8400-e29b-41d4-a716-446655440100",
        "itemName": "Cà phê Sữa Đá",
        "quantity": 2,
        "unitPrice": 35000,
        "addons": "Trân châu đen",
        "notes": "Ít dá"
      },
      {
        "itemId": "550e8400-e29b-41d4-a716-446655440101",
        "itemName": "Trà Sữa",
        "quantity": 1,
        "unitPrice": 30000,
        "addons": "Trân châu trắng",
        "notes": null
      }
    ]
  }' | jq '.data.id'
```

**Store value:** `orderId`

**🔔 WebSocket Event:** `OrderCreatedEvent` broadcast to `/topic/orders/550e8400-e29b-41d4-a716-446655440002`

---

### 4️⃣ GET ORDER DETAIL

```bash
curl -X GET "http://localhost:8080/api/v1/orders/YOUR_ORDER_ID" \
  -H "Authorization: Bearer YOUR_BRANCH_TOKEN" | jq '.'
```

---

### 5️⃣ LIST ORDERS

```bash
curl -X GET "http://localhost:8080/api/v1/orders?status=PENDING&page=0&size=20" \
  -H "Authorization: Bearer YOUR_BRANCH_TOKEN" | jq '.data.content'
```

---

### 6️⃣ UPDATE STATUS → PROCESSING

```bash
curl -X PUT "http://localhost:8080/api/v1/orders/YOUR_ORDER_ID/status" \
  -H "Authorization: Bearer YOUR_BRANCH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "PROCESSING",
    "reason": "Bếp đang pha chế"
  }' | jq '.data.status'
```

**🔔 WebSocket Event:** `OrderStatusChangedEvent` broadcast (status: PENDING → PROCESSING)

---

### 7️⃣ UPDATE STATUS → COMPLETED

```bash
curl -X PUT "http://localhost:8080/api/v1/orders/YOUR_ORDER_ID/status" \
  -H "Authorization: Bearer YOUR_BRANCH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newStatus": "COMPLETED",
    "reason": "Đã pha chế xong"
  }' | jq '.data.status'
```

**🔔 WebSocket Event:** `OrderCompletedEvent` broadcast (status: PROCESSING → COMPLETED)

---

### 8️⃣ CANCEL ORDER

```bash
curl -X POST "http://localhost:8080/api/v1/orders/YOUR_ORDER_ID/cancel" \
  -H "Authorization: Bearer YOUR_BRANCH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Khách không muốn"
  }' | jq '.data.status'
```

**🔔 WebSocket Event:** `OrderCancelledEvent` broadcast (status → CANCELLED)

---

## 📊 EXPECTED JSON RESPONSES

### Create Order Response

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
    "notes": "Test S-10 Realtime WebSocket",
    "completedAt": null,
    "items": [
      {
        "id": "item-1",
        "itemId": "550e8400-e29b-41d4-a716-446655440100",
        "itemName": "Cà phê Sữa Đá",
        "quantity": 2,
        "unitPrice": 35000,
        "totalPrice": 70000,
        "addons": ["Trân châu đen"],
        "notes": "Ít dá",
        "status": "PENDING"
      },
      {
        "id": "item-2",
        "itemId": "550e8400-e29b-41d4-a716-446655440101",
        "itemName": "Trà Sữa",
        "quantity": 1,
        "unitPrice": 30000,
        "totalPrice": 30000,
        "addons": ["Trân châu trắng"],
        "notes": null,
        "status": "PENDING"
      }
    ]
  },
  "error": null,
  "timestamp": "2026-03-31T20:15:00Z"
}
```

---

## 🌐 WEBSOCKET MONITORING (wscat)

### Install

```bash
npm install -g wscat
```

### Connect & Subscribe

```bash
wscat -c ws://localhost:8080/ws

# After connected, type STOMP CONNECT:
CONNECT
accept-version:1.2

# Subscribe to order topic:
SUBSCRIBE
id:sub-1
destination:/topic/orders/550e8400-e29b-41d4-a716-446655440002

# Now wait for messages...
# Every order change → will see broadcast message
```

---

## 🚀 FULL TEST SEQUENCE (COPY-PASTE ALL)

Save as `test_s10.sh`:

```bash
#!/bin/bash
set -e

API="http://localhost:8080"

# Color codes
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}========== S-10 Order Realtime Test ==========${NC}\n"

# 1. LOGIN
echo -e "${YELLOW}[1] Login...${NC}"
LOGIN=$(curl -s -X POST $API/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@smartfnb.vn","password":"Password123!"}')
TOKEN=$(echo $LOGIN | jq -r '.data.accessToken')
echo -e "${GREEN}✅ Token: ${TOKEN:0:20}...${NC}\n"

# 2. SELECT BRANCH
echo -e "${YELLOW}[2] Select Branch...${NC}"
BRANCH=$(curl -s -X POST $API/api/v1/auth/select-branch \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"branchId":"550e8400-e29b-41d4-a716-446655440002"}')
BRANCH_TOKEN=$(echo $BRANCH | jq -r '.data.accessToken')
echo -e "${GREEN}✅ Branch Token: ${BRANCH_TOKEN:0:20}...${NC}\n"

# 3. CREATE ORDER
echo -e "${YELLOW}[3] Create Order...${NC}"
ORDER=$(curl -s -X POST $API/api/v1/orders \
  -H "Authorization: Bearer $BRANCH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tableId":"550e8400-e29b-41d4-a716-446655440003",
    "source":"IN_STORE",
    "notes":"Test S-10",
    "items":[{
      "itemId":"550e8400-e29b-41d4-a716-446655440100",
      "itemName":"Cà phê",
      "quantity":2,
      "unitPrice":35000
    }]
  }')
ORDER_ID=$(echo $ORDER | jq -r '.data.id')
ORDER_NUM=$(echo $ORDER | jq -r '.data.orderNumber')
echo -e "${GREEN}✅ Order created: $ORDER_NUM (ID: ${ORDER_ID:0:8}...)${NC}"
echo -e "${BLUE}📡 WebSocket Broadcast: OrderCreatedEvent sent!${NC}\n"

# 4. GET ORDER DETAIL
echo -e "${YELLOW}[4] Get Order Detail...${NC}"
GET_ORDER=$(curl -s -X GET $API/api/v1/orders/$ORDER_ID \
  -H "Authorization: Bearer $BRANCH_TOKEN")
STATUS=$(echo $GET_ORDER | jq -r '.data.status')
echo -e "${GREEN}✅ Status: $STATUS${NC}\n"

# 5. UPDATE TO PROCESSING
echo -e "${YELLOW}[5] Update Status → PROCESSING...${NC}"
UPDATE1=$(curl -s -X PUT $API/api/v1/orders/$ORDER_ID/status \
  -H "Authorization: Bearer $BRANCH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newStatus":"PROCESSING","reason":"Bếp đang làm"}')
STATUS=$(echo $UPDATE1 | jq -r '.data.status')
echo -e "${GREEN}✅ Status: $STATUS${NC}"
echo -e "${BLUE}📡 WebSocket Broadcast: OrderStatusChangedEvent sent!${NC}\n"

# 6. UPDATE TO COMPLETED
echo -e "${YELLOW}[6] Update Status → COMPLETED...${NC}"
UPDATE2=$(curl -s -X PUT $API/api/v1/orders/$ORDER_ID/status \
  -H "Authorization: Bearer $BRANCH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"newStatus":"COMPLETED","reason":"Xong"}')
STATUS=$(echo $UPDATE2 | jq -r '.data.status')
COMPLETED_AT=$(echo $UPDATE2 | jq -r '.data.completedAt')
echo -e "${GREEN}✅ Status: $STATUS${NC}"
echo -e "${GREEN}✅ Completed at: $COMPLETED_AT${NC}"
echo -e "${BLUE}📡 WebSocket Broadcast: OrderCompletedEvent sent!${NC}\n"

# 7. LIST ORDERS
echo -e "${YELLOW}[7] List Orders...${NC}"
LIST=$(curl -s -X GET "$API/api/v1/orders?status=COMPLETED&page=0&size=20" \
  -H "Authorization: Bearer $BRANCH_TOKEN")
COUNT=$(echo $LIST | jq '.data.content | length')
echo -e "${GREEN}✅ Total COMPLETED orders: $COUNT${NC}\n"

echo -e "${BLUE}========== ✅ ALL TESTS PASSED ==========${NC}"
echo -e "${GREEN}📊 Summary:${NC}"
echo -e "  ✅ Login successful"
echo -e "  ✅ Branch selected"
echo -e "  ✅ Order created (OrderCreatedEvent broadcast)"
echo -e "  ✅ Status updated to PROCESSING (OrderStatusChangedEvent broadcast)"
echo -e "  ✅ Status updated to COMPLETED (OrderCompletedEvent broadcast)"
echo -e "  ✅ Orders listed\n"
echo -e "${YELLOW}🌐 WebSocket Check:${NC}"
echo -e "  Open 2 browser tabs + subscribe /topic/orders/550e8400-e29b-41d4-a716-446655440002"
echo -e "  Run this script → Both tabs will receive realtime broadcasts! 🚀"
```

**Run:**

```bash
chmod +x test_s10.sh
./test_s10.sh
```

---

## 🔍 SWAGGER URL

```
http://localhost:8080/swagger-ui.html
```

**In Swagger UI:**

1. Click "Authorize" button (top-right)
2. Paste accessToken
3. Try all Order endpoints

---

## 📊 QUICK STATUS REFERENCE

```
PENDING     → Order tạo xong, chờ làm
PROCESSING  → Bếp đang pha chế
COMPLETED   → Hoàn thành, ready thanh toán
CANCELLED   → Bị hủy
```

---

## ✅ VALIDATION CHECKLIST

- [ ] Application running on http://localhost:8080
- [ ] PostgreSQL database ready
- [ ] Swagger accessible: http://localhost:8080/swagger-ui.html
- [ ] Login successful → get accessToken
- [ ] Select branch → get branchToken
- [ ] Create order → receive orderId
- [ ] Order status PENDING
- [ ] Update to PROCESSING → WebSocket broadcasts
- [ ] Update to COMPLETED → WebSocket broadcasts
- [ ] List orders shows created order
- [ ] Cancel order → status = CANCELLED
- [ ] 2 WebSocket clients both receive broadcasts simultaneously

---

## 🆘 QUICK TROUBLESHOOT

| Error                 | Fix                                                                    |
| --------------------- | ---------------------------------------------------------------------- |
| 401                   | Token expired, re-login                                                |
| 403                   | Need to select branch first                                            |
| 404                   | Order ID wrong                                                         |
| Connection refused    | App not running, run: `mvn spring-boot:run`                            |
| No WebSocket messages | Check topic branchId, should be `550e8400-e29b-41d4-a716-446655440002` |

---

## 💡 TIPS

✅ **Save tokens in env variables:**

```bash
export TOKEN="eyJhbGci..."
export BRANCH_TOKEN="eyJhbGci..."
```

Then use in curl:

```bash
curl -H "Authorization: Bearer $BRANCH_TOKEN" ...
```

✅ **Pretty-print JSON:**

```bash
curl ... | jq '.'
```

✅ **Extract specific field:**

```bash
curl ... | jq '.data.status'
```

---

## 📞 NEED HELP?

Check full guide: `docs/S10_TESTING_GUIDE.md`
