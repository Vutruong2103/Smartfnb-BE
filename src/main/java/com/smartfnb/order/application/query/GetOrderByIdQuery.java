package com.smartfnb.order.application.query;

import java.util.UUID;

/**
 * Query lấy thông tin chi tiết đơn hàng theo ID.
 *
 * @author SmartF&B Team
 * @since 2026-03-31
 */
public record GetOrderByIdQuery(
    UUID orderId,
    UUID tenantId,
    UUID branchId
) {}
