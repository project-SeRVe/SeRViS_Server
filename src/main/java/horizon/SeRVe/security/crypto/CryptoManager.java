package horizon.SeRVe.security.crypto;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.aead.AeadConfig;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class CryptoManager {

    static {
        try {
            // Tink 설정 초기화 (앱 실행 시 1회 필수)
            AeadConfig.register();
        } catch (Exception e) {
            throw new RuntimeException("Tink 초기화 실패", e);
        }
    }

    /**
     * [서버용] 새로운 AES-256-GCM 대칭키 생성
     */
    public KeysetHandle generateAesKey() throws Exception {
        return KeysetHandle.generateNew(KeyTemplates.get("AES256_GCM"));
    }

    /**
     * [데이터 봉인] 평문 -> 암호문 (Base64)
     */
    public String encryptData(String plainText, KeysetHandle aesKey) throws Exception {
        Aead aead = aesKey.getPrimitive(Aead.class);
        byte[] ciphertext = aead.encrypt(plainText.getBytes(StandardCharsets.UTF_8), null);
        return Base64.getEncoder().encodeToString(ciphertext);
    }

    /**
     * [데이터 개봉] 암호문 (Base64) -> 평문
     */
    public String decryptData(String base64Ciphertext, KeysetHandle aesKey) throws Exception {
        Aead aead = aesKey.getPrimitive(Aead.class);
        byte[] decoded = Base64.getDecoder().decode(base64Ciphertext);
        byte[] decrypted = aead.decrypt(decoded, null);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}