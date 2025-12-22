package horizon.SeRVe.repository;

import horizon.SeRVe.entity.EdgeNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EdgeNodeRepository extends JpaRepository<EdgeNode, String> {
    Optional<EdgeNode> findBySerialNumber(String serialNumber);
}