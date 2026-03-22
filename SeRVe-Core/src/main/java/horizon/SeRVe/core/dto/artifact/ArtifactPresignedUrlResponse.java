package horizon.SeRVe.core.dto.artifact;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArtifactPresignedUrlResponse {
    private String artifactId;
    private String presignedUrl;  // S3 GET URL (15분 유효) — 이 URL로 직접 다운로드
}
