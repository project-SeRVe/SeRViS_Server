package horizon.SeRVe.core.controller;

import horizon.SeRVe.core.dto.artifact.ArtifactPresignedUrlResponse;
import horizon.SeRVe.core.dto.artifact.ArtifactResponse;
import horizon.SeRVe.core.dto.artifact.ArtifactUploadRequest;
import horizon.SeRVe.core.dto.artifact.ArtifactUploadResponse;
import horizon.SeRVe.core.service.ArtifactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService artifactService;

    // Demo의 Artifact 목록 조회
    @GetMapping("/api/demos/{demoId}/artifacts")
    public ResponseEntity<List<ArtifactResponse>> getArtifacts(
            @PathVariable String demoId) {
        return ResponseEntity.ok(artifactService.getArtifacts(demoId));
    }

    // 업로드 요청: Scenario/Demo 자동 생성 + presigned PUT URL 반환
    // 클라이언트는 응답의 presignedUrl로 S3에 직접 PUT 업로드
    @PostMapping("/api/artifacts/upload-request")
    public ResponseEntity<ArtifactUploadResponse> requestUpload(
            @RequestBody ArtifactUploadRequest request) {
        return ResponseEntity.ok(artifactService.requestUpload(request));
    }

    // 다운로드 presigned GET URL 반환
    // 클라이언트는 응답의 presignedUrl로 S3에서 직접 다운로드
    @GetMapping("/api/artifacts/{artifactId}/presigned-url")
    public ResponseEntity<ArtifactPresignedUrlResponse> getPresignedDownloadUrl(
            @PathVariable String artifactId) {
        return ResponseEntity.ok(artifactService.getPresignedDownloadUrl(artifactId));
    }
}
