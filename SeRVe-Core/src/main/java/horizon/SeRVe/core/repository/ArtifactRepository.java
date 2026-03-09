package horizon.SeRVe.core.repository;

import horizon.SeRVe.core.entity.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, String> {

    List<Artifact> findByDemo_DemoId(String demoId);
}
