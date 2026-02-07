package horizon.SeRVe.auth.service;

import horizon.SeRVe.auth.dto.*;
import horizon.SeRVe.auth.entity.User;
import horizon.SeRVe.auth.repository.UserRepository;
import horizon.SeRVe.auth.feign.TeamServiceClient;
import horizon.SeRVe.common.dto.feign.EdgeNodeAuthResponse;
import horizon.SeRVe.common.security.jwt.JwtTokenProvider;
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
    private final TeamServiceClient teamServiceClient;

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

    // 6. 로봇 로그인 (EdgeNode는 Team 서비스에서 Feign으로 조회)
    public LoginResponse robotLogin(RobotLoginRequest req) {
        EdgeNodeAuthResponse robot = teamServiceClient.getEdgeNodeBySerial(req.getSerialNumber());
        if (robot == null) {
            throw new IllegalArgumentException("등록되지 않은 기기입니다.");
        }

        if (!passwordEncoder.matches(req.getApiToken(), robot.getHashedToken())) {
            throw new IllegalArgumentException("API 토큰이 유효하지 않습니다.");
        }

        // 로봇용 토큰 발급 (userId 자리에 nodeId, email 자리에 serialNumber 사용)
        String accessToken = jwtTokenProvider.createToken(robot.getNodeId(), robot.getSerialNumber());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .userId(robot.getNodeId())
                .email(robot.getSerialNumber()) // 이메일 필드에 시리얼 번호 담음
                .encryptedPrivateKey("") // 로봇은 개인키를 서버에 백업하지 않음 (로컬 TPM 관리 가정)
                .build();
    }
}
