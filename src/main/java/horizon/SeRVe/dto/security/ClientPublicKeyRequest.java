package horizon.SeRVe.dto.security;

import lombok.Data;

@Data
public class ClientPublicKeyRequest {
    private String publicKeyJson; // 클라이언트의 RSA/ECIES 공개키
}