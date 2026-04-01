package com.smartfnb.order.infrastructure.websocket;

import com.smartfnb.order.application.command.UpdateOrderStatusCommand;
import com.smartfnb.order.application.command.UpdateOrderStatusCommandHandler;
import com.smartfnb.order.application.dto.OrderResponse;
import com.smartfnb.shared.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * STOMP controller xử lý WebSocket messages từ client.
 * Client gửi message tới /app/orders/change-status để cập nhật trạng thái đơn hàng.
 * Broadcast kết quả qua /topic/orders/{branchId} cho tất cả client cùng chi nhánh.
 *
 * @author SmartF&B Team
 * @since 2026-03-31
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderWebSocketController {

    private final UpdateOrderStatusCommandHandler updateOrderStatusCommandHandler;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderStatusBroadcaster orderStatusBroadcaster;

    /**
     * Xử lý message từ client gửi trạng thái đơn hàng mới.
     * Endpoint: /app/orders/change-status
     * Client subscribe /topic/orders/{branchId} để nhận kết quả broadcast.
     *
     * @param request thông tin cập nhật trạng thái (gồm orderId, newStatus, reason)
     */
    @MessageMapping("/orders/change-status")
    public void handleChangeOrderStatus(@Payload UpdateOrderStatusWebSocketRequest request) {
        log.info("Nhận WebSocket message cập nhật trạng thái đơn {} sang {}",
            request.orderId(), request.newStatus());

        try {
            UpdateOrderStatusCommand command = new UpdateOrderStatusCommand(
                request.orderId(),
                TenantContext.getCurrentTenantId(),
                TenantContext.getCurrentBranchId(),
                TenantContext.getCurrentUserId(),
                request.newStatus(),
                request.reason()
            );

            var result = updateOrderStatusCommandHandler.handle(command);
            var orderResponse = OrderResponse.from(result);

            // Broadcast kết quả thành công
            orderStatusBroadcaster.broadcastOrderStatus(result.getBranchId(), orderResponse);

            log.info("Cập nhật trạng thái đơn {} qua WebSocket thành công", request.orderId());
        } catch (Exception e) {
            log.error("Lỗi cập nhật trạng thái đơn {} qua WebSocket: {}", request.orderId(), e.getMessage(), e);
            // Có thể gửi error message qua /user/{username}/queue/errors, nhưng hiện tại skip
        }
    }

    /**
     * DTO cho request WebSocket đổi trạng thái đơn hàng.
     */
    public record UpdateOrderStatusWebSocketRequest(
        UUID orderId,
        String newStatus,
        String reason
    ) {}
}
