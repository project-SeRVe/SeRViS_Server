package horizon.SeRVe.core.repository;

import horizon.SeRVe.core.entity.Demo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DemoRepository extends JpaRepository<Demo, String> {

    List<Demo> findByScenario_ScenarioId(String scenarioId);
}
