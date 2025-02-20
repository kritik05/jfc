package capstone.jfc.repository;

import capstone.jfc.model.JobEntity;
import capstone.jfc.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, String> {
    List<JobEntity> findByStatus(JobStatus status);
    int countByJobCategoryAndStatus(String jobCategory, JobStatus status);
    int countByJobCategoryAndTenantIdAndStatus(String jobCategory, Integer tenantId, JobStatus status);
}
