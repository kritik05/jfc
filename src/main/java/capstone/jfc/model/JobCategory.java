package capstone.jfc.model;
import jakarta.persistence.*;

@Entity
@Table(name = "job_category_config")
public class JobCategory {
    @Id
    @Column(name = "job_category", nullable = false)
    private String jobCategory;

    @Column(name = "max_concurrent_jobs")
    private Integer maxConcurrentJobs;

    @Column(name = "destination_topic")
    private String destinationTopic;


    public JobCategory() {}

    public String getJobCategory() {
        return jobCategory;
    }
    public void setJobCategory(String jobCategory) {
        this.jobCategory = jobCategory;
    }
    public Integer getMaxConcurrentJobs() {
        return maxConcurrentJobs;
    }
    public void setMaxConcurrentJobs(Integer maxConcurrentJobs) {
        this.maxConcurrentJobs = maxConcurrentJobs;
    }
    public String getDestinationTopic() {
        return destinationTopic;
    }
    public void setDestinationTopic(String destinationTopic) {
        this.destinationTopic = destinationTopic;
    }
}
