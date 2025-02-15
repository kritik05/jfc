package capstone.jfc.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tenant_config")
public class TenantConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Integer tenantId;

    @Column(name = "job_category")
    private String jobCategory;

    @Column(name = "max_concurrent_jobs")
    private int maxConcurrentJobs;

    public TenantConfigEntity(){};

    public TenantConfigEntity(Long id, Integer tenantId, String jobCategory, int maxConcurrentJobs) {
        this.id = id;
        this.tenantId = tenantId;
        this.jobCategory = jobCategory;
        this.maxConcurrentJobs = maxConcurrentJobs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    public String getJobCategory() {
        return jobCategory;
    }

    public void setJobCategory(String jobCategory) {
        this.jobCategory = jobCategory;
    }

    public int getMaxConcurrentJobs() {
        return maxConcurrentJobs;
    }

    public void setMaxConcurrentJobs(int maxConcurrentJobs) {
        this.maxConcurrentJobs = maxConcurrentJobs;
    }
}