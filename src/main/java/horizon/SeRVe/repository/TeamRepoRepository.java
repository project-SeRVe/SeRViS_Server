package horizon.SeRVe.repository;

import horizon.SeRVe.entity.TeamRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TeamRepoRepository extends JpaRepository<TeamRepository, Long> {
    Optional<TeamRepository> findByName(String name);

    /**
     * [추가] 내가 소유자(Owner)로 있는 저장소 목록 조회
     * 기존 TeamRepository 엔티티의 String ownerId 필드를 활용.
     */
    List<TeamRepository> findAllByOwnerId(String ownerId);

    // repoId로 단건 조회 (String 타입 식별자 사용 시)
    java.util.Optional<TeamRepository> findByRepoId(String repoId);
}