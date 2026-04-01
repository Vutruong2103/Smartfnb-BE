package com.smartfnb.order.application.command;

import com.smartfnb.order.domain.model.Order;
import com.smartfnb.order.domain.repository.OrderRepository;
import com.smartfnb.order.domain.exception.OrderNotFoundException;
import com.smartfnb.order.infrastructure.persistence.OrderStatusLogJpaEntity;
import com.smartfnb.order.infrastructure.persistence.OrderStatusLogJpaRepository;
import com.smartfnb.order.infrastructure.persistence.TableJpaEntity;
import com.smartfnb.order.infrastructure.persistence.TableJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class CancelOrderCommandHandler {

    private final OrderRepository orderRepository;
    private final OrderStatusLogJpaRepository statusLogRepository;
    private final TableJpaRepository tableRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order handle(CancelOrderCommand command) {
        log.info("Huỷ đơn {} bởi staff {}", command.orderId(), command.staffId());

        Order order = orderRepository.findByIdAndTenantIdAndBranchId(command.orderId(), command.tenantId(), command.branchId())
            .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        String oldStatus = order.getStatus().name();

        // Hủy đơn
        order.cancel();

        // Nếu bàn đang Occupied thì giải phóng bàn (nếu không có đơn nào khác trên bàn)
        if (order.getTableId() != null) {
            tableRepository.findByIdAndTenantIdAndDeletedAtIsNull(order.getTableId(), command.tenantId())
                .ifPresent(table -> {
                    // Logic thực tế có thể cần kiểm tra xem bàn còn đơn nào khác không
                    table.setStatus("AVAILABLE");
                    tableRepository.save(table);
                });
        }

        Order savedOrder = orderRepository.save(order);

        // Ghi log
        OrderStatusLogJpaEntity logEntity = new OrderStatusLogJpaEntity();
        logEntity.setOrderId(order.getId());
        logEntity.setOldStatus(oldStatus);
        logEntity.setNewStatus(order.getStatus().name());
        logEntity.setChangedByUserId(command.staffId());
        logEntity.setReason(command.reason());
        statusLogRepository.save(logEntity);

        // Phát event
        eventPublisher.publishEvent(new com.smartfnb.order.domain.event.OrderStatusChangedEvent(
                savedOrder.getId(), savedOrder.getBranchId(), savedOrder.getOrderNumber(),
                oldStatus, savedOrder.getStatus().name(), command.staffId(), Instant.now()
        ));
        
        eventPublisher.publishEvent(new com.smartfnb.order.domain.event.OrderCancelledEvent(
                savedOrder.getId(),
                savedOrder.getBranchId(),
                savedOrder.getOrderNumber(),
                Instant.now()
        ));

        return savedOrder;
    }
}
