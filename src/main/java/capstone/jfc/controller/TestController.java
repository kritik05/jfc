package capstone.jfc.controller;

import capstone.jfc.model.JobEntity;
import capstone.jfc.repository.JobRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestController {
    private final JobRepository jobRepository;

    public TestController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * Returns all jobs in the DB, so you can see the statuses.
     * Example usage: http://localhost:8080/jobs
     */
    @GetMapping("/jobs")
    public List<JobEntity> getAllJobs() {
        return jobRepository.findAll();
    }
}
