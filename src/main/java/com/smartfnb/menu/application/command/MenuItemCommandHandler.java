package com.smartfnb.menu.application.command;

import com.smartfnb.menu.application.dto.*;
import com.smartfnb.menu.domain.exception.DuplicateMenuItemNameException;
import com.smartfnb.menu.domain.exception.MenuItemNotFoundException;
import com.smartfnb.menu.infrastructure.persistence.BranchItemJpaRepository;
import com.smartfnb.menu.infrastructure.persistence.MenuItemJpaEntity;
import com.smartfnb.menu.infrastructure.persistence.MenuItemJpaRepository;
import com.smartfnb.shared.TenantContext;
import com.smartfnb.shared.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Command Handler xử lý CRUD cho MenuItem và BranchItem.
 * Hỗ trợ soft delete và thiết lập giá riêng theo chi nhánh.
 * Ảnh được upload qua FileStorageService thay vì nhận URL từ client.
 *
 * @author SmartF&B Team
 * @since 2026-03-28
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MenuItemCommandHandler {

    private final MenuItemJpaRepository menuItemJpaRepository;
    private final BranchItemJpaRepository branchItemJpaRepository;
    private final FileStorageService fileStorageService;

    /**
     * Tạo món ăn mới trong thực đơn.
     * Nếu có file ảnh, upload và lưu URL vào entity.
     *
     * @param request thông tin món ăn cần tạo
     * @param image   file ảnh (tùy chọn — null nếu không upload)
     * @return DTO response chứa thông tin món ăn vừa tạo
     * @throws DuplicateMenuItemNameException nếu tên đã tồn tại
     */
    @Transactional
    public MenuItemResponse createMenuItem(CreateMenuItemRequest request, MultipartFile image) {
        UUID tenantId = TenantContext.requireCurrentTenantId();

        log.info("Tạo món ăn mới '{}' cho tenant {}", request.name(), tenantId);

        // Validate unique tên trong tenant
        if (menuItemJpaRepository.existsByTenantIdAndNameAndDeletedAtIsNull(tenantId, request.name())) {
            throw new DuplicateMenuItemNameException(request.name());
        }

        // Upload ảnh nếu có
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = fileStorageService.store(image);
            log.info("Đã upload ảnh cho món '{}': {}", request.name(), imageUrl);
        }

        MenuItemJpaEntity entity = new MenuItemJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setCategoryId(request.categoryId());
        entity.setName(request.name());
        entity.setType("SELLABLE");
        entity.setBasePrice(request.basePrice());
        entity.setUnit(request.unit());
        entity.setImageUrl(imageUrl);
        entity.setIsActive(true);
        entity.setIsSyncDelivery(Boolean.TRUE.equals(request.isSyncDelivery()));

        MenuItemJpaEntity saved = menuItemJpaRepository.save(entity);
        log.info("Đã tạo món ăn {} - '{}'", saved.getId(), saved.getName());

        return MenuItemResponse.from(saved);
    }

    /**
     * Cập nhật thông tin món ăn.
     * Nếu có file ảnh mới: xóa ảnh cũ và upload ảnh mới.
     * Nếu không có ảnh: giữ nguyên ảnh cũ.
     *
     * @param itemId  ID món ăn cần cập nhật
     * @param request thông tin cập nhật
     * @param image   file ảnh mới (tùy chọn — null để giữ nguyên ảnh cũ)
     * @return DTO response sau cập nhật
     * @throws MenuItemNotFoundException      nếu món ăn không tồn tại hoặc đã xóa
     * @throws DuplicateMenuItemNameException nếu tên mới đã tồn tại
     */
    @Transactional
    public MenuItemResponse updateMenuItem(UUID itemId, UpdateMenuItemRequest request, MultipartFile image) {
        UUID tenantId = TenantContext.requireCurrentTenantId();

        log.info("Cập nhật món ăn {} cho tenant {}", itemId, tenantId);

        // Lấy entity — kết hợp tenantId để chống IDOR
        MenuItemJpaEntity entity = menuItemJpaRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId)
                .orElseThrow(() -> new MenuItemNotFoundException(itemId));

        // Kiểm tra tên unique khi thay đổi tên
        if (!entity.getName().equals(request.name()) &&
                menuItemJpaRepository.existsByTenantIdAndNameAndIdNotAndDeletedAtIsNull(
                        tenantId, request.name(), itemId)) {
            throw new DuplicateMenuItemNameException(request.name());
        }

        // Xử lý ảnh: nếu có ảnh mới thì upload, xóa ảnh cũ
        if (image != null && !image.isEmpty()) {
            String oldImageUrl = entity.getImageUrl();
            String newImageUrl = fileStorageService.store(image);
            entity.setImageUrl(newImageUrl);
            log.info("Cập nhật ảnh món {}: {} → {}", itemId, oldImageUrl, newImageUrl);

            // Xóa ảnh cũ sau khi đã đổi (không throw nếu xóa thất bại)
            fileStorageService.delete(oldImageUrl);
        }
        // Nếu không có ảnh mới → giữ nguyên entity.imageUrl

        entity.setCategoryId(request.categoryId());
        entity.setName(request.name());
        entity.setBasePrice(request.basePrice());
        entity.setUnit(request.unit());
        if (request.isActive() != null) {
            entity.setIsActive(request.isActive());
        }
        if (request.isSyncDelivery() != null) {
            entity.setIsSyncDelivery(request.isSyncDelivery());
        }

        MenuItemJpaEntity saved = menuItemJpaRepository.save(entity);
        log.info("Đã cập nhật món ăn {} thành công", itemId);

        return MenuItemResponse.from(saved);
    }

    /**
     * Soft delete món ăn.
     * Đặt deleted_at và is_active = false để giữ lại dữ liệu lịch sử.
     * Không xóa ảnh — ảnh có thể vẫn được dùng ở lịch sử đơn hàng.
     *
     * @param itemId ID món ăn cần xóa
     * @throws MenuItemNotFoundException nếu món ăn không tồn tại
     */
    @Transactional
    public void deleteMenuItem(UUID itemId) {
        UUID tenantId = TenantContext.requireCurrentTenantId();

        log.info("Soft delete món ăn {} của tenant {}", itemId, tenantId);

        MenuItemJpaEntity entity = menuItemJpaRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId)
                .orElseThrow(() -> new MenuItemNotFoundException(itemId));

        entity.softDelete();
        menuItemJpaRepository.save(entity);

        log.info("Đã soft delete món ăn {} thành công", itemId);
    }

    /**
     * Thiết lập giá bán riêng cho món ăn tại một chi nhánh cụ thể.
     * Dùng upsert: cập nhật nếu đã có, tạo mới nếu chưa có.
     *
     * @param branchId ID chi nhánh
     * @param itemId   ID món ăn
     * @param request  thông tin giá và trạng thái
     * @throws MenuItemNotFoundException nếu món ăn không tồn tại trong tenant
     */
    @Transactional
    public void setBranchItemPrice(UUID branchId, UUID itemId, SetBranchItemPriceRequest request) {
        UUID tenantId = TenantContext.requireCurrentTenantId();

        log.info("Thiết lập giá branch {} cho món {} tại chi nhánh {}", tenantId, itemId, branchId);

        // Verify món ăn tồn tại trong tenant (chống IDOR)
        menuItemJpaRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(itemId, tenantId)
                .orElseThrow(() -> new MenuItemNotFoundException(itemId));

        // Upsert branch item
        branchItemJpaRepository.upsertBranchItem(
                branchId,
                itemId,
                request.branchPrice(),
                Boolean.TRUE.equals(request.isAvailable())
        );

        log.info("Đã thiết lập giá branch cho món {} tại chi nhánh {}", itemId, branchId);
    }
}
