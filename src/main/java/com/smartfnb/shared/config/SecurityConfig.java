package com.smartfnb.shared.config;

import com.smartfnb.auth.infrastructure.jwt.JwtAuthFilter;
import com.smartfnb.rbac.security.CustomPermissionEvaluator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.Serializable;

/**
 * Cấu hình Spring Security 6 cho SmartF&B.
 * Sử dụng JWT stateless — không có session.
 * Bật @PreAuthorize và @PostAuthorize cho phân quyền chi tiết.
 *
 * @author SmartF&B Team
 * @since 2026-03-26
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomPermissionEvaluator customPermissionEvaluator;

    /**
     * Chuỗi filter bảo mật chính.
     * Public endpoints: /api/v1/auth/**, /swagger-ui/**, /api-docs/**, /actuator/health
     * Các endpoints còn lại yêu cầu xác thực JWT.
     *
     * @param http HttpSecurity builder
     * @return SecurityFilterChain được cấu hình
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF — API stateless không cần CSRF protection
            .csrf(AbstractHttpConfigurer::disable)

            // Session stateless — JWT token thay thế session
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Cấu hình phân quyền URL
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints — public
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/forgot-password",
                    "/api/v1/auth/reset-password"
                ).permitAll()

                // Swagger UI — public (dev/staging)
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // Actuator health — public
                .requestMatchers("/actuator/health").permitAll()

                // File uploads (ảnh món ăn) — public, không cần JWT để hiển thị trong POS
                .requestMatchers(HttpMethod.GET, "/api/v1/files/**").permitAll()

                // Mọi request còn lại yêu cầu xác thực JWT
                .anyRequest().authenticated()
            )

            // Thêm JWT filter trước UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCryptPasswordEncoder với strength 12 — cân bằng giữa bảo mật và hiệu năng.
     *
     * @return PasswordEncoder BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * AuthenticationManager — cần thiết cho luồng đăng nhập thủ công.
     *
     * @param authConfig cấu hình xác thực Spring
     * @return AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * MethodSecurityExpressionHandler — đăng ký CustomPermissionEvaluator
     * để @PreAuthorize("hasPermission(null, 'ORDER_CREATE')") hoạt động.
     *
     * @return expression handler với custom evaluator
     */
    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler =
                new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(customPermissionEvaluator);
        return handler;
    }
}

