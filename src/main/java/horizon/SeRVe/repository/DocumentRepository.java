package horizon.SeRVe.repository;

import horizon.SeRVe.entity.Document;
import horizon.SeRVe.entity.Team;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // 목록 조회 시 업로더 정보 함께 로딩 (N+1 방지)
    @EntityGraph(attributePaths = {"uploader"})
    List<Document> findAllByTeam(Team team); // 기존: findAllByTeamRepository

    Optional<Document> findByDocumentId(String documentId);

    // 같은 이름이면 버전을 올리고 없으면 새로 만드는 로직
    Optional<Document> findByTeamAndOriginalFileName(Team team, String originalFileName);
}
