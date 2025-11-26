package horizon.SeRVe.controller;

import com.google.crypto.tink.KeysetHandle;
import horizon.SeRVe.dto.security.ClientPublicKeyRequest;
import horizon.SeRVe.dto.security.ServerKeyResponse;
import horizon.SeRVe.security.crypto.CryptoManager;
import horizon.SeRVe.security.crypto.KeyExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
@Slf4j // lombok 로깅
public class SecurityController {
    
    // Lombok Plugin 설치해야 함 !! 중요
    private final KeyExchangeService keyExchangeService;
    private final CryptoManager cryptoManager;

    // [API] 핸드셰이크: 클라이언트 공개키 수신 -> 서버 AES 키 암호화 전송
    @PostMapping("/handshake")
    public ResponseEntity<ServerKeyResponse> handshake(@RequestBody ClientPublicKeyRequest request) {
        try {
            System.out.println(">>> [Handshake 요청] 클라이언트 공개키 수신됨");

            // 1. 서버: 저장소용 AES 키 생성 (나중엔 DB에서 꺼내오는 로직으로 대체)
            KeysetHandle serverAesKey = cryptoManager.generateAesKey();

            // 2. 서버: 클라이언트의 공개키로 AES 키를 포장(Wrap)
            byte[] wrappedKey = keyExchangeService.wrapAesKey(serverAesKey, request.getPublicKeyJson());

            // 3. 응답: 포장된 키 전송
            System.out.println(">>> [Handshake 응답] 암호화된 AES 키 전송 완료");
            return ResponseEntity.ok(new ServerKeyResponse(wrappedKey));
        } catch (Exception e) {
            log.error("핸드셰이크 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}