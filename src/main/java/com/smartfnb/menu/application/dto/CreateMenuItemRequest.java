package com.smartfnb.menu.application.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO request tạo món ăn mới trong thực đơn.
 * tenantId không nhận từ client — lấy từ JWT qua TenantContext.
 * imageUrl không nhận từ đây — nhận qua @RequestPart("image") MultipartFile.
 *
 * @author SmartF&B Team
 * @since 2026-03-28
 */
public record CreateMenuItemRequest(

        /** ID danh mục — có thể null */
        UUID categoryId,

        /** Tên món ăn — unique trong tenant */
        @NotBlank(message = "Tên món ăn không được để trống")
        @Size(max = 255, message = "Tên món ăn tối đa 255 ký tự")
        String name,

        /** Giá bán mặc định — phải >= 0 */
        @NotNull(message = "Giá bán không được để trống")
        @DecimalMin(value = "0", message = "Giá bán không được âm")
        BigDecimal basePrice,

        /** Đơn vị tính */
        @Size(max = 30, message = "Đơn vị tính tối đa 30 ký tự")
        String unit,

        /** Đồng bộ lên app giao hàng — mặc định false */
        Boolean isSyncDelivery
) {}
