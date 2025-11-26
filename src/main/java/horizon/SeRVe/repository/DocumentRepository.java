package horizon.SeRVe.repository;

import horizon.SeRVe.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}