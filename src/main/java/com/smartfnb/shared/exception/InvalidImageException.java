package com.smartfnb.shared.exception;

/**
 * Exception khi file ảnh upload không hợp lệ.
 * Ném khi: định dạng không phải ảnh, hoặc vượt quá kích thước cho phép.
 *
 * @author SmartF&B Team
 * @since 2026-04-09
 */
public class InvalidImageException extends SmartFnbException {

    private static final String ERROR_CODE = "INVALID_IMAGE";

    /**
     * @param message mô tả lỗi chi tiết (VD: "Chỉ chấp nhận ảnh JPEG, PNG, WebP")
     */
    public InvalidImageException(String message) {
        super(ERROR_CODE, message, 400);
    }
}
