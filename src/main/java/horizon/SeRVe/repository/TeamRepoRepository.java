package horizon.SeRVe.repository;

import horizon.SeRVe.entity.TeamRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TeamRepoRepository extends JpaRepository<TeamRepository, Long> {
    Optional<TeamRepository> findByName(String name);
}