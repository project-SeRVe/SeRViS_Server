package horizon.SeRVe.team.controller;

import horizon.SeRVe.team.dto.repo.CreateRepoRequest;
import horizon.SeRVe.team.dto.repo.RepoResponse;
import horizon.SeRVe.team.service.RepoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class RepoController {

    private final RepoService repoService;

    // 저장소 생성 (ownerId는 JWT에서 추출)
    @PostMapping
    public ResponseEntity<Map<String, String>> createRepository(
            Authentication authentication,
            @RequestBody CreateRepoRequest request) {
        String userId = (String) authentication.getPrincipal();
        String repoId = repoService.createRepository(
                request.getName(),
                request.getDescription(),
                userId,
                request.getEncryptedTeamKey()
        );
        return ResponseEntity.ok(Map.of("id", repoId, "role", "ADMIN"));
    }

    // 내 저장소 목록 조회
    @GetMapping
    public ResponseEntity<List<RepoResponse>> getRepositories(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<RepoResponse> responses = repoService.getMyRepos(userId);
        return ResponseEntity.ok(responses);
    }

    // 팀 키 조회
    @GetMapping("/{teamId}/keys")
    public ResponseEntity<String> getTeamKey(
            @PathVariable String teamId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        String encryptedTeamKey = repoService.getTeamKey(teamId, userId);
        return ResponseEntity.ok(encryptedTeamKey);
    }

    // 저장소 삭제
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteRepository(
            @PathVariable String teamId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        repoService.deleteRepo(teamId, userId);
        return ResponseEntity.ok().build();
    }
}
