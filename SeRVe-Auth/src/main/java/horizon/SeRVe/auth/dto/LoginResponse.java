package horizon.SeRVe.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private String userId;
    private String email;
    private String encryptedPrivateKey; // 클라이언트 로컬 복구용
}
