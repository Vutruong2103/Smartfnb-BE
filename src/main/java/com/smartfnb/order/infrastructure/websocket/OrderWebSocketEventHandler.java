package com.smartfnb.order.infrastructure.websocket;

import com.smartfnb.order.application.dto.OrderResponse;
import com.smartfnb.order.domain.event.OrderCancelledEvent;
import com.smartfnb.order.domain.event.OrderCompletedEvent;
import com.smartfnb.order.domain.event.OrderCreatedEvent;
import com.smartfnb.order.domain.event.OrderStatusChangedEvent;
import com.smartfnb.order.domain.repository.OrderRepository;
import com.smartfnb.order.domain.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lắng nghe các Domain Events từ module Order và broadcast qua WebSocket.
 *
 * <p>Các event được xử lý:
 * <ul>
 *   <li>{@link OrderCreatedEvent} → broadcast đơn mới cho bếp</li>
 *   <li>{@link OrderStatusChangedEvent} → broadcast thay đổi trạng thái</li>
 *   <li>{@link OrderCompletedEvent} → broadcast đơn hoàn thành</li>
 *   <li>{@link OrderCancelledEvent} → broadcast đơn hủy</li>
 * </ul>
 * </p>
 *
 * @author SmartF&B Team
 * @since 2026-03-31
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderWebSocketEventHandler {

    private final OrderStatusBroadcaster orderStatusBroadcaster;

    /**
     * Xử lý sự kiện đơn hàng mới được tạo.
     * Broadcast tới /topic/orders/{branchId} để bếp nhận thông báo.
     */
    @EventListener
    @Async
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Nhận sự kiện đơn hàng mới: {} tại chi nhánh {}", event.orderId(), event.branchId());

        // Broadcast ngay bằng thông tin từ event
        // Event đã chứa orderId, branchId, orderNumber, occurredAt
        orderStatusBroadcaster.broadcastNewOrder(
            event.branchId(),
            buildOrderResponseFromEvent(event)
        );
        log.info("Đã broadcast đơn mới {} tới WebSocket topic", event.orderNumber());
    }

    /**
     * Xử lý sự kiện trạng thái đơn hàng thay đổi.
     * Broadcast tới /topic/orders/{branchId} để các client cập nhật UI.
     */
    @EventListener
    @Async
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Nhận sự kiện thay đổi trạng thái đơn {} từ {} sang {}",
            event.orderId(), event.oldStatus(), event.newStatus());

        // Broadcast thay đổi trạng thái
        orderStatusBroadcaster.broadcastOrderStatus(
            event.branchId(),
            buildOrderResponseFromStatusChange(event)
        );
        log.info("Đã broadcast thay đổi trạng thái đơn {} sang {} tới WebSocket", 
            event.orderNumber(), event.newStatus());
    }

    /**
     * Xử lý sự kiện đơn hàng hoàn tất.
     * Broadcast để cập nhật trạng thái và trigger các process downstream.
     */
    @EventListener
    @Async
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Nhận sự kiện đơn hàng hoàn tất: {}", event.orderNumber());

        // Broadcast hoàn thành
        orderStatusBroadcaster.broadcastOrderStatus(
            event.branchId(),
            buildOrderResponseFromCompletedEvent(event)
        );
        log.info("Đã broadcast đơn {} hoàn tất tới WebSocket", event.orderNumber());
    }

    /**
     * Xử lý sự kiện đơn hàng bị hủy.
     * Broadcast để các client biết và cập nhật trạng thái.
     */
    @EventListener
    @Async
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Nhận sự kiện đơn hàng bị hủy: {}", event.orderId());

        // Broadcast hủy
        orderStatusBroadcaster.broadcastOrderStatus(
            event.branchId(),
            buildOrderResponseFromCancelledEvent(event)
        );
        log.info("Đã broadcast đơn {} hủy tới WebSocket", event.orderId());
    }

    /**
     * Xây dựng OrderResponse từ OrderCreatedEvent.
     */
    private OrderResponse buildOrderResponseFromEvent(OrderCreatedEvent event) {
        return new OrderResponse(
            event.orderId(),
            event.orderNumber(),
            null, // tableId - không có trong event
            null, // source
            "PENDING", // status vừa tạo
            null, null, null, null, // subtotal, discount, tax, total
            null, // notes
            null, // completedAt
            java.util.List.of() // items
        );
    }

    /**
     * Xây dựng OrderResponse từ OrderStatusChangedEvent.
     */
    private OrderResponse buildOrderResponseFromStatusChange(OrderStatusChangedEvent event) {
        return new OrderResponse(
            event.orderId(),
            event.orderNumber(),
            null, // tableId
            null, // source
            event.newStatus(), // trạng thái mới
            null, null, null, null, null, null, java.util.List.of()
        );
    }

    /**
     * Xây dựng OrderResponse từ OrderCompletedEvent.
     */
    private OrderResponse buildOrderResponseFromCompletedEvent(OrderCompletedEvent event) {
        return new OrderResponse(
            event.orderId(),
            event.orderNumber(),
            null,
            null,
            "COMPLETED",
            null, null, null,
            event.totalAmount(),
            null, null,
            java.util.List.of()
        );
    }

    /**
     * Xây dựng OrderResponse từ OrderCancelledEvent.
     */
    private OrderResponse buildOrderResponseFromCancelledEvent(OrderCancelledEvent event) {
        return new OrderResponse(
            event.orderId(),
            event.orderNumber(),
            null,
            null,
            "CANCELLED",
            null, null, null, null, null, null,
            java.util.List.of()
        );
    }
}
