package com.smartfnb.menu.application.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO request cập nhật món ăn.
 * imageUrl không nhận từ đây — nhận qua @RequestPart("image") MultipartFile.
 * Nếu không upload ảnh mới thì giữ nguyên ảnh cũ.
 *
 * @author SmartF&B Team
 * @since 2026-03-28
 */
public record UpdateMenuItemRequest(

        /** ID danh mục mới — null để bỏ khỏi danh mục */
        UUID categoryId,

        /** Tên món ăn mới */
        @NotBlank(message = "Tên món ăn không được để trống")
        @Size(max = 255, message = "Tên món ăn tối đa 255 ký tự")
        String name,

        /** Giá bán mới */
        @NotNull(message = "Giá bán không được để trống")
        @DecimalMin(value = "0", message = "Giá bán không được âm")
        BigDecimal basePrice,

        /** Đơn vị tính mới */
        @Size(max = 30, message = "Đơn vị tính tối đa 30 ký tự")
        String unit,

        /** Trạng thái kích hoạt */
        Boolean isActive,

        /** Đồng bộ lên app giao hàng */
        Boolean isSyncDelivery
) {}
