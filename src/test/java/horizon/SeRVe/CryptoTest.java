package horizon.SeRVe;

import com.google.crypto.tink.KeysetHandle;
import horizon.SeRVe.security.crypto.CryptoManager;
import horizon.SeRVe.security.crypto.KeyExchangeService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CryptoTest {

    @Test
    void Zerotrust_Encrypt_Scenario_Test() throws Exception {
        CryptoManager cryptoManager = new CryptoManager();
        KeyExchangeService keyExchangeService = new KeyExchangeService();

        System.out.println("=== 1. [클라이언트] 접속 및 키 생성 ===");
        KeysetHandle clientKeyPair = keyExchangeService.generateClientKeyPair();
        String publicKey = keyExchangeService.getPublicKeyJson(clientKeyPair);
        System.out.println("클라이언트 공개키 전송: " + publicKey.substring(0, 30) + "...");

        System.out.println("\n=== 2. [서버] 저장소용 AES 키 생성 및 공유 ===");
        KeysetHandle serverAesKey = cryptoManager.generateAesKey();
        byte[] encryptedAesKey = keyExchangeService.wrapAesKey(serverAesKey, publicKey);
        System.out.println("암호화된 AES 키 블롭 크기: " + encryptedAesKey.length + " bytes");

        System.out.println("\n=== 3. [클라이언트] AES 키 수신 및 복구 ===");
        KeysetHandle restoredAesKey = keyExchangeService.unwrapAesKey(encryptedAesKey, clientKeyPair);
        assertNotNull(restoredAesKey);

        System.out.println("\n=== 4. [클라이언트] 데이터 암호화 전송 ===");
        String secretData = "Top Secret Vector Data: [0.123, 0.999]";
        String cipherText = cryptoManager.encryptData(secretData, restoredAesKey);
        System.out.println("암호문: " + cipherText);

        System.out.println("\n=== 5. [서버] 데이터 수신 및 검증 ===");
        String decryptedData = cryptoManager.decryptData(cipherText, serverAesKey);
        System.out.println("복호화된 데이터: " + decryptedData);

        assertEquals(secretData, decryptedData);
        System.out.println("\n>> 테스트 성공: 키 교환부터 데이터 전송까지 완벽합니다.");
    }
}