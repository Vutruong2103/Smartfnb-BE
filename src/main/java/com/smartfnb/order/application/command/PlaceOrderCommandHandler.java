package com.smartfnb.order.application.command;

import com.smartfnb.order.domain.model.Order;
import com.smartfnb.order.domain.model.OrderItem;
import com.smartfnb.order.domain.repository.OrderRepository;
import com.smartfnb.order.domain.exception.TableNotAvailableException;
import com.smartfnb.order.infrastructure.persistence.TableJpaEntity;
import com.smartfnb.order.infrastructure.persistence.TableJpaRepository;
import com.smartfnb.order.infrastructure.external.MenuInventoryAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Xử lý lệnh tạo đơn hàng.
 * Luồng: Validate bàn -> Kiểm tra tồn kho -> Tạo đơn -> Publish event
 *
 * @author SmartF&B Team
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlaceOrderCommandHandler {

    private final OrderRepository orderRepository;
    private final TableJpaRepository tableRepository;
    private final MenuInventoryAdapter inventoryAdapter;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order handle(PlaceOrderCommand command) {
        log.info("Tạo đơn hàng cho bàn {} tại chi nhánh {}", command.tableId(), command.branchId());

        // 1. Kiểm tra bàn còn trống không (nếu mua tại quán)
        if (command.tableId() != null) {
            TableJpaEntity table = tableRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(command.tableId(), command.tenantId())
                .orElseThrow(() -> new com.smartfnb.order.domain.exception.TableNotFoundException(command.tableId()));

            if (!table.isAvailable()) {
                throw new TableNotAvailableException(command.tableId());
            }

            // Cập nhật trạng thái bàn sang OCCUPIED
            table.setStatus("OCCUPIED");
            tableRepository.save(table);
        }

        // Map items
        List<OrderItem> items = command.items().stream()
            .map(cmdItem -> OrderItem.builder()
                .itemId(cmdItem.itemId())
                .itemName(cmdItem.itemName())
                .quantity(cmdItem.quantity())
                .unitPrice(cmdItem.unitPrice())
                .addons(cmdItem.addons())
                .notes(cmdItem.notes())
                .build())
            .collect(Collectors.toList());

        // 2. Kiểm tra tồn kho nguyên liệu
        inventoryAdapter.checkStock(command.branchId(), items);

        // 3. Tạo aggregate Order
        Order order = Order.create(
            command.tenantId(),
            command.branchId(),
            command.posSessionId(),
            command.staffId(),
            command.tableId(),
            command.source(),
            items,
            command.notes()
        );

        // 4. Lưu vào DB
        Order savedOrder = orderRepository.save(order);

        // 5. Publish domain event
        eventPublisher.publishEvent(new com.smartfnb.order.domain.event.OrderCreatedEvent(
            savedOrder.getId(), 
            savedOrder.getBranchId(),
            savedOrder.getOrderNumber(),
            java.time.Instant.now()
        ));

        log.info("Đã tạo đơn hàng {} thành công", savedOrder.getOrderNumber());
        return savedOrder;
    }
}
