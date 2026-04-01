package com.smartfnb.order.application.query;

import java.time.Instant;
import java.util.UUID;

/**
 * Query lấy danh sách đơn hàng theo bộ lọc.
 *
 * @author SmartF&B Team
 * @since 2026-03-31
 */
public record GetOrderListQuery(
    UUID branchId,
    String status,
    Instant from,
    Instant to,
    UUID tableId,
    int page,
    int size
) {}
