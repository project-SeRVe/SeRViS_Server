package horizon.SeRVe.core.repository;

import horizon.SeRVe.core.entity.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, String> {

    Optional<Scenario> findByPromptHash(String promptHash);
}
