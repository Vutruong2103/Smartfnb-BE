package com.smartfnb.auth.application.command;

import com.smartfnb.auth.application.dto.AuthResponse;
import com.smartfnb.auth.infrastructure.jwt.JwtService;
import com.smartfnb.auth.infrastructure.persistence.UserJpaEntity;
import com.smartfnb.auth.infrastructure.persistence.UserRepository;
import com.smartfnb.rbac.domain.service.PermissionService;
import com.smartfnb.shared.exception.SmartFnbException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Xử lý lệnh làm mới JWT access token từ refresh token.
 * Chiến lược: validate refresh token → cấp access token mới.
 * Refresh token vẫn có hiệu lực cho đến khi hết hạn tự nhiên.
 *
 * <p>Lưu ý: Để bảo mật cao hơn (rotate refresh), cần lưu refresh token vào DB.
 * Hiện tại implement stateless — có thể nâng cấp sau.</p>
 *
 * @author SmartF&B Team
 * @since 2026-03-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenCommandHandler {

    private final JwtService      jwtService;
    private final UserRepository   userRepository;
    private final PermissionService permissionService;

    /**
     * Làm mới access token từ refresh token hợp lệ.
     *
     * @param command lệnh chứa refresh token
     * @return AuthResponse mới với access token mới
     * @throws SmartFnbException TOKEN_INVALID nếu refresh token không hợp lệ / hết hạn
     * @throws SmartFnbException ACCOUNT_INACTIVE nếu tài khoản đã bị vô hiệu hóa
     */
    @Transactional(readOnly = true)
    public AuthResponse handle(RefreshTokenCommand command) {
        // 1. Validate và parse refresh token
        Claims claims;
        try {
            claims = jwtService.validateAndExtractClaims(command.refreshToken());
        } catch (JwtException ex) {
            throw new SmartFnbException("TOKEN_INVALID",
                    "Refresh token không hợp lệ hoặc đã hết hạn", 401);
        }

        // 2. Kiểm tra đây là refresh token (không phải access token)
        String tokenType = claims.get("type", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new SmartFnbException("TOKEN_INVALID",
                    "Token không phải refresh token hợp lệ", 401);
        }

        // 3. Lấy userId từ subject
        UUID userId = UUID.fromString(claims.getSubject());

        // 4. Tải lại thông tin user từ DB để đảm bảo tài khoản còn active
        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new SmartFnbException("TOKEN_INVALID",
                        "Người dùng không tồn tại", 401));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new SmartFnbException("ACCOUNT_INACTIVE",
                    "Tài khoản đã bị vô hiệu hóa", 403);
        }

        // 5. Load role và permissions thật từ DB qua PermissionService
        List<String> roleNames  = permissionService.getRoleNames(user.getId(), user.getTenantId());
        String       primaryRole = roleNames.isEmpty() ? "STAFF" : roleNames.get(0);
        List<String> permissions = permissionService.getPermissionCodes(user.getId(), user.getTenantId());

        String newAccessToken = jwtService.generateAccessToken(
                userId, user.getTenantId(), primaryRole, permissions, null);

        log.info("Refresh token thành công — userId: {}", userId);

        return AuthResponse.of(
                newAccessToken,
                command.refreshToken(), // Giữ refresh token cũ
                jwtService.getAccessExpirationSeconds(),
                userId.toString(),
                user.getTenantId().toString(),
                primaryRole
        );
    }
}
