package com.smartfnb.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình Spring MVC cho SmartF&B.
 * Map /api/v1/files/** → local filesystem để phục vụ ảnh đã upload.
 *
 * <p>Khi dùng Docker, upload-dir được mount qua named volume nên file
 * sẽ persist qua các lần restart container.</p>
 *
 * @author SmartF&B Team
 * @since 2026-04-09
 */
@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    /**
     * Map URL /api/v1/files/** sang thư mục upload trên filesystem.
     * Endpoint này public (không cần JWT) — cấu hình trong SecurityConfig.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Chuẩn hóa path: đảm bảo kết thúc bằng "/"
        String location = "file:" + uploadDir.replace("\\", "/");
        if (!location.endsWith("/")) {
            location += "/";
        }

        log.info("Đăng ký file handler: /api/v1/files/** → {}", location);

        registry.addResourceHandler("/api/v1/files/**")
                .addResourceLocations(location)
                .setCachePeriod(3600); // Cache 1 giờ ở client
    }
}
