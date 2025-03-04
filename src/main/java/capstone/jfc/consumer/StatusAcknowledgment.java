package capstone.jfc.consumer;

import capstone.jfc.event.AcknowledgementEvent;
import capstone.jfc.model.*;
import capstone.jfc.repository.JobRepository;
import capstone.jfc.service.JobScheduler;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;

@Component
public class StatusAcknowledgment {
    private final Random random = new Random();
    private final JobRepository jobRepository;
    private final JobScheduler jobScheduler;
    public StatusAcknowledgment(JobRepository jobRepository, JobScheduler jobScheduler) {
        this.jobRepository = jobRepository;
        this.jobScheduler = jobScheduler;
    }

    @KafkaListener(
            topics = "#{'${app.kafka.topics.ack}'}",
            groupId = "jfc-ingestion-consumer",
            containerFactory = "acknowledgmentListenerContainerFactory"
    )
    public void onMessage(AcknowledgementEvent event) {
        try {
            handleAcknowledgmentEvent(event);
        } catch (Exception e) {
        }
    }
    private void handleAcknowledgmentEvent(AcknowledgementEvent event) throws JsonProcessingException {
        try {
            int sleepMs = 7000;
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        AcknowledgementPayload payload = event.getPayload();
        String eventId = payload.getOriginalEventId();
        String status =payload.getStatus();

        Optional<JobEntity> jobEntity = jobRepository.findById(eventId);
        if (jobEntity.isEmpty()) {
            System.out.println(eventId);
            System.out.println("job not found");
        }
        JobEntity job = jobEntity.get();
        job.setStatus(JobStatus.valueOf(status));
        jobRepository.save(job);
        jobScheduler.dispatchJobs();
    }
}
