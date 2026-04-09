package com.smartfnb.shared.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction layer cho File Storage.
 * Cho phép swap giữa Local / S3 / MinIO mà không cần sửa code nghiệp vụ.
 *
 * <p>Quy tắc sử dụng:</p>
 * <ul>
 *   <li>{@code store()} — lưu file và trả về URL công khai có thể truy cập</li>
 *   <li>{@code delete()} — xóa file theo URL đã lưu (dùng khi update ảnh cũ)</li>
 * </ul>
 *
 * @author SmartF&B Team
 * @since 2026-04-09
 */
public interface FileStorageService {

    /**
     * Lưu file vào storage và trả về URL có thể truy cập công khai.
     *
     * @param file file cần lưu (từ multipart/form-data)
     * @return URL đầy đủ để truy cập file (VD: http://localhost:8080/api/v1/files/uuid.jpg)
     * @throws com.smartfnb.shared.exception.InvalidImageException nếu file không hợp lệ
     */
    String store(MultipartFile file);

    /**
     * Xóa file theo URL đã lưu.
     * Không throw exception nếu file không tồn tại (idempotent).
     *
     * @param fileUrl URL của file cần xóa (có thể null — sẽ bỏ qua)
     */
    void delete(String fileUrl);
}
