package horizon.SeRVe.core.dto.artifact;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactUploadRequest {

    // Scenario 식별 (없으면 신규 생성)
    private String promptText;

    // S3 objectKey 생성에 사용 ({teamId}/{scenarioId}/{demoId}/{filename})
    private String teamId;
    private String filename;

    // Demo 메타데이터 (optional)
    private Integer numSteps;
    private Integer stateDim;
    private Integer actionDim;
    private Integer imageH;
    private Integer imageW;
    private Integer embedDim;
    private String embedModelId;

    // Artifact 메타데이터
    private String kind;
    private String sha256;
    private Long size;
    private String artifactVersion;
    private String encAlgo;
    private String nonce;
    private String dekWrappedByKek;
    private String kekVersion;
}
