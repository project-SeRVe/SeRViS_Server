package horizon.SeRVe.service;

import horizon.SeRVe.dto.auth.*;
import horizon.SeRVe.entity.User;
import horizon.SeRVe.repository.UserRepository;
import horizon.SeRVe.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 1. 회원가입
    @Transactional
    public void signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .email(req.getEmail())
                .hashedPassword(passwordEncoder.encode(req.getPassword()))
                .publicKey(req.getPublicKey())
                .encryptedPrivateKey(req.getEncryptedPrivateKey())
                .build();

        userRepository.save(user);
    }

    // 2. 로그인
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(req.getPassword(), user.getHashedPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtTokenProvider.createToken(user.getUserId(), user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .userId(user.getUserId())
                .email(user.getEmail())
                .encryptedPrivateKey(user.getEncryptedPrivateKey())
                .build();
    }

    // 3. 비밀번호 재설정
    @Transactional
    public void resetPassword(PasswordResetRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        String newEncodedPassword = passwordEncoder.encode(req.getNewPassword());
        user.updatePassword(newEncodedPassword, req.getNewEncryptedPrivateKey());
    }

    // 4. 회원 탈퇴
    @Transactional
    public void withdraw(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));
        userRepository.delete(user);
    }

    // 5. 공개키 조회 (멤버 초대 시 사용)
    public String getPublicKey(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자를 찾을 수 없습니다."));
        return user.getPublicKey();
    }
}
