package horizon.SeRVe.core.dto.artifact;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactCreateRequest {
    private String teamId;          // S3 objectKey 생성에 사용 ({teamId}/{scenarioId}/{demoId}/{filename})
    private String kind;            // "processed" 또는 "raw"
    private String encryptedData;   // Base64 인코딩된 암호화 바이너리 (기존 업로드 패턴과 동일)
    private String filename;        // S3 저장 파일명 (e.g., "processed_demo_v1.npz.enc")

    // 무결성 및 암호화 메타데이터
    private String sha256;
    private Long size;
    private String artifactVersion;
    private String encAlgo;
    private String nonce;
    private String dekWrappedByKek;
    private String kekVersion;
}
