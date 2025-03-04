package capstone.jfc.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job")
public class JobEntity {
    @Id
    @Column(name = "job_id", updatable = false)
    private String jobId;

    @Column(name = "job_category")
    private String jobCategory;

    @Column(name = "tenant_id")
    private Integer tenantId;

    @Lob
    @Column(name = "payload",columnDefinition = "LONGTEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private JobStatus status;

    @Column(name = "timestamp_created")
    private LocalDateTime timestampCreated;

    @Column(name = "timestamp_updated")
    private LocalDateTime timestampUpdated;

    public JobEntity() {}

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobCategory() {
        return jobCategory;
    }

    public void setJobCategory(String jobCategory) {
        this.jobCategory = jobCategory;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestampCreated() {
        return timestampCreated;
    }

    public void setTimestampCreated(LocalDateTime timestampCreated) {
        this.timestampCreated = timestampCreated;
    }

    public LocalDateTime getTimestampUpdated() {
        return timestampUpdated;
    }

    public void setTimestampUpdated(LocalDateTime timestampUpdated) {
        this.timestampUpdated = timestampUpdated;
    }

    @PrePersist
    public void onPrePersist() {
        if (timestampCreated == null) {
            timestampCreated = LocalDateTime.now();
        }
        timestampUpdated = LocalDateTime.now();
    }

    @PreUpdate
    public void onPreUpdate() {
        timestampUpdated = LocalDateTime.now();
    }

}
