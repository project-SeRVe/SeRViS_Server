package horizon.SeRVe.controller;

import horizon.SeRVe.dto.repo.RepoResponse;
import horizon.SeRVe.service.RepoService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class RepoController {

    private final RepoService repoService;

    // DTO (내부 클래스로 간단히 정의)
    @Data
    public static class CreateRepoRequest {
        private String name;
        private String description;
        private String ownerId; // 임시: 원래는 토큰에서 꺼내야 함
        private String encryptedTeamKey;
    }

    // 저장소 생성
    @PostMapping
    public ResponseEntity<String> createRepository(@RequestBody CreateRepoRequest request) {
        String repoId = repoService.createRepository(
                request.getName(),
                request.getDescription(),
                request.getOwnerId(),
                request.getEncryptedTeamKey()
        );
        return ResponseEntity.ok(repoId);
    }

    // 내 저장소 목록 조회
    // (GET 요청이라 Body가 없으므로, 임시로 파라미터로 userId를 받습니다)
    @GetMapping
    public ResponseEntity<List<RepoResponse>> getRepositories(@RequestParam String userId) {
        List<RepoResponse> responses = repoService.getMyRepos(userId);
        return ResponseEntity.ok(responses);
    }

    // 3. 팀 키 조회
    @GetMapping("/{teamId}/keys")
    public ResponseEntity<String> getTeamKey(
            @PathVariable String teamId, // 기존: repoId
            @RequestParam String userId) {

        String encryptedTeamKey = repoService.getTeamKey(teamId, userId);
        return ResponseEntity.ok(encryptedTeamKey);
    }

    // 4. 저장소 삭제
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteRepository(
            @PathVariable String teamId, // 기존: repoId
            @RequestParam String userId) {

        repoService.deleteRepo(teamId, userId);
        return ResponseEntity.ok().build();
    }
}
