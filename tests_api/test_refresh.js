const BASE_URL = 'http://localhost:8080/api/v1';

async function request(endpoint, method = 'GET', body = null, token = null) {
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const config = { method, headers };
    if (body) config.body = JSON.stringify(body);

    const res = await fetch(`${BASE_URL}${endpoint}`, config);
    let data;
    try { data = await res.json(); } catch { data = await res.text(); }
    return { status: res.status, data };
}

function parseJwt(token) {
    try {
        return JSON.parse(Buffer.from(token.split('.')[1], 'base64').toString());
    } catch (e) {
        return null;
    }
}

async function testRefresh() {
    console.log("1. Đăng nhập để lấy tokens...");
    const email = "admin@smartfnb.com"; // Assuming default admin from seed or use test registration
    const password = "password"; 

    // Mất test runner có tạo tài khoản testowner, ta có thể tự tạo lại 1 cái.
    let res = await request('/auth/register', 'POST', {
        tenantName: "Refresh Test Tenant",
        email: "test_refresh@smartfnb.com",
        password: "Password123!",
        ownerName: "Tester",
        planSlug: "standard"
    });

    res = await request('/auth/login', 'POST', {
        email: "test_refresh@smartfnb.com",
        password: "Password123!"
    });

    if (res.status !== 200) {
        console.error("Login thất bại", res.data);
        return;
    }

    const { accessToken, refreshToken, role: loginRole } = res.data.data;
    const loginPayload = parseJwt(accessToken);
    console.log(`\nLogin thành công. Báo cáo Token gốc:`);
    console.log(`- Trả về DTO Role: ${loginRole}`);
    console.log(`- Trong JWT Role: ${loginPayload.role}`);
    console.log(`- Trong JWT Permissions length: ${loginPayload.permissions?.length || 0}`);

    console.log("\n2. Gọi API Refresh Token...");
    const refreshRes = await request('/auth/refresh', 'POST', {
        refreshToken: refreshToken
    });

    if (refreshRes.status !== 200) {
        console.error("Refresh thất bại", refreshRes.data);
        return;
    }

    const { accessToken: newAccessToken, role: newRole } = refreshRes.data.data;
    const newPayload = parseJwt(newAccessToken);

    console.log(`\nRefresh thành công. Báo cáo Token mới:`);
    console.log(`- Trả về DTO Role: ${newRole}`);
    console.log(`- Trong JWT Role: ${newPayload.role}`);
    console.log(`- Trong JWT Permissions length: ${newPayload.permissions?.length || 0}`);

    if (newRole === loginRole && newPayload.permissions?.length > 0) {
        console.log("\n✅ KIỂM TRA THÀNH CÔNG: Access Token mới sau khi refresh ĐÃ GIỮ LẠI toàn bộ Role và Permissions!");
    } else {
        console.log("\n❌ CẢNH BÁO: Access Token mới SAU KHI REFRESH vẫn KHÔNG KHỚP / CHƯA ĐƯỢC FIX HOÀN TOÀN!");
    }
}

testRefresh();
