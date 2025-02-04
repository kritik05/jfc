package capstone.jfc.service;
import capstone.jfc.model.JobEntity;
import capstone.jfc.producer.JobProducer;
import capstone.jfc.model.JobCategory;
import capstone.jfc.model.JobStatus;
import capstone.jfc.repository.JobRepository;
import capstone.jfc.repository.JobCategoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

@Service
public class BatchDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchDispatcher.class);

    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final JobProducer jobProducer;

    @Value("${jfc.global-concurrency-limit:5}")
    private int globalConcurrencyLimit;

    public BatchDispatcher(JobRepository jobRepository,
                           JobCategoryRepository jobCategoryRepository,
                           JobProducer jobProducer) {
        this.jobRepository = jobRepository;
        this.jobCategoryRepository = jobCategoryRepository;
        this.jobProducer = jobProducer;
    }

    @Scheduled(fixedRate = 2000)
    public synchronized void dispatchJobsScheduled() {
        dispatchJobs();
    }

    public void dispatchJobs() {
        LOGGER.info("=== Starting dispatch cycle ===");

        int globalInProgress = jobRepository.countByStatus(JobStatus.IN_PROGRESS);

        if (globalInProgress >= globalConcurrencyLimit) {
            LOGGER.info("Global concurrency limit reached. No new jobs can be dispatched.");
            return;
        }

        List<JobEntity> newJobs = jobRepository.findByStatus(JobStatus.NEW);
        if (newJobs.isEmpty()) {
            LOGGER.info("No NEW jobs available.");
            return;
        }
        newJobs.sort(Comparator.comparing(JobEntity::getTimestampCreated));

        for (JobEntity job : newJobs) {
            if (globalInProgress >= globalConcurrencyLimit) {
                LOGGER.info("Reached global concurrency limit while iterating jobs.");
                break;
            }

            String jobCategory = job.getJobCategory();
            JobCategory config = jobCategoryRepository.findById(jobCategory).orElse(null);
            if (config == null) {
                LOGGER.warn("No config found for tool '{}'; skipping job {}", jobCategory, job.getJobId());
                continue; // skip this job, maybe the next job belongs to a valid tool
            }

            int jobLimit = config.getMaxConcurrentJobs();

            int jobInProgress = jobRepository.countByJobCategoryAndStatus(jobCategory, JobStatus.IN_PROGRESS);

            if (jobInProgress >= jobLimit) {
                continue;
            }

            job.setStatus(JobStatus.IN_PROGRESS);
            jobRepository.save(job);

            Map<String, Object> message = new HashMap<>();
            message.put("jobId", job.getJobId());
            message.put("jobCategory", job.getJobCategory());
            message.put("payload", job.getPayload());
            message.put("priority", job.getPriority());

            jobProducer.sendJobToJobCategory(config.getDestinationTopic(), message);

            globalInProgress++;

        }
        LOGGER.info("=== End of dispatch cycle ===");
    }
}