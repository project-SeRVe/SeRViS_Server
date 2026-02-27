package horizon.SeRVe.core.repository;

import horizon.SeRVe.core.entity.VectorDemo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VectorDemoRepository extends JpaRepository<VectorDemo, String> {

    List<VectorDemo> findByTaskIdAndIsDeletedOrderByDemoIndexAsc(String taskId, boolean isDeleted);

    List<VectorDemo> findByTaskIdAndVersionGreaterThanOrderByDemoIndexAsc(String taskId, int lastVersion);

    List<VectorDemo> findByTeamIdAndVersionGreaterThanOrderByVersionAsc(String teamId, int lastVersion);

    Optional<VectorDemo> findByTaskIdAndDemoIndex(String taskId, int demoIndex);

    List<VectorDemo> findByTaskId(String taskId);
}
