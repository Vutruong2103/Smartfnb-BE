package com.smartfnb.order.infrastructure.websocket;

import com.smartfnb.order.application.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Service broadcast cập nhật trạng thái đơn hàng qua WebSocket STOMP.
 * Topic: /topic/orders/{branchId}
 *
 * <p>Được gọi khi trạng thái đơn hàng thay đổi:
 * <ul>
 *   <li>PENDING → PROCESSING (Bếp bắt đầu làm)</li>
 *   <li>PROCESSING → COMPLETED (Đơn hoàn thành)</li>
 *   <li>PENDING/PROCESSING → CANCELLED (Đơn bị hủy)</li>
 * </ul>
 * </p>
 *
 * @author SmartF&B Team
 * @since 2026-03-31
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast trạng thái đơn hàng mới tới tất cả client đang theo dõi chi nhánh.
     * Client subscribe /topic/orders/{branchId} để nhận cập nhật realtime.
     *
     * @param branchId ID chi nhánh cần broadcast
     * @param orderResponse thông tin đơn hàng cập nhật
     */
    public void broadcastOrderStatus(UUID branchId, OrderResponse orderResponse) {
        String topic = "/topic/orders/" + branchId;

        log.debug("Broadcast trạng thái đơn {} tới topic {}", orderResponse.orderNumber(), topic);

        messagingTemplate.convertAndSend(topic, orderResponse);
    }

    /**
     * Broadcast thông báo đơn hàng mới được tạo.
     * Dùng khi OrderCreatedEvent được publish.
     *
     * @param branchId ID chi nhánh
     * @param orderResponse thông tin đơn hàng mới
     */
    public void broadcastNewOrder(UUID branchId, OrderResponse orderResponse) {
        String topic = "/topic/orders/" + branchId;

        log.info("Broadcast đơn mới {} tới topic {}", orderResponse.orderNumber(), topic);

        messagingTemplate.convertAndSend(topic, orderResponse);
    }
}
