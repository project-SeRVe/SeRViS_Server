package horizon.SeRVe.core.service;

import horizon.SeRVe.core.dto.artifact.ArtifactPresignedUrlResponse;
import horizon.SeRVe.core.dto.artifact.ArtifactResponse;
import horizon.SeRVe.core.dto.artifact.ArtifactUploadRequest;
import horizon.SeRVe.core.dto.artifact.ArtifactUploadResponse;
import horizon.SeRVe.core.entity.Artifact;
import horizon.SeRVe.core.entity.Demo;
import horizon.SeRVe.core.entity.Scenario;
import horizon.SeRVe.core.repository.ArtifactRepository;
import horizon.SeRVe.core.repository.DemoRepository;
import horizon.SeRVe.core.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtifactService {

    private final ArtifactRepository artifactRepository;
    private final DemoRepository demoRepository;
    private final ScenarioRepository scenarioRepository;
    private final S3StorageService s3StorageService;

    // Demo의 Artifact 목록 조회
    @Transactional(readOnly = true)
    public List<ArtifactResponse> getArtifacts(String demoId) {
        if (!demoRepository.existsById(demoId)) {
            throw new IllegalArgumentException("Demo를 찾을 수 없습니다.");
        }
        return artifactRepository.findByDemo_DemoId(demoId).stream()
                .map(ArtifactResponse::from)
                .collect(Collectors.toList());
    }

    // 업로드 요청: Scenario 조회/생성 → Demo 생성 → Artifact 메타 저장 → presigned PUT URL 반환
    @Transactional
    public ArtifactUploadResponse requestUpload(ArtifactUploadRequest request) {
        // 1. Scenario 조회 or 생성 (promptHash 기준 중복 방지)
        String hash = sha256(request.getPromptText());
        Scenario scenario = scenarioRepository.findByPromptHash(hash)
                .orElseGet(() -> scenarioRepository.save(Scenario.builder()
                        .scenarioId(UUID.randomUUID().toString())
                        .promptText(request.getPromptText())
                        .promptHash(hash)
                        .build()));

        // 2. Demo 생성
        Demo demo = demoRepository.save(Demo.builder()
                .demoId(UUID.randomUUID().toString())
                .scenario(scenario)
                .numSteps(request.getNumSteps())
                .stateDim(request.getStateDim())
                .actionDim(request.getActionDim())
                .imageH(request.getImageH())
                .imageW(request.getImageW())
                .embedDim(request.getEmbedDim())
                .embedModelId(request.getEmbedModelId())
                .build());

        // 3. objectKey 생성: {teamId}/{scenarioId}/{demoId}/{filename}
        String objectKey = request.getTeamId()
                + "/" + scenario.getScenarioId()
                + "/" + demo.getDemoId()
                + "/" + request.getFilename();

        // 4. Artifact 메타데이터 DB 저장
        Artifact artifact = artifactRepository.save(Artifact.builder()
                .artifactId(UUID.randomUUID().toString())
                .demo(demo)
                .kind(request.getKind() != null ? request.getKind() : "processed")
                .objectKey(objectKey)
                .sha256(request.getSha256())
                .size(request.getSize())
                .version(request.getArtifactVersion())
                .encAlgo(request.getEncAlgo())
                .nonce(request.getNonce())
                .dekWrappedByKek(request.getDekWrappedByKek())
                .kekVersion(request.getKekVersion())
                .build());

        // 5. presigned PUT URL 발급
        String presignedUrl = s3StorageService.generatePresignedUploadUrl(objectKey);

        return ArtifactUploadResponse.builder()
                .artifactId(artifact.getArtifactId())
                .presignedUrl(presignedUrl)
                .objectKey(objectKey)
                .build();
    }

    // 다운로드 presigned GET URL 반환
    @Transactional(readOnly = true)
    public ArtifactPresignedUrlResponse getPresignedDownloadUrl(String artifactId) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new IllegalArgumentException("Artifact를 찾을 수 없습니다."));
        return ArtifactPresignedUrlResponse.builder()
                .artifactId(artifactId)
                .presignedUrl(s3StorageService.generatePresignedUrl(artifact.getObjectKey()))
                .build();
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
