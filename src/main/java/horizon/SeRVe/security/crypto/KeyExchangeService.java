package horizon.SeRVe.security.crypto;

import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.HybridEncrypt;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.TinkJsonProtoKeysetFormat;
import org.springframework.stereotype.Service;

@Service
public class KeyExchangeService {

    static {
        try {
            HybridConfig.register();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 1. [클라이언트용] RSA/ECIES 키 쌍 생성
    public KeysetHandle generateClientKeyPair() throws Exception {
        return KeysetHandle.generateNew(KeyTemplates.get("ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM"));
    }

    // 2. [클라이언트 -> 서버] 공개키 추출 (JSON 문자열)
    public String getPublicKeyJson(KeysetHandle privateKeyHandle) throws Exception {
        KeysetHandle publicHandle = privateKeyHandle.getPublicKeysetHandle();
        return TinkJsonProtoKeysetFormat.serializeKeyset(publicHandle, InsecureSecretKeyAccess.get());
    }

    // 3. [서버 동작] 클라이언트 공개키로 "AES 키"를 암호화해서 포장
    public byte[] wrapAesKey(KeysetHandle aesKey, String clientPublicKeyJson) throws Exception {
        KeysetHandle publicHandle = TinkJsonProtoKeysetFormat.parseKeyset(clientPublicKeyJson, InsecureSecretKeyAccess.get());
        HybridEncrypt hybridEncrypt = publicHandle.getPrimitive(HybridEncrypt.class);

        // AES 키를 문자열로 변환 후 암호화
        byte[] aesKeyBytes = TinkJsonProtoKeysetFormat.serializeKeyset(aesKey, InsecureSecretKeyAccess.get()).getBytes();
        return hybridEncrypt.encrypt(aesKeyBytes, null);
    }

    // 4. [클라이언트 동작] 포장된 AES 키를 풀어서 복구
    public KeysetHandle unwrapAesKey(byte[] encryptedAesKey, KeysetHandle clientPrivateKey) throws Exception {
        HybridDecrypt hybridDecrypt = clientPrivateKey.getPrimitive(HybridDecrypt.class);
        byte[] aesKeyBytes = hybridDecrypt.decrypt(encryptedAesKey, null);
        return TinkJsonProtoKeysetFormat.parseKeyset(new String(aesKeyBytes), InsecureSecretKeyAccess.get());
    }
}