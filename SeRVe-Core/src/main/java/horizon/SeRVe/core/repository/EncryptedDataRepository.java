package horizon.SeRVe.core.repository;

import horizon.SeRVe.core.entity.Task;
import horizon.SeRVe.core.entity.EncryptedData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EncryptedDataRepository extends JpaRepository<EncryptedData, String> {
    Optional<EncryptedData> findByTask(Task task);
}
