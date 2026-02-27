package horizon.SeRVe.core.dto.task;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UploadTaskRequest {
    private String fileName;
    private String fileType;
    private String encryptedBlob; // Base64 String
}
