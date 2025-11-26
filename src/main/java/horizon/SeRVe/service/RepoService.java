package horizon.SeRVe.service;

import horizon.SeRVe.entity.TeamRepository;
import horizon.SeRVe.repository.TeamRepoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RepoService {

    private final TeamRepoRepository teamRepoRepository;

    // 저장소 생성 로직
    @Transactional
    public Long createRepository(String name, String description, String ownerId) {
        // 1. 중복 이름 체크 (선택 사항)
        if (teamRepoRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 저장소 이름입니다.");
        }

        // 2. 저장
        TeamRepository repo = new TeamRepository(name, description, ownerId);
        TeamRepository saved = teamRepoRepository.save(repo);

        return saved.getId();
    }
}