package com.smartfnb.order.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfnb.order.application.command.PlaceOrderCommand;
import com.smartfnb.order.application.command.PlaceOrderCommandHandler;
import com.smartfnb.order.application.command.UpdateOrderStatusCommand;
import com.smartfnb.order.application.command.UpdateOrderStatusCommandHandler;
import com.smartfnb.order.application.dto.OrderItemResponse;
import com.smartfnb.order.application.dto.OrderResponse;
import com.smartfnb.order.application.query.OrderQueryHandler;
import com.smartfnb.order.domain.model.Order;
import com.smartfnb.order.domain.model.OrderItem;
import com.smartfnb.order.domain.model.OrderSource;
import com.smartfnb.order.domain.repository.OrderRepository;
import com.smartfnb.shared.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test WebSocket realtime broadcast cho S-10 — Order Realtime.
 * Kiểm tra:
 * - 2 client kết nối đồng thời
 * - Cả 2 nhận được OrderCreatedEvent broadcast
 * - Cả 2 nhận được OrderStatusChangedEvent broadcast khi status thay đổi
 *
 * @author SmartF&B Team
 * @since 2026-03-31
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext
@Slf4j
@DisplayName("S-10: Order WebSocket Realtime Integration Tests")
public class OrderWebSocketIntegrationTest {

    @Autowired
    private PlaceOrderCommandHandler placeOrderCommandHandler;

    @Autowired
    private UpdateOrderStatusCommandHandler updateOrderStatusCommandHandler;

    @Autowired
    private OrderQueryHandler orderQueryHandler;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID tenantId;
    private UUID branchId;
    private UUID tableId;
    private UUID staffId;

    private WebSocketStompClient stompClient;
    private String webSocketUrl;

    @BeforeEach
    public void setup() throws Exception {
        // Setup test data
        tenantId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        tableId = UUID.randomUUID();
        staffId = UUID.randomUUID();

        // Setup TenantContext for test
        TenantContext.setCurrentTenantId(tenantId);
        TenantContext.setCurrentBranchId(branchId);
        TenantContext.setCurrentUserId(staffId);

        // Setup WebSocket client
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(webSocketClient);
        webSocketUrl = "ws://localhost:8080/ws";
    }

    @Test
    @DisplayName("Test 2 concurrent clients nhận OrderCreatedEvent broadcast qua WebSocket")
    public void testTwoConcurrentClientsReceiveOrderCreatedEvent() throws Exception {
        log.info("============ TEST: 2 Concurrent Clients Receive OrderCreatedEvent ============");

        // Queue để store messages nhận được từ WebSocket
        BlockingQueue<OrderResponse> client1Messages = new LinkedBlockingDeque<>();
        BlockingQueue<OrderResponse> client2Messages = new LinkedBlockingDeque<>();

        try {
            // Client 1 connect và subscribe
            StompSession session1 = stompClient.connect(webSocketUrl, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
            session1.subscribe("/topic/orders/" + branchId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return OrderResponse.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    OrderResponse order = (OrderResponse) payload;
                    log.info("[Client1] Nhận message: {}", order.orderNumber());
                    client1Messages.offer(order);
                }
            });

            // Client 2 connect và subscribe (cùng topic)
            StompSession session2 = stompClient.connect(webSocketUrl, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
            session2.subscribe("/topic/orders/" + branchId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return OrderResponse.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    OrderResponse order = (OrderResponse) payload;
                    log.info("[Client2] Nhận message: {}", order.orderNumber());
                    client2Messages.offer(order);
                }
            });

            // Tạo đơn hàng (sẽ trigger OrderCreatedEvent → broadcast)
            log.info("Creating order...");
            PlaceOrderCommand command = new PlaceOrderCommand(
                tenantId,
                branchId,
                null,
                staffId,
                tableId,
                OrderSource.IN_STORE,
                "Test order",
                List.of(
                    new PlaceOrderCommand.OrderItemCommand(
                        UUID.randomUUID(),
                        "Cà phê đen",
                        2,
                        BigDecimal.valueOf(20000),
                        null,
                        null
                    )
                )
            );

            Order createdOrder = placeOrderCommandHandler.handle(command);
            log.info("Order {} created", createdOrder.getOrderNumber());

            // Chờ cả 2 client nhận được message
            OrderResponse msg1 = client1Messages.poll(10, TimeUnit.SECONDS);
            OrderResponse msg2 = client2Messages.poll(10, TimeUnit.SECONDS);

            assertNotNull(msg1, "Client 1 should receive OrderCreatedEvent");
            assertNotNull(msg2, "Client 2 should receive OrderCreatedEvent");

            assertEquals(createdOrder.getOrderNumber(), msg1.orderNumber(), "Client 1 should receive correct order");
            assertEquals(createdOrder.getOrderNumber(), msg2.orderNumber(), "Client 2 should receive correct order");
            assertEquals("PENDING", msg1.status(), "Order status should be PENDING");
            assertEquals("PENDING", msg2.status(), "Order status should be PENDING");

            log.info("✅ Cả 2 client nhận được OrderCreatedEvent broadcast thành công!");

            // Cleanup
            session1.disconnect();
            session2.disconnect();

        } catch (Exception e) {
            log.error("Test failed", e);
            fail("WebSocket test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test 2 concurrent clients nhận OrderStatusChangedEvent khi cập nhật trạng thái")
    public void testTwoConcurrentClientsReceiveOrderStatusChangedEvent() throws Exception {
        log.info("============ TEST: 2 Concurrent Clients Receive OrderStatusChangedEvent ============");

        BlockingQueue<OrderResponse> client1Messages = new LinkedBlockingDeque<>();
        BlockingQueue<OrderResponse> client2Messages = new LinkedBlockingDeque<>();

        try {
            // Tạo order trước
            PlaceOrderCommand createCommand = new PlaceOrderCommand(
                tenantId,
                branchId,
                null,
                staffId,
                tableId,
                OrderSource.IN_STORE,
                "Test order for status change",
                List.of(
                    new PlaceOrderCommand.OrderItemCommand(
                        UUID.randomUUID(),
                        "Trà sữa",
                        1,
                        BigDecimal.valueOf(30000),
                        null,
                        "Ít dá"
                    )
                )
            );

            Order order = placeOrderCommandHandler.handle(createCommand);
            UUID orderId = order.getId();
            log.info("Order {} created", order.getOrderNumber());

            // Client 1 connect
            StompSession session1 = stompClient.connect(webSocketUrl, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
            session1.subscribe("/topic/orders/" + branchId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return OrderResponse.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    OrderResponse orderResp = (OrderResponse) payload;
                    log.info("[Client1] Nhận status change: {} → {}", orderResp.orderNumber(), orderResp.status());
                    client1Messages.offer(orderResp);
                }
            });

            // Client 2 connect
            StompSession session2 = stompClient.connect(webSocketUrl, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
            session2.subscribe("/topic/orders/" + branchId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return OrderResponse.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    OrderResponse orderResp = (OrderResponse) payload;
                    log.info("[Client2] Nhận status change: {} → {}", orderResp.orderNumber(), orderResp.status());
                    client2Messages.offer(orderResp);
                }
            });

            // Cập nhật trạng thái sang PROCESSING
            log.info("Updating order status to PROCESSING...");
            UpdateOrderStatusCommand updateCommand = new UpdateOrderStatusCommand(
                orderId,
                tenantId,
                branchId,
                staffId,
                "PROCESSING",
                "Bếp đang pha chế"
            );

            Order updatedOrder = updateOrderStatusCommandHandler.handle(updateCommand);
            log.info("Order status updated to {}", updatedOrder.getStatus());

            // Chờ cả 2 client nhận được message
            OrderResponse msg1 = client1Messages.poll(10, TimeUnit.SECONDS);
            OrderResponse msg2 = client2Messages.poll(10, TimeUnit.SECONDS);

            assertNotNull(msg1, "Client 1 should receive OrderStatusChangedEvent");
            assertNotNull(msg2, "Client 2 should receive OrderStatusChangedEvent");

            assertEquals("PROCESSING", msg1.status(), "Client 1 should receive PROCESSING status");
            assertEquals("PROCESSING", msg2.status(), "Client 2 should receive PROCESSING status");

            log.info("✅ Cả 2 client nhận được OrderStatusChangedEvent broadcast thành công!");

            // Cập nhật sang COMPLETED
            log.info("Updating order status to COMPLETED...");
            UpdateOrderStatusCommand completeCommand = new UpdateOrderStatusCommand(
                orderId,
                tenantId,
                branchId,
                staffId,
                "COMPLETED",
                "Đã pha chế xong"
            );

            Order completedOrder = updateOrderStatusCommandHandler.handle(completeCommand);
            log.info("Order status updated to {}", completedOrder.getStatus());

            // Chờ cả 2 client nhận được message lần 2
            OrderResponse msg1Complete = client1Messages.poll(10, TimeUnit.SECONDS);
            OrderResponse msg2Complete = client2Messages.poll(10, TimeUnit.SECONDS);

            assertNotNull(msg1Complete, "Client 1 should receive COMPLETED status");
            assertNotNull(msg2Complete, "Client 2 should receive COMPLETED status");

            assertEquals("COMPLETED", msg1Complete.status(), "Client 1 should receive COMPLETED status");
            assertEquals("COMPLETED", msg2Complete.status(), "Client 2 should receive COMPLETED status");

            log.info("✅ Cả 2 client nhận được toàn bộ status changes thành công!");

            // Cleanup
            session1.disconnect();
            session2.disconnect();

        } catch (Exception e) {
            log.error("Test failed", e);
            fail("WebSocket test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test STOMP message: Client gửi status update qua /app/orders/change-status")
    public void testClientSendStatusUpdateViaStomp() throws Exception {
        log.info("============ TEST: Client Send Status Update Via STOMP ============");

        BlockingQueue<OrderResponse> messages = new LinkedBlockingDeque<>();

        try {
            // Tạo order
            PlaceOrderCommand createCommand = new PlaceOrderCommand(
                tenantId,
                branchId,
                null,
                staffId,
                tableId,
                OrderSource.IN_STORE,
                "Test STOMP message",
                List.of(
                    new PlaceOrderCommand.OrderItemCommand(
                        UUID.randomUUID(),
                        "Nước ép cam",
                        1,
                        BigDecimal.valueOf(25000),
                        null,
                        null
                    )
                )
            );

            Order order = placeOrderCommandHandler.handle(createCommand);
            UUID orderId = order.getId();
            log.info("Order {} created", order.getOrderNumber());

            // Client connect
            StompSession session = stompClient.connect(webSocketUrl, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
            session.subscribe("/topic/orders/" + branchId, new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return OrderResponse.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    OrderResponse orderResp = (OrderResponse) payload;
                    log.info("[Client] Nhận broadcast: {} status={}", orderResp.orderNumber(), orderResp.status());
                    messages.offer(orderResp);
                }
            });

            // Client gửi message qua STOMP để cập nhật status
            log.info("Sending STOMP message to change status...");
            OrderWebSocketController.UpdateOrderStatusWebSocketRequest request =
                new OrderWebSocketController.UpdateOrderStatusWebSocketRequest(
                    orderId,
                    "PROCESSING",
                    "Client updated via STOMP"
                );

            session.send("/app/orders/change-status", request);

            // Chờ nhận broadcast result
            OrderResponse response = messages.poll(10, TimeUnit.SECONDS);
            assertNotNull(response, "Should receive broadcast after STOMP message");
            assertEquals("PROCESSING", response.status(), "Status should be PROCESSING");

            log.info("✅ STOMP message processed và broadcast thành công!");

            // Cleanup
            session.disconnect();

        } catch (Exception e) {
            log.error("Test failed", e);
            fail("STOMP test failed: " + e.getMessage());
        }
    }
}
