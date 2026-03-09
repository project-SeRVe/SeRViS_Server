package horizon.SeRVe.core.dto.task;

import horizon.SeRVe.core.entity.EncryptedData;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EncryptedDataResponse {
    private Long id;
    private String objectKey; // S3 key. 클라이언트가 S3에서 직접 다운로드
    private int version;

    public static EncryptedDataResponse from(EncryptedData data) {
        return EncryptedDataResponse.builder()
                .id(data.getTask().getId())
                .objectKey(data.getObjectKey())
                .version(data.getVersion())
                .build();
    }
}
