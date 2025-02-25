package capstone.jfc.service;
import capstone.jfc.event.*;
import capstone.jfc.model.*;
import capstone.jfc.repository.JobRepository;
import capstone.jfc.repository.JobCategoryRepository;
import capstone.jfc.repository.TenantConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

@Service
public class JobScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobScheduler.class);

    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final TenantConfigRepository tenantConfigRepository;
    private final KafkaTemplate<String, Object> sendJob;
    private final KafkaTemplate<String, String> sendingJob;
    private final ObjectMapper objectMapper;

    public JobScheduler(JobRepository jobRepository,
                        JobCategoryRepository jobCategoryRepository,
                        TenantConfigRepository tenantConfigRepository,
                        KafkaTemplate<String, Object> sendJob,
                        ObjectMapper objectMapper,
                        KafkaTemplate<String, String> sendingJob) {
        this.jobRepository = jobRepository;
        this.jobCategoryRepository = jobCategoryRepository;
        this.tenantConfigRepository=tenantConfigRepository;
        this.sendJob=sendJob;
        this.objectMapper=objectMapper;
        this.sendingJob=sendingJob;
    }

    @Scheduled(fixedRate = 2000)
    public synchronized void dispatchJobsScheduled() throws JsonProcessingException {
        dispatchJobs();
    }

    public void dispatchJobs() throws JsonProcessingException {
        LOGGER.info("=== Starting dispatch cycle ===");

        List<JobEntity> newJobs = jobRepository.findByStatus(JobStatus.NEW);
        if (newJobs.isEmpty()) {
            LOGGER.info("No NEW jobs available.");
            return;
        }
        newJobs.sort(Comparator.comparing(JobEntity::getTimestampCreated));

        for (JobEntity job : newJobs) {
            String jobCategory = job.getJobCategory();

            Optional<JobCategory> jobCatOpt = jobCategoryRepository.findById(jobCategory);
            if (jobCatOpt.isEmpty()) {
                LOGGER.warn("No config found for jobCategory '{}'; skipping job {}", jobCategory, job.getJobId());
                continue;
            }
            JobCategory catConfig = jobCatOpt.get();

            int jobInProgress = jobRepository.countByJobCategoryAndStatus(jobCategory, JobStatus.IN_PROGRESS);
            if (jobInProgress >= catConfig.getMaxConcurrentJobs()) {
                continue;
            }
            Integer tenantId = job.getTenantId();

            Optional<TenantConfigEntity> tenantConfigOpt =
                    tenantConfigRepository.findByTenantIdAndJobCategory(tenantId, jobCategory);
            if (tenantConfigOpt.isEmpty()) {
                LOGGER.warn("No tenant config found for tenantId={} and jobCategory={}, skipping job {}",
                        tenantId, jobCategory, job.getJobId());
                continue;
            }
            TenantConfigEntity tenantConfig = tenantConfigOpt.get();

            int tenantInProgress = jobRepository.countByJobCategoryAndTenantIdAndStatus(
                    jobCategory, tenantId, JobStatus.IN_PROGRESS
            );
            if (tenantInProgress >= tenantConfig.getMaxConcurrentJobs()) {
                continue;
            }

            job.setStatus(JobStatus.IN_PROGRESS);
            jobRepository.save(job);
            String storedJson = job.getPayload();
            String topic= catConfig.getDestinationTopic();

            if (jobCategory.startsWith("scan")) {
                ScanRequestPayload scanPayload = objectMapper.readValue(storedJson, ScanRequestPayload.class);
                ScanRequestEvent scanEvent = new ScanRequestEvent(scanPayload);
                scanEvent.setEventId(job.getJobId());
                sendJob.send(topic, scanEvent);

            } else if (jobCategory.startsWith("parse")) {
                ParseRequestPayload parsePayload = objectMapper.readValue(storedJson, ParseRequestPayload.class);
                ParseRequestEvent parseEvent = new ParseRequestEvent(parsePayload);
                parseEvent.setEventId(job.getJobId());
                sendJob.send(topic, parseEvent);

            }else if(jobCategory.startsWith("update")){
                UpdateRequestPayload updateRequestPayload =objectMapper.readValue(storedJson, UpdateRequestPayload.class);
                UpdateRequestEvent updateRequestEvent=new UpdateRequestEvent(updateRequestPayload);
                updateRequestEvent.setEventId(job.getJobId());
                String json = objectMapper.writeValueAsString(updateRequestEvent);
                sendingJob.send(topic, json);
//                sendJob.send(topic, updateRequestEvent);
            }
            else if(jobCategory.startsWith("ticketCreate")){
                TicketCreateRequestPayload ticketCreateRequestPayload =objectMapper.readValue(storedJson, TicketCreateRequestPayload.class);
                TicketCreateRequestEvent ticketCreateRequestEvent=new TicketCreateRequestEvent(ticketCreateRequestPayload);
                ticketCreateRequestEvent.setEventId(job.getJobId());
                String json = objectMapper.writeValueAsString(ticketCreateRequestEvent);
                sendingJob.send(topic, json);
//                sendJob.send(topic, ticketCreateRequestEvent);
            }
            else if(jobCategory.startsWith("ticketTransition")){
                TicketTransitionRequestPayload ticketTransitionRequestPayload =objectMapper.readValue(storedJson, TicketTransitionRequestPayload.class);
                TicketTransitionRequestEvent ticketTransitionRequestEvent=new TicketTransitionRequestEvent(ticketTransitionRequestPayload);
                ticketTransitionRequestEvent.setEventId(job.getJobId());
                String json = objectMapper.writeValueAsString(ticketTransitionRequestEvent);
                System.out.println("hello");
                System.out.println(json);
                sendingJob.send(topic, json);
            }
            else {
                LOGGER.warn("Unrecognized jobCategory={}, skipping...", jobCategory);
                continue;
            }

        }
        LOGGER.info("=== End of dispatch cycle ===");
    }
}