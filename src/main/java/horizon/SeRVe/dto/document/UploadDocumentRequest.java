package horizon.SeRVe.dto.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UploadDocumentRequest {
    private String fileName;
    private String fileType;
    private String encryptedBlob; // Base64 String
}