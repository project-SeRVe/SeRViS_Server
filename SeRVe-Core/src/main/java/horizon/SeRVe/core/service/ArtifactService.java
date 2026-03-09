package horizon.SeRVe.core.service;

import horizon.SeRVe.core.dto.artifact.ArtifactCreateRequest;
import horizon.SeRVe.core.dto.artifact.ArtifactResponse;
import horizon.SeRVe.core.entity.Artifact;
import horizon.SeRVe.core.entity.Demo;
import horizon.SeRVe.core.repository.ArtifactRepository;
import horizon.SeRVe.core.repository.DemoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtifactService {

    private final ArtifactRepository artifactRepository;
    private final DemoRepository demoRepository;
    private final S3StorageService s3StorageService;

    // Artifact 등록: S3 업로드 후 메타데이터 저장
    // objectKey 형식: {teamId}/{scenarioId}/{demoId}/{filename}
    @Transactional
    public ArtifactResponse createArtifact(String demoId, ArtifactCreateRequest request) {
        Demo demo = demoRepository.findById(demoId)
                .orElseThrow(() -> new IllegalArgumentException("Demo를 찾을 수 없습니다."));

        String scenarioId = demo.getScenario().getScenarioId();

        // 1. S3 objectKey 생성 (수정지시서 형식 준수)
        String objectKey = request.getTeamId()
                + "/" + scenarioId
                + "/" + demoId
                + "/" + request.getFilename();

        // 2. 바이너리 디코딩 후 S3 업로드 (기존 업로드 패턴과 동일)
        byte[] blobData = Base64.getDecoder().decode(request.getEncryptedData());
        s3StorageService.upload(objectKey, blobData);

        // 3. Artifact 메타데이터 DB 저장
        Artifact artifact = Artifact.builder()
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
                .build();

        return ArtifactResponse.from(artifactRepository.save(artifact));
    }

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

    // Artifact 단건 조회 (objectKey 포함)
    @Transactional(readOnly = true)
    public ArtifactResponse getArtifact(String artifactId) {
        return artifactRepository.findById(artifactId)
                .map(ArtifactResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Artifact를 찾을 수 없습니다."));
    }
}
