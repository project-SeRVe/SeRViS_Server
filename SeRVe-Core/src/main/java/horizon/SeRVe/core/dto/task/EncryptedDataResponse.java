package horizon.SeRVe.core.dto.task;

import horizon.SeRVe.core.entity.EncryptedData;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EncryptedDataResponse {
    private Long id;
    private byte[] content;
    private int version;

    public static EncryptedDataResponse from(EncryptedData data) {
        return EncryptedDataResponse.builder()
                .id(data.getTask().getId())
                .content(data.getEncryptedBlob())
                .version(data.getVersion())
                .build();
    }
}
