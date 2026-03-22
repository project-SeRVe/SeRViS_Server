package horizon.SeRVe.core.dto.artifact;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArtifactUploadResponse {
    private String artifactId;
    private String presignedUrl;  // S3 PUT URL (15분 유효) — 이 URL로 직접 업로드
    private String objectKey;
}
