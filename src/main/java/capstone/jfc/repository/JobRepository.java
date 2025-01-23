package capstone.jfc.repository;

import capstone.jfc.model.JobEntity;
import capstone.jfc.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, String> {
    List<JobEntity> findByToolId(String toolId);
    List<JobEntity> findByToolIdAndStatus(String toolId, JobStatus status);
    List<JobEntity> findByStatus(JobStatus status);
    int countByToolIdAndStatus(String toolId, JobStatus status);
}
