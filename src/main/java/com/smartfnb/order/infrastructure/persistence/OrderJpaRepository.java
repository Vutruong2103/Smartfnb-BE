package com.smartfnb.order.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID>, JpaSpecificationExecutor<OrderJpaEntity> {
    Optional<OrderJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<OrderJpaEntity> findByIdAndTenantIdAndBranchId(UUID id, UUID tenantId, UUID branchId);
    Page<OrderJpaEntity> findByTenantIdAndBranchId(UUID tenantId, UUID branchId, Pageable pageable);
    Page<OrderJpaEntity> findByTenantIdAndBranchIdAndStatus(UUID tenantId, UUID branchId, String status, Pageable pageable);
}
