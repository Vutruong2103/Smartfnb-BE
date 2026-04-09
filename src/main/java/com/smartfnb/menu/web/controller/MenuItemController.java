package com.smartfnb.menu.web.controller;

import com.smartfnb.menu.application.command.MenuItemCommandHandler;
import com.smartfnb.menu.application.dto.*;
import com.smartfnb.menu.application.query.MenuItemQueryHandler;
import com.smartfnb.menu.application.query.RecipeQueryHandler;
import com.smartfnb.shared.web.ApiResponse;
import com.smartfnb.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller quản lý món ăn trong thực đơn.
 * Hỗ trợ tìm kiếm pg_trgm, soft delete và xem công thức chế biến.
 * POST/PUT dùng multipart/form-data để upload ảnh từ máy người dùng.
 *
 * @author SmartF&B Team
 * @since 2026-03-28
 */
@RestController
@RequestMapping("/api/v1/menu/items")
@RequiredArgsConstructor
@Tag(name = "Menu - MenuItem", description = "API quản lý món ăn trong thực đơn")
public class MenuItemController {

    private final MenuItemCommandHandler menuItemCommandHandler;
    private final MenuItemQueryHandler menuItemQueryHandler;
    private final RecipeQueryHandler recipeQueryHandler;

    /**
     * Lấy danh sách món ăn — hỗ trợ tìm kiếm fuzzy bằng pg_trgm.
     *
     * @param keyword từ khóa tìm kiếm (tùy chọn — dùng pg_trgm similarity)
     * @param page    số trang (mặc định 0)
     * @param size    số bản ghi mỗi trang (mặc định 20)
     * @return danh sách món ăn
     */
    @GetMapping
    @PreAuthorize("hasPermission(null, 'MENU_VIEW')")
    @Operation(summary = "Danh sách món ăn (hỗ trợ tìm kiếm pg_trgm)")
    public ResponseEntity<ApiResponse<PageResponse<MenuItemResponse>>> listMenuItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<MenuItemResponse> result = menuItemQueryHandler.listMenuItems(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Lấy tất cả món đang active — dùng cho POS dropdown picker.
     *
     * @return danh sách món ăn active
     */
    @GetMapping("/active")
    @PreAuthorize("hasPermission(null, 'MENU_VIEW')")
    @Operation(summary = "Danh sách món ăn đang kích hoạt (dùng cho POS)")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> listActiveItems() {
        List<MenuItemResponse> result = menuItemQueryHandler.listActiveMenuItems();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Lấy chi tiết một món ăn.
     *
     * @param id ID món ăn
     * @return thông tin chi tiết món ăn
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'MENU_VIEW')")
    @Operation(summary = "Chi tiết món ăn")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getMenuItemById(@PathVariable UUID id) {
        MenuItemResponse result = menuItemQueryHandler.getMenuItemById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Lấy công thức chế biến của một món ăn.
     *
     * @param id ID món ăn
     * @return danh sách nguyên liệu trong công thức
     */
    @GetMapping("/{id}/recipe")
    @PreAuthorize("hasPermission(null, 'MENU_VIEW')")
    @Operation(summary = "Công thức chế biến của món ăn")
    public ResponseEntity<ApiResponse<List<RecipeResponse>>> getRecipeByItem(@PathVariable UUID id) {
        List<RecipeResponse> result = recipeQueryHandler.getRecipesByItem(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Tạo món ăn mới trong thực đơn với ảnh upload từ máy.
     *
     * <p>Request dùng multipart/form-data với 2 parts:</p>
     * <ul>
     *   <li>{@code data} — JSON thông tin món ăn (Content-Type: application/json)</li>
     *   <li>{@code image} — File ảnh (JPEG/PNG/WebP, tối đa 5MB) — tùy chọn</li>
     * </ul>
     *
     * @param request thông tin món ăn cần tạo (JSON part "data")
     * @param image   file ảnh (part "image", tùy chọn)
     * @return thông tin món ăn vừa tạo kèm imageUrl
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission(null, 'MENU_EDIT')")
    @Operation(summary = "Tạo món ăn mới (upload ảnh từ máy)")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createMenuItem(
            @RequestPart("data") @Valid CreateMenuItemRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        MenuItemResponse result = menuItemCommandHandler.createMenuItem(request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    /**
     * Cập nhật thông tin món ăn.
     * Nếu gửi kèm ảnh mới thì thay thế ảnh cũ. Không gửi ảnh thì giữ nguyên.
     *
     * <p>Request dùng multipart/form-data với 2 parts:</p>
     * <ul>
     *   <li>{@code data} — JSON thông tin cập nhật (Content-Type: application/json)</li>
     *   <li>{@code image} — File ảnh mới (JPEG/PNG/WebP, tối đa 5MB) — tùy chọn</li>
     * </ul>
     *
     * @param id      ID món ăn cần cập nhật
     * @param request thông tin cập nhật (JSON part "data")
     * @param image   file ảnh mới (part "image", tùy chọn)
     * @return thông tin món ăn sau cập nhật
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasPermission(null, 'MENU_EDIT')")
    @Operation(summary = "Cập nhật món ăn (thay ảnh nếu có)")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @PathVariable UUID id,
            @RequestPart("data") @Valid UpdateMenuItemRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        MenuItemResponse result = menuItemCommandHandler.updateMenuItem(id, request, image);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Xóa món ăn (soft delete — giữ lại dữ liệu lịch sử đơn hàng).
     *
     * @param id ID món ăn cần xóa
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'MENU_EDIT')")
    @Operation(summary = "Xóa món ăn (soft delete)")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable UUID id) {
        menuItemCommandHandler.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }
}
