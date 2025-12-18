package horizon.SeRVe.dto.document;

import horizon.SeRVe.entity.EncryptedData;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EncryptedDataResponse {
    private String docId;
    private byte[] encryptedBlob;
    private int version;

    public static EncryptedDataResponse from(EncryptedData data) {
        return EncryptedDataResponse.builder()
                .docId(data.getDocument().getDocumentId())
                .encryptedBlob(data.getEncryptedBlob())
                .version(data.getVersion())
                .build();
    }
}