package horizon.SeRVe.core.controller;

import horizon.SeRVe.core.dto.artifact.ArtifactCreateRequest;
import horizon.SeRVe.core.dto.artifact.ArtifactResponse;
import horizon.SeRVe.core.service.ArtifactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService artifactService;

    // Artifact 등록 (S3 업로드 + 메타데이터 저장)
    @PostMapping("/api/demos/{demoId}/artifacts")
    public ResponseEntity<ArtifactResponse> createArtifact(
            @PathVariable String demoId,
            @RequestBody ArtifactCreateRequest request) {
        return ResponseEntity.ok(artifactService.createArtifact(demoId, request));
    }

    // Demo의 Artifact 목록 조회
    @GetMapping("/api/demos/{demoId}/artifacts")
    public ResponseEntity<List<ArtifactResponse>> getArtifacts(
            @PathVariable String demoId) {
        return ResponseEntity.ok(artifactService.getArtifacts(demoId));
    }

    // Artifact 단건 조회 (objectKey 반환)
    @GetMapping("/api/artifacts/{artifactId}")
    public ResponseEntity<ArtifactResponse> getArtifact(
            @PathVariable String artifactId) {
        return ResponseEntity.ok(artifactService.getArtifact(artifactId));
    }
}
