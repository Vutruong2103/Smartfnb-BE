package com.smartfnb.order.application.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Kết quả tóm tắt đơn hàng dùng cho danh sách.
 *
 * @author SmartF&B Team
 * @since 2026-03-31
 */
public record OrderListResult(
    UUID id,
    String orderNumber,
    UUID tableId,
    String tableName,
    String status,
    BigDecimal totalAmount,
    Instant createdAt,
    String staffName
) {}
