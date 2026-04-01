package com.smartfnb.order.web.controller;

import com.smartfnb.order.application.command.*;
import com.smartfnb.order.application.dto.*;
import com.smartfnb.order.application.query.GetOrderByIdQuery;
import com.smartfnb.order.application.query.GetOrderListQuery;
import com.smartfnb.order.application.query.OrderListResult;
import com.smartfnb.order.application.query.OrderQueryHandler;
import com.smartfnb.order.domain.model.Order;
import com.smartfnb.shared.TenantContext;
import com.smartfnb.shared.web.ApiResponse;
import com.smartfnb.shared.web.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller quản lý đơn hàng.
 *
 * @author SmartF&B Team
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final PlaceOrderCommandHandler placeOrderCommandHandler;
    private final UpdateOrderStatusCommandHandler updateOrderStatusCommandHandler;
    private final CancelOrderCommandHandler cancelOrderCommandHandler;
    private final OrderQueryHandler orderQueryHandler;

    @PostMapping
    @PreAuthorize("hasAuthority('ORDER_CREATE') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        
        List<PlaceOrderCommand.OrderItemCommand> itemCommands = request.items().stream()
            .map(item -> new PlaceOrderCommand.OrderItemCommand(
                item.itemId(), item.itemName(), item.quantity(), 
                item.unitPrice(), item.addons(), item.notes()
            )).collect(Collectors.toList());

        PlaceOrderCommand command = new PlaceOrderCommand(
            TenantContext.getCurrentTenantId(),
            TenantContext.getCurrentBranchId(),
            null, // posSessionId
            TenantContext.getCurrentUserId(),
            request.tableId(),
            request.source(),
            request.notes(),
            itemCommands
        );

        var result = placeOrderCommandHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(OrderResponse.from(result)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ORDER_UPDATE') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id, 
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        
        UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(
            id,
            TenantContext.getCurrentTenantId(),
            TenantContext.getCurrentBranchId(),
            TenantContext.getCurrentUserId(),
            request.newStatus(),
            request.reason()
        );

        var result = updateOrderStatusCommandHandler.handle(command);
        return ResponseEntity.ok(ApiResponse.ok(OrderResponse.from(result)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('ORDER_CANCEL') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable UUID id, 
            @Valid @RequestBody CancelOrderRequest request) {
        
        CancelOrderCommand command = new CancelOrderCommand(
            id,
            TenantContext.getCurrentTenantId(),
            TenantContext.getCurrentBranchId(),
            TenantContext.getCurrentUserId(),
            request.reason()
        );

        var result = cancelOrderCommandHandler.handle(command);
        return ResponseEntity.ok(ApiResponse.ok(OrderResponse.from(result)));
    }

    /**
     * Lấy danh sách đơn hàng theo bộ lọc.
     * Query params: status, from, to, tableId, page, size
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_VIEW') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderListResult>>> getOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID tableId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        GetOrderListQuery query = new GetOrderListQuery(
            TenantContext.getCurrentBranchId(),
            status,
            from,
            to,
            tableId,
            page,
            size
        );

        Page<OrderListResult> result = orderQueryHandler.handle(query);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    /**
     * Lấy chi tiết đơn hàng theo ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_VIEW') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID id) {

        GetOrderByIdQuery query = new GetOrderByIdQuery(
            id,
            TenantContext.getCurrentTenantId(),
            TenantContext.getCurrentBranchId()
        );

        Order order = orderQueryHandler.handle(query);
        return ResponseEntity.ok(ApiResponse.ok(OrderResponse.from(order)));
    }
}
