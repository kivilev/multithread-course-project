package dev.sorokin.dao;

import dev.sorokin.domain.TaskEntity;
import dev.sorokin.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaskJpaRepository extends JpaRepository<TaskEntity, UUID> {
    @Query(
            value = """
                                select * 
                                  from tasks t 
                                 where t.status = :taskStatus
                                    or (status = :retryStatus and next_attempt_at <= :now)
                                    or (status = :processingStatus and next_attempt_at <= :now)       
                                  limit :pickBatchSize
                                 for update skip locked
                    """,
            nativeQuery = true
    )
    List<TaskEntity> pickAllUnprocessedTasksWithLocking(TaskStatus taskStatus, TaskStatus retryStatus, TaskStatus processingStatus, Instant now, int pickBatchSize);
}
