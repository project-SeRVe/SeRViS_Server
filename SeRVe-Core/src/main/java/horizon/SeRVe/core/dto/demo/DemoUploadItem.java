package horizon.SeRVe.core.dto.demo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DemoUploadItem {
    private int demoIndex;
    private String encryptedBlob; // Base64 String
}
