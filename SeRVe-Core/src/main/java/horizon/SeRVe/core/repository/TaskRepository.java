package horizon.SeRVe.core.repository;

import horizon.SeRVe.core.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByTeamId(String teamId);

    Optional<Task> findByTaskId(String taskId);

    Optional<Task> findByTeamIdAndOriginalFileName(String teamId, String originalFileName);

    List<Task> findAllByTaskIdIn(List<String> taskIds);
}
