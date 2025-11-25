package horizon.SeRVe.dto.security;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class ServerKeyResponse {
    private byte[] encryptedAesKey; // 클라이언트 공개키로 잠긴 AES 키
}