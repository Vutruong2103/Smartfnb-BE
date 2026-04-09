package com.smartfnb.shared.web;

import com.smartfnb.shared.exception.SmartFnbException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * Xử lý tập trung tất cả exception trong hệ thống SmartF&B.
 * Đảm bảo mọi lỗi đều trả về format ApiResponse chuẩn.
 *
 * <p>Thứ tự xử lý:</p>
 * <ol>
 *   <li>SmartFnbException — lỗi nghiệp vụ với errorCode</li>
 *   <li>MethodArgumentNotValidException — lỗi validation Bean Validation</li>
 *   <li>AccessDeniedException — lỗi phân quyền Spring Security</li>
 *   <li>Exception — lỗi không mong đợi</li>
 * </ol>
 *
 * @author SmartF&B Team
 * @since 2026-03-26
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý lỗi nghiệp vụ SmartFnbException.
     * HTTP status được lấy từ exception (thường là 400, 404, 409).
     *
     * @param ex exception nghiệp vụ
     * @return ResponseEntity với ApiResponse lỗi
     */
    @ExceptionHandler(SmartFnbException.class)
    public ResponseEntity<ApiResponse<Void>> handleSmartFnbException(SmartFnbException ex) {
        log.warn("Lỗi nghiệp vụ [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
    }

    /**
     * Xử lý lỗi validation từ @Valid / @Validated.
     * Gộp tất cả field errors thành một thông điệp dễ đọc.
     *
     * @param ex lỗi validation input
     * @return ResponseEntity 400 với danh sách lỗi
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {

        // Gộp tất cả field validation errors
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Lỗi validation: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("VALIDATION_ERROR", message));
    }

    /**
     * Xử lý lỗi phân quyền — người dùng không có quyền truy cập tài nguyên.
     *
     * @param ex lỗi truy cập bị từ chối
     * @return ResponseEntity 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Truy cập bị từ chối: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail("ACCESS_DENIED",
                        "Bạn không có quyền thực hiện thao tác này"));
    }

    /**
     * Xử lý lỗi file upload vượt quá kích thước giới hạn.
     * Spring tự throw khi file > spring.servlet.multipart.max-file-size
     *
     * @param ex exception khi file quá lớn
     * @return ResponseEntity 400 với mã lỗi FILE_TOO_LARGE
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex) {
        log.warn("File upload vượt kích thước cho phép: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail("FILE_TOO_LARGE",
                        "Ảnh không được vượt quá 5MB"));
    }

    /**
     * Xử lý mọi exception không mong đợi — log đầy đủ stack trace.
     *
     * @param ex exception không xác định
     * @return ResponseEntity 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Lỗi không mong đợi: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("INTERNAL_SERVER_ERROR",
                        "Hệ thống gặp sự cố, vui lòng thử lại sau"));
    }
}
