package horizon.SeRVe.repository;

import horizon.SeRVe.entity.Document;
import horizon.SeRVe.entity.EncryptedData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EncryptedDataRepository extends JpaRepository<EncryptedData, String> {
    Optional<EncryptedData> findByDocument(Document document);
}