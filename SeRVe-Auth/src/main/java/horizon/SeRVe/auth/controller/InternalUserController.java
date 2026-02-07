package horizon.SeRVe.auth.controller;

import horizon.SeRVe.auth.entity.User;
import horizon.SeRVe.auth.repository.UserRepository;
import horizon.SeRVe.common.dto.feign.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 서비스 간 통신 전용 내부 API.
 * Team/Core 서비스가 Feign으로 호출합니다.
 * SecurityConfig에서 /internal/** 경로는 인증 없이 허용됩니다.
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository userRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<UserInfoResponse> getUserInfo(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(UserInfoResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .publicKey(user.getPublicKey())
                .build());
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<UserInfoResponse> getUserByEmail(@PathVariable String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(UserInfoResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .publicKey(user.getPublicKey())
                .build());
    }

    @GetMapping("/{userId}/exists")
    public ResponseEntity<Boolean> userExists(@PathVariable String userId) {
        return ResponseEntity.ok(userRepository.existsById(userId));
    }

    @GetMapping("/{userId}/public-key")
    public ResponseEntity<String> getPublicKey(@PathVariable String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return ResponseEntity.ok(user.getPublicKey());
    }
}
