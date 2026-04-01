package com.smartfnb.order.application.query;

import com.smartfnb.order.domain.model.Order;
import com.smartfnb.order.domain.repository.OrderRepository;
import com.smartfnb.order.infrastructure.persistence.OrderJpaEntity;
import com.smartfnb.order.infrastructure.persistence.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Xử lý các query liên quan đến đơn hàng.
 *
 * @author SmartF&B Team
 * @since 2026-03-31
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueryHandler {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderRepository orderRepository;

    /**
     * Lấy danh sách đơn hàng theo bộ lọc.
     * Chỉ trả về đơn trong scope tenant/branch của người dùng hiện tại.
     *
     * @param query query chứa tham số lọc
     * @return danh sách đơn hàng phân trang
     */
    public Page<OrderListResult> handle(GetOrderListQuery query) {
        log.info("Lấy danh sách đơn hàng: branch={}, status={}, from={}, to={}",
            query.branchId(), query.status(), query.from(), query.to());

        PageRequest pageRequest = PageRequest.of(
            query.page(),
            Math.min(query.size(), 100),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<OrderJpaEntity> orderPage;

        // Build query động theo filter
        if (query.status() != null && !query.status().isBlank()) {
            orderPage = orderJpaRepository.findByTenantIdAndBranchIdAndStatus(
                query.branchId(),  // tenantId
                query.branchId(),  // branchId
                query.status(),
                pageRequest
            );
        } else {
            orderPage = orderJpaRepository.findByTenantIdAndBranchId(
                query.branchId(),  // tenantId
                query.branchId(),  // branchId
                pageRequest
            );
        }

        return orderPage.map(this::toOrderListResult);
    }

    /**
     * Lấy thông tin chi tiết đơn hàng theo ID.
     * Kiểm tra tenant và branch để đảm bảo phân quyền.
     *
     * @param query query chứa orderId và context
     * @return thông tin chi tiết đơn hàng
     */
    public Order handle(GetOrderByIdQuery query) {
        log.info("Lấy chi tiết đơn hàng: {}", query.orderId());

        return orderRepository.findByIdAndTenantIdAndBranchId(
                query.orderId(),
                query.tenantId(),
                query.branchId()
            )
            .orElseThrow(() -> {
                log.warn("Không tìm thấy đơn hàng {} hoặc không thuộc scope", query.orderId());
                return new com.smartfnb.order.domain.exception.OrderNotFoundException(query.orderId());
            });
    }

    private OrderListResult toOrderListResult(OrderJpaEntity entity) {
        String tableName = entity.getTableId() != null ? "Bàn " + entity.getTableId().toString().substring(0, 4) : "Takeaway";
        String staffName = entity.getUserId() != null ? "Staff " + entity.getUserId().toString().substring(0, 4) : "Unknown";

        // Chuyển LocalDateTime thành Instant
        Instant createdAtInstant = entity.getCreatedAt() != null
            ? entity.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
            : Instant.now();

        return new OrderListResult(
            entity.getId(),
            entity.getOrderNumber(),
            entity.getTableId(),
            tableName,
            entity.getStatus(),
            entity.getTotalAmount(),
            createdAtInstant,
            staffName
        );
    }
}
