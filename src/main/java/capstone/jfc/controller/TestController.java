package capstone.jfc.controller;

import capstone.jfc.model.JobEntity;
import capstone.jfc.model.JobStatus;
import capstone.jfc.repository.JobRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestController {
    private final JobRepository jobRepository;

    public TestController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping("/jobs")
    public List<JobEntity> getJobs(
            @RequestParam(required = false) String toolId,
            @RequestParam(required = false) String status
    ) {
        if (toolId != null && status != null) {
            return jobRepository.findByToolIdAndStatus(toolId, JobStatus.valueOf(status));
        } else if (toolId != null) {
            return jobRepository.findByToolId(toolId);
        } else if (status != null) {
            return jobRepository.findByStatus(JobStatus.valueOf(status));
        } else {
            return jobRepository.findAll();
        }
    }

}
