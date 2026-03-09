package horizon.SeRVe.core.dto.artifact;

import horizon.SeRVe.core.entity.Artifact;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ArtifactResponse {
    private String artifactId;
    private String demoId;
    private String kind;
    private String objectKey;       // S3 key. 클라이언트가 이 key로 S3에서 직접 다운로드
    private String sha256;
    private Long size;
    private String version;
    private String encAlgo;
    private String nonce;
    private String dekWrappedByKek;
    private String kekVersion;
    private LocalDateTime createdAt;

    public static ArtifactResponse from(Artifact artifact) {
        return ArtifactResponse.builder()
                .artifactId(artifact.getArtifactId())
                .demoId(artifact.getDemo().getDemoId())
                .kind(artifact.getKind())
                .objectKey(artifact.getObjectKey())
                .sha256(artifact.getSha256())
                .size(artifact.getSize())
                .version(artifact.getVersion())
                .encAlgo(artifact.getEncAlgo())
                .nonce(artifact.getNonce())
                .dekWrappedByKek(artifact.getDekWrappedByKek())
                .kekVersion(artifact.getKekVersion())
                .createdAt(artifact.getCreatedAt())
                .build();
    }
}
