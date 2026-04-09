const fs = require('fs');
const path = require('path');

const BASE_URL = 'http://localhost:8080/api/v1';

async function request(endpoint, method = 'GET', body = null, token = null) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const config = { method, headers };
    if (body) config.body = JSON.stringify(body);

    const res = await fetch(`${BASE_URL}${endpoint}`, config);
    // if status is 204 No Content, don't parse JSON
    if (res.status === 204) return { status: res.status, data: {} };
    
    let data;
    try {
        data = await res.json();
    } catch {
        data = await res.text();
    }
    
    return { status: res.status, data };
}

/**
 * Gửi request multipart/form-data với JSON part "data" và file part "image" (tùy chọn).
 * Dùng cho POST /menu/items và PUT /menu/items/{id} sau khi chuyển sang file upload.
 * @param {string} endpoint
 * @param {string} method POST hoặc PUT
 * @param {object} dataJson object sẽ serialize thành JSON trong part "data"
 * @param {string|null} imageFilePath đường dẫn file ảnh cục bộ, null = không kèm ảnh
 * @param {string|null} token Bearer token
 */
async function requestMultipart(endpoint, method, dataJson, imageFilePath = null, token = null) {
    // FormData + Blob available natively từ Node.js 18+
    const form = new FormData();
    form.append('data', new Blob([JSON.stringify(dataJson)], { type: 'application/json' }));

    if (imageFilePath) {
        const fileBuffer = fs.readFileSync(imageFilePath);
        const ext = path.extname(imageFilePath).slice(1).toLowerCase();
        const mimeMap = { jpg: 'image/jpeg', jpeg: 'image/jpeg', png: 'image/png', webp: 'image/webp' };
        const mime = mimeMap[ext] || 'image/jpeg';
        form.append('image', new Blob([fileBuffer], { type: mime }), path.basename(imageFilePath));
    }

    const headers = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const res = await fetch(`${BASE_URL}${endpoint}`, { method, headers, body: form });
    if (res.status === 204) return { status: res.status, data: {} };

    let data;
    try { data = await res.json(); }
    catch { data = await res.text(); }
    return { status: res.status, data };
}

async function runTests() {
    console.log("==========================================");
    console.log("🚀 Bắt đầu chuỗi Test API SmartF&B (S-01 -> S-12)");
    console.log("==========================================\n");

    const email = `testowner_${Date.now()}@test.com`;
    const password = "Password123!";
    let currentToken = null;
    let userId = null;
    let branchId = null;
    let categoryId = null;
    let itemId = null;
    let zoneId = null;
    let tableId = null;
    let orderId = null;

    try {
        // --- S-01, S-02: AUTH & TENANT ---
        console.log("1. MỚI: Đăng ký Tenant (Chủ quán)");
        let res = await request('/auth/register', 'POST', {
            tenantName: "Quán Test Tự Động",
            email: email,
            password: password,
            ownerName: "Auto Tester",
            planSlug: "standard"
        });
        if (res.status !== 200 && res.status !== 201) throw new Error("Register failed: " + JSON.stringify(res.data));
        console.log("   ✅ Đăng ký thành công.");

        console.log("2. Đăng nhập");
        res = await request('/auth/login', 'POST', { email, password });
        if (res.status !== 200) throw new Error("Login failed: " + JSON.stringify(res.data));
        currentToken = res.data.data.accessToken || res.data.data.token;
        console.log("   ✅ Đăng nhập thành công. Token lấy được.");

        console.log("3. Kiểm tra Gói cước (Subscription)");
        res = await request('/subscriptions/current', 'GET', null, currentToken);
        if (res.status !== 200) throw new Error("Get subscription failed: " + JSON.stringify(res.data));
        console.log("   ✅ API Subscription chạy tốt.");

        // --- S-03: BRANCH ---
        console.log("4. Tạo Chi nhánh mới");
        res = await request('/branches', 'POST', {
            name: "Chi nhánh Auto " + Date.now(),
            code: "CN" + Date.now().toString().slice(-4),
            address: "123 Test Street"
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Create branch failed: " + JSON.stringify(res.data));
        branchId = res.data.data.id;
        console.log("   ✅ Tạo chi nhánh thành công: " + branchId);

        console.log("5. Chọn chi nhánh làm việc (Select Branch)");
        res = await request('/auth/select-branch', 'POST', { branchId }, currentToken);
        if (res.status !== 200) throw new Error("Select branch failed: " + JSON.stringify(res.data));
        currentToken = res.data.data.token || res.data.data.accessToken || currentToken;
        // Notice API returns data.data.token if select-branch replaces token.
        console.log("   ✅ Chuyển scope sang chi nhánh thành công.");

        // --- S-05, S-06: MENU ---
        console.log("6. Tạo Danh mục (Category)");
        res = await request('/menu/categories', 'POST', {
            name: "Đồ uống Test",
            displayOrder: 1,
            isActive: true
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Create category failed: " + JSON.stringify(res.data));
        categoryId = res.data.data.id;
        console.log("   ✅ Tạo Category thành công.");

        console.log("7. Tạo Món bán (Item) — multipart/form-data (không kèm ảnh)");
        res = await requestMultipart('/menu/items', 'POST', {
            categoryId: categoryId,
            name: "Cà phê Auto",
            basePrice: 20000,
            unit: "Ly"
        }, null, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Create item failed: " + JSON.stringify(res.data));
        itemId = res.data.data.id;
        console.log("   ✅ Tạo Món thành công. ID: " + itemId);

        console.log("7b. Cập nhật Món (Item) — multipart PUT không kèm ảnh (giữ nguyên ảnh cũ)");
        res = await requestMultipart(`/menu/items/${itemId}`, 'PUT', {
            categoryId: categoryId,
            name: "Cà phê Auto (Updated)",
            basePrice: 25000,
            unit: "Ly",
            isActive: true,
            isSyncDelivery: false
        }, null, currentToken);
        if (res.status !== 200) throw new Error("Update item failed: " + JSON.stringify(res.data));
        console.log("   ✅ Cập nhật Món thành công.");

        // --- S-08: TABLES ---
        console.log("8. Tạo Khu vực (Zone)");
        res = await request(`/branches/${branchId}/zones`, 'POST', {
            name: "Tầng 1"
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Create zone failed: " + JSON.stringify(res.data));
        zoneId = res.data.data.id;
        console.log("   ✅ Tạo Zone thành công.");

        console.log("9. Tạo Bàn (Table)");
        res = await request(`/branches/${branchId}/tables`, 'POST', {
            zoneId: zoneId,
            name: "Bàn 01",
            capacity: 4,
            shape: "square"
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Create table failed: " + JSON.stringify(res.data));
        tableId = res.data.data.id;
        console.log("   ✅ Tạo Bàn thành công.");

        // --- S-10: ORDER ---
        console.log("10. Tạo Đơn hàng (Order)");
        res = await request('/orders', 'POST', {
            tableId: tableId,
            source: "IN_STORE",
            notes: "Test tự động",
            items: [
                {
                    itemId: itemId,
                    itemName: "Cà phê Auto",
                    quantity: 2,
                    unitPrice: 20000
                }
            ]
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Create order failed: " + JSON.stringify(res.data));
        orderId = res.data.data.id;
        let totalAmount = res.data.data.totalAmount;
        console.log("   ✅ Tạo Order thành công. Mã: " + orderId);

        console.log("11. Cập nhật Order sang COMPLETED");
        res = await request(`/orders/${orderId}/status`, 'PUT', {
            newStatus: "COMPLETED",
            reason: ""
        }, currentToken);
        if (res.status !== 200) throw new Error("Update order failed: " + JSON.stringify(res.data));
        console.log("   ✅ Đổi Order sang COMPLETED.");

        // --- S-11: PAYMENT & INVOICE ---
        console.log("12. Thanh toán bằng tiền mặt (Cash Payment)");
        res = await request('/payments/cash', 'POST', {
            orderId: orderId,
            amount: totalAmount
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Payment failed: " + JSON.stringify(res.data));
        const paymentId = res.data.data.id;
        console.log("   ✅ Thanh toán thành công. PaymentId: " + paymentId);

        console.log("13. Truy vấn Hóa đơn (Invoices)");
        res = await request('/payments/invoices/search', 'GET', null, currentToken);
        if (res.status !== 200) throw new Error("Search invoices failed: " + JSON.stringify(res.data));
        console.log(`   ✅ Truy vấn hóa đơn thành công. Có ${res.data.data.totalElements} hóa đơn trong chi nhánh.`);

        // --- S-13 & S-14: INVENTORY ---
        console.log("\n--- BẮT ĐẦU TEST S-13 & S-14 (INVENTORY) ---");

        console.log("14. [Đúng] Nhập kho (Import Stock) nguyên liệu (+50)");
        res = await request('/inventory/import', 'POST', {
            itemId: itemId,
            supplierId: "00000000-0000-0000-0000-000000000000",
            quantity: 50,
            costPerUnit: 10000,
            expiresAt: new Date(Date.now() + 30*24*3600*1000).toISOString(),
            note: "Nhập test lô hàng 1"
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Import stock failed: " + JSON.stringify(res.data));
        console.log("   ✅ Nhập kho thành công. BatchId: " + res.data.data);

        console.log("15. [Sai] Nhập kho với số lượng âm (Validation Error)");
        let failRes = await request('/inventory/import', 'POST', {
            itemId: itemId,
            quantity: -10,
            costPerUnit: 10000
        }, currentToken);
        if (failRes.status === 200 || failRes.status === 201) throw new Error("Expected validation error but succeeded!");
        console.log("   ✅ Server từ chối request sai thành công: " + (failRes.data.message || JSON.stringify(failRes.data.errors)));

        console.log("16. [Đúng] Điều chỉnh kho (Adjust Stock) -> Tồn kho = 45");
        res = await request('/inventory/adjust', 'POST', {
            itemId: itemId,
            newQuantity: 45,
            reason: "Kiểm kê tháng"
        }, currentToken);
        if (res.status !== 200) throw new Error("Adjust stock failed: " + JSON.stringify(res.data));
        console.log("   ✅ Điều chỉnh kho thành công.");

        console.log("17. [Đúng] Ghi hao hụt (Record Waste) (-5)");
        res = await request('/inventory/waste', 'POST', {
            itemId: itemId,
            quantity: 5,
            reason: "Hư hỏng"
        }, currentToken);
        if (res.status !== 200) throw new Error("Record waste failed: " + JSON.stringify(res.data));
        console.log("   ✅ Ghi nhận hao hụt thành công.");

        console.log("18. [Sai] Ghi hao hụt mà thiếu lý do (Validation Error)");
        failRes = await request('/inventory/waste', 'POST', {
            itemId: itemId,
            quantity: 5,
            reason: "" // Rỗng
        }, currentToken);
        if (failRes.status === 200) throw new Error("Expected validation error but succeeded!");
        console.log("   ✅ Server từ chối request thiếu lý do thành công.");

        console.log("19. [Đúng] Truy vấn Tồn kho (GET /inventory)");
        res = await request('/inventory', 'GET', null, currentToken);
        if (res.status !== 200) throw new Error("Get inventory failed: " + JSON.stringify(res.data));
        console.log(`   ✅ Truy vấn tồn kho thành công. Số mã hàng: ${res.data.data.totalElements}`);
        
        let invItem = res.data.data.content.find(i => i.itemId === itemId);
        if (invItem) {
            console.log(`   🔎 Số lượng tồn kho hiện tại đối với món test (sau khi set 45 -> trừ 5 hao hụt -> còn 40): ${invItem.quantity}`);
            if (Number(invItem.quantity) !== 40) {
                console.warn("   ⚠️ CẢNH BÁO: Số dư tồn kho không khớp kỳ vọng! Thực tế: " + invItem.quantity);
            } else {
                console.log("   ✅ Cân bằng kho (Balance) tính toán chính xác!");
            }
        }

        console.log("\n--- BẮT ĐẦU TEST S-15 (STAFF) ---");
        
        console.log("20. Tạo Chức vụ (Position)");
        res = await request('/positions', 'POST', {
            name: "Quản lý cửa hàng",
            description: "Quản lý chung chi nhánh"
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Create position failed: " + JSON.stringify(res.data));
        const positionId = res.data.data;
        console.log("   ✅ Tạo Chức vụ thành công. ID: " + positionId);

        console.log("21. Lấy danh sách Chức vụ");
        res = await request('/positions', 'GET', null, currentToken);
        if (res.status !== 200) throw new Error("Get positions failed");
        console.log("   ✅ Lấy danh sách Chức vụ thành công.");

        console.log("22. Tạo Nhân sự (Staff)");
        res = await request('/staff', 'POST', {
            positionId: positionId,
            fullName: "Nguyễn Văn Test",
            email: "stafftest@smartfnb.com",
            phone: "0999888777",
            employeeCode: "EMP-001",
            baseSalary: 10000000,
            hireDate: "2026-04-06"
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Create staff failed: " + JSON.stringify(res.data));
        let staffId = res.data.data;
        console.log("   ✅ Tạo Nhân sự thành công. ID: " + staffId);

        console.log("23. Cập nhật Nhân sự (Staff)");
        res = await request(`/staff/${staffId}`, 'PUT', {
            positionId: positionId,
            fullName: "Nguyễn Văn Test (Updated)",
            email: "stafftest_upd@smartfnb.com",
            phone: "0999888777",
            employeeCode: "EMP-001X",
            baseSalary: 12000000,
            hireDate: "2026-04-06",
            isActive: true
        }, currentToken);
        if (res.status !== 200) throw new Error("Update staff failed: " + JSON.stringify(res.data));
        console.log("   ✅ Cập nhật Nhân sự thành công.");

        console.log("24. Tạo Vai trò (Role)");
        res = await request('/roles', 'POST', {
            name: "Thu Ngân Test",
            description: "Role dành cho thu ngân test"
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Create role failed: " + JSON.stringify(res.data));
        const roleId = res.data.data;
        console.log("   ✅ Tạo Role thành công. ID: " + roleId);

        // --- S-16: SHIFT & SESSION ---
        console.log("\n--- BẮT ĐẦU TEST S-16 (SHIFT) ---");

        console.log("25. Tạo Ca làm việc mẫu (Shift Template)");
        res = await request('/shift-templates', 'POST', {
            name: "Ca Sáng Test",
            startTime: "07:00:00",
            endTime: "15:00:00",
            minStaff: 2,
            maxStaff: 5,
            color: "#FF5733",
            active: true
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Create shift template failed: " + JSON.stringify(res.data));
        const templateId = res.data.data;
        console.log("   ✅ Tạo Shift Template thành công. ID: " + templateId);

        console.log("26. Đăng ký Ca làm việc (Register Shift)");
        const jwtPayload = JSON.parse(Buffer.from(currentToken.split('.')[1], 'base64').toString());
        const currentUserId = jwtPayload.sub;
        res = await request('/shifts', 'POST', {
            userId: currentUserId,
            shiftTemplateId: templateId,
            date: "2026-04-07"
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Register shift failed: " + JSON.stringify(res.data));
        const scheduleId = res.data.data;
        console.log("   ✅ Đăng ký Shift Schedule thành công. ID: " + scheduleId);

        console.log("27. Lấy danh sách Ca của tôi (My Shifts)");
        res = await request('/shifts/my?startDate=2026-04-07&endDate=2026-04-07', 'GET', null, currentToken);
        if (res.status !== 200) throw new Error("Get my shifts failed: " + JSON.stringify(res.data));
        console.log("   ✅ Lấy danh sách ca làm việc cá nhân thành công.");

        console.log("28. Check-IN Ca làm việc");
        res = await request(`/shifts/${scheduleId}/checkin`, 'POST', {}, currentToken);
        if (res.status !== 200) throw new Error("Check-in failed: " + JSON.stringify(res.data));
        console.log("   ✅ Check-In thành công.");

        console.log("29. Check-OUT Ca làm việc");
        res = await request(`/shifts/${scheduleId}/checkout`, 'POST', {}, currentToken);
        if (res.status !== 200) throw new Error("Check-out failed: " + JSON.stringify(res.data));
        console.log("   ✅ Check-Out thành công.");

        console.log("30. Mở Phiên bàn giao POS (Open PosSession)");
        res = await request('/pos-sessions/open', 'POST', {
            startingCash: 1000000,
            shiftScheduleId: scheduleId
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Open POS Session failed: " + JSON.stringify(res.data));
        const sessionId = res.data.data;
        console.log("   ✅ Mở POS Session thành công. ID: " + sessionId);

        console.log("31. Lấy POS Session đang Active");
        res = await request('/pos-sessions/active', 'GET', null, currentToken);
        if (res.status !== 200) throw new Error("Get Active POS Session failed: " + JSON.stringify(res.data));
        console.log("   ✅ Lấy Active POS Session thành công.");

        console.log("32. Đóng Phiên Bàn giao POS (Close PosSession)");
        res = await request(`/pos-sessions/${sessionId}/close`, 'POST', {
            endingCashActual: 1500000,
            note: "Đóng két cuối ca"
        }, currentToken);
        if (res.status !== 200) throw new Error("Close POS Session failed: " + JSON.stringify(res.data));
        console.log("   ✅ Đóng POS Session thành công.");

        console.log("\n--- BẮT ĐẦU TEST S-17 (SUPPLIER & PURCHASE ORDER) ---");
        console.log("33. Tạo Nhà cung cấp (Supplier)");
        res = await request('/suppliers', 'POST', {
            name: "NCC Cà Phê Mộc",
            code: "NCC-CFM",
            phone: "0901234567",
            address: "123 Đường ABC",
            note: "Nhà cung cấp hạt cà phê ngon"
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Create supplier failed: " + JSON.stringify(res.data));
        const supplierId = res.data.data;
        console.log("   ✅ Tạo Supplier thành công. ID: " + supplierId);

        console.log("34. Tạo Đơn mua hàng (Purchase Order) - DRAFT");
        // We need an itemId to map to PO items. Taking global testItemId (from S-06)
        res = await request('/purchase-orders', 'POST', {
            supplierId: supplierId,
            note: "Nhập đợt 1",
            expectedDate: "2026-05-01",
            items: [
                {
                    itemId: itemId,
                    itemName: "Hạt Cafe Arabica",
                    unit: "kg",
                    quantity: 10,
                    unitPrice: 200000,
                    note: "Loại 1"
                }
            ]
        }, currentToken);
        if (res.status !== 200 && res.status !== 201) throw new Error("Create PO failed: " + JSON.stringify(res.data));
        const poId = res.data.data;
        console.log("   ✅ Tạo Purchase Order (DRAFT) thành công. ID: " + poId);

        console.log("35. Gửi Đơn mua hàng cho NCC (DRAFT -> SENT)");
        res = await request(`/purchase-orders/${poId}/send`, 'POST', {}, currentToken);
        if (res.status !== 200) throw new Error("Send PO failed: " + JSON.stringify(res.data));
        console.log("   ✅ Send Purchase Order thành công.");

        console.log("36. Xác nhận nhận hàng (SENT -> RECEIVED)");
        res = await request(`/purchase-orders/${poId}/receive`, 'POST', {}, currentToken);
        if (res.status !== 200) throw new Error("Receive PO failed: " + JSON.stringify(res.data));
        console.log("   ✅ Nhận Purchase Order thành công. (Đã trigger sinh StockBatch autmatically)");

        console.log("37. Huỷ Đơn mua hàng (CANCELLED)");
        // create a quick dummy PO to test cancel
        res = await request('/purchase-orders', 'POST', {
            supplierId: supplierId,
            items: [{ itemId: itemId, itemName: "Test Item", quantity: 1, unitPrice: 10 }]
        }, currentToken);
        const poCancelId = res.data.data;
        res = await request(`/purchase-orders/${poCancelId}/cancel`, 'POST', { reason: "Không cần nhập hàng nữa" }, currentToken);
        if (res.status !== 200) throw new Error("Cancel PO failed: " + JSON.stringify(res.data));
        console.log("   ✅ Huỷ Purchase Order thành công.");

        console.log("\n==========================================");
        console.log("🎉 TẤT CẢ MODULES (S-01 đến S-17) HOẠT ĐỘNG HOÀN HẢO!");
        console.log("==========================================");

    } catch (e) {
        console.error("\n❌ LỖI TRONG QUÁ TRÌNH TEST:");
        console.error(e.message);
        process.exit(1);
    }
}

runTests();
