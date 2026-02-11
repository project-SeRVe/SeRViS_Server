package horizon.SeRVe.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import horizon.SeRVe.auth.dto.LoginResponse;
import horizon.SeRVe.auth.dto.PasswordResetRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 클라이언트(client-robot) SDK와의 호환성 검증 테스트
 */
class ClientCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("비밀번호 재설정 - newEncryptedPrivateKey 없이 역직렬화 가능")
    void passwordReset_withoutEncryptedPrivateKey() throws Exception {
        // 클라이언트가 보내는 형식: {email, newPassword} (newEncryptedPrivateKey 없음)
        String clientJson = """
                {"email": "test@test.com", "newPassword": "newpass123"}
                """;

        PasswordResetRequest request = objectMapper.readValue(clientJson, PasswordResetRequest.class);

        assertEquals("test@test.com", request.getEmail());
        assertEquals("newpass123", request.getNewPassword());
        assertNull(request.getNewEncryptedPrivateKey());
    }

    @Test
    @DisplayName("비밀번호 재설정 - newEncryptedPrivateKey 포함 시에도 정상 동작")
    void passwordReset_withEncryptedPrivateKey() throws Exception {
        String clientJson = """
                {"email": "test@test.com", "newPassword": "newpass123", "newEncryptedPrivateKey": "enc-key-data"}
                """;

        PasswordResetRequest request = objectMapper.readValue(clientJson, PasswordResetRequest.class);

        assertEquals("enc-key-data", request.getNewEncryptedPrivateKey());
    }

    @Test
    @DisplayName("로그인 응답 - 클라이언트가 기대하는 필드명 포함")
    void loginResponse_containsExpectedFields() throws Exception {
        // 클라이언트가 기대하는 필드: accessToken, userId, email, encryptedPrivateKey
        LoginResponse response = LoginResponse.builder()
                .accessToken("jwt-token-123")
                .userId("user-uuid-456")
                .email("test@test.com")
                .encryptedPrivateKey("enc-priv-key")
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"accessToken\""));
        assertTrue(json.contains("\"userId\""));
        assertTrue(json.contains("\"email\""));
        assertTrue(json.contains("\"encryptedPrivateKey\""));
    }
}
