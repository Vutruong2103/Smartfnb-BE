package com.smartfnb.shared.storage;

import com.smartfnb.shared.exception.InvalidImageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Implement FileStorageService lưu file vào local filesystem.
 *
 * <p>Cấu hình trong application.yml:</p>
 * <pre>
 *   app:
 *     storage:
 *       upload-dir: ./uploads          # Thư mục lưu file (mount volume trong Docker)
 *       base-url: http://localhost:8080 # Base URL để tạo link trả về
 * </pre>
 *
 * <p>Có thể swap sang S3FileStorageService bằng cách implement FileStorageService
 * và đánh dấu bean phù hợp với @Primary hoặc @ConditionalOnProperty.</p>
 *
 * @author SmartF&B Team
 * @since 2026-04-09
 */
@Slf4j
@Service
public class LocalFileStorageService implements FileStorageService {

    /** Các MIME type được chấp nhận */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    /** Kích thước tối đa: 5MB */
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L;

    /** Prefix URL path để phục vụ file */
    private static final String FILE_URL_PATH = "/api/v1/files/";

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    @Value("${app.storage.base-url}")
    private String baseUrl;

    /** Đường dẫn tuyệt đối tới thư mục lưu file */
    private Path uploadPath;

    /**
     * Khởi tạo thư mục upload khi bean được tạo.
     * Tạo thư mục nếu chưa tồn tại.
     */
    @PostConstruct
    public void init() {
        uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            log.info("File storage khởi tạo tại: {}", uploadPath);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Không thể tạo thư mục upload: " + uploadPath, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Quy trình:</p>
     * <ol>
     *   <li>Validate MIME type (chỉ JPEG, PNG, WebP)</li>
     *   <li>Validate kích thước ≤ 5MB</li>
     *   <li>Tạo tên file UUID để tránh trùng lặp và path traversal</li>
     *   <li>Lưu file</li>
     *   <li>Trả về URL đầy đủ</li>
     * </ol>
     */
    @Override
    public String store(MultipartFile file) {
        validateFile(file);

        String extension = getExtension(file.getContentType());
        String filename = UUID.randomUUID() + "." + extension;
        Path targetPath = uploadPath.resolve(filename);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Đã lưu ảnh: {} ({} bytes)", filename, file.getSize());
        } catch (IOException e) {
            throw new InvalidImageException("Không thể lưu file ảnh, vui lòng thử lại");
        }

        return buildUrl(filename);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Trích xuất tên file từ URL rồi xóa khỏi filesystem.
     * Không throw exception nếu file đã bị xóa trước đó.</p>
     */
    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        // Trích xuất tên file từ URL: http://...../api/v1/files/{filename}
        int idx = fileUrl.lastIndexOf(FILE_URL_PATH);
        if (idx < 0) {
            log.warn("URL ảnh không đúng định dạng, bỏ qua xóa: {}", fileUrl);
            return;
        }

        String filename = fileUrl.substring(idx + FILE_URL_PATH.length());

        // Bảo vệ path traversal: filename không được chứa '/' hoặc '..'
        if (filename.contains("/") || filename.contains("..")) {
            log.warn("Phát hiện path traversal trong filename: {}", filename);
            return;
        }

        Path filePath = uploadPath.resolve(filename).normalize();

        // Đảm bảo file nằm trong uploadPath (double-check path traversal)
        if (!filePath.startsWith(uploadPath)) {
            log.warn("File nằm ngoài thư mục upload: {}", filePath);
            return;
        }

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Đã xóa ảnh cũ: {}", filename);
            }
        } catch (IOException e) {
            // Log nhưng không throw — không để lỗi xóa file cũ block nghiệp vụ
            log.warn("Không thể xóa ảnh cũ {}: {}", filename, e.getMessage());
        }
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("File ảnh không được để trống");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidImageException(
                    "Chỉ chấp nhận ảnh định dạng JPEG, PNG hoặc WebP");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidImageException("Ảnh không được vượt quá 5MB");
        }
    }

    private String getExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "jpg"; // fallback (đã validate trước)
        };
    }

    private String buildUrl(String filename) {
        // Đảm bảo không có double slash
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + FILE_URL_PATH + filename;
    }
}
