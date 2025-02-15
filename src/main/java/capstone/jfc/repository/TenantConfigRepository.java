package capstone.jfc.repository;


import capstone.jfc.model.TenantConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantConfigRepository extends JpaRepository<TenantConfigEntity, Integer> {
    Optional<TenantConfigEntity> findByTenantIdAndJobCategory(Integer tenantId, String jobCategory);
}
