package capstone.jfc.repository;

import capstone.jfc.model.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolConfigRepository extends JpaRepository<ToolEntity, String> {
}