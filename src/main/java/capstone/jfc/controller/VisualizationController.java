package capstone.jfc.controller;

import capstone.jfc.model.JobEntity;
import capstone.jfc.model.JobStatus;
import capstone.jfc.model.JobCategory;
import capstone.jfc.repository.JobRepository;
import capstone.jfc.repository.JobCategoryRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class VisualizationController {

    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;

    public VisualizationController(JobRepository jobRepository, JobCategoryRepository jobCategoryRepository) {
        this.jobRepository = jobRepository;
        this.jobCategoryRepository = jobCategoryRepository;
    }

    // Returns all jobs with status = NEW
    @GetMapping("/jobs/new")
    public List<JobEntity> getNewJobs() {
        return jobRepository.findByStatus(JobStatus.NEW);
    }

    // Returns all jobs with status = IN_PROGRESS
    @GetMapping("/jobs/inprogress")
    public List<JobEntity> getInProgressJobs() {
        return jobRepository.findByStatus(JobStatus.IN_PROGRESS);
    }

    // (Optional) Return the tool configs so we can see each tool's concurrency limit
    @GetMapping("/jobscategory")
    public List<JobCategory> getAllJobsCategory() {
        return jobCategoryRepository.findAll();
    }
}