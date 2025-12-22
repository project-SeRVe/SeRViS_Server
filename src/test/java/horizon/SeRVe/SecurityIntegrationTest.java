package horizon.SeRVe;

import com.google.crypto.tink.KeysetHandle;
import horizon.SeRVe.dto.member.InviteMemberRequest;
import horizon.SeRVe.dto.repo.CreateRepoRequest;
import horizon.SeRVe.entity.User;
import horizon.SeRVe.repository.UserRepository;
import horizon.SeRVe.security.crypto.CryptoManager;
import horizon.SeRVe.security.crypto.KeyExchangeService;
import horizon.SeRVe.service.MemberService;
import horizon.SeRVe.service.RepoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // 테스트 종료 후 데이터 롤백 (DB 오염 방지)
class SecurityIntegrationTest {

    @Autowired private RepoService repoService;
    @Autowired private MemberService memberService;
    @Autowired private UserRepository userRepository;

    // 기존 보안 모듈 (이 테스트의 핵심 검증 대상)
    @Autowired private KeyExchangeService keyExchangeService;
    @Autowired private CryptoManager cryptoManager;

    // 테스트용 데이터
    private User owner;
    private User member;

    // 시뮬레이션용 클라이언트 키 (DB 저장 X, 로컬 메모리 상에만 존재)
    private KeysetHandle ownerKeyPair;
    private KeysetHandle memberKeyPair;
    private KeysetHandle sharedTeamKey; // 팀 공유 AES 키

    @BeforeEach
    void setUp() throws Exception {
        // 1. [User 준비] DB에 사용자 저장
        String ownerId = UUID.randomUUID().toString();
        String memberId = UUID.randomUUID().toString();

        owner = User.builder()
                .userId(ownerId)
                .email("owner@security.test")
                .hashedPassword("hashed")
                .publicKey("ownerPubKey")
                .encryptedPrivateKey("ownerEncPrivKey")
                .build();
        member = User.builder()
                .userId(memberId)
                .email("member@security.test")
                .hashedPassword("hashed")
                .publicKey("memberPubKey")
                .encryptedPrivateKey("memberEncPrivKey")
                .build();

        userRepository.save(owner);
        userRepository.save(member);

        // 2. [Client Key Gen] 클라이언트 키 쌍 생성 (Tink 활용)
        ownerKeyPair = keyExchangeService.generateClientKeyPair();
        memberKeyPair = keyExchangeService.generateClientKeyPair();

        // 3. [Team Key Gen] 공유할 AES 대칭키 생성
        sharedTeamKey = cryptoManager.generateAesKey();
    }

    @Test
    @DisplayName("통합 검증: 저장소 생성(Controller 파라미터 호환) -> 멤버 초대 -> 키 복구 -> 데이터 암복호화")
    void verifyFullSecurityLifecycle() throws Exception {
        System.out.println(">>> [TEST] 보안 모듈 & 컨트롤러/서비스 로직 통합 테스트 시작");

        // =================================================================
        // STEP 1: [Owner] 저장소 생성 (RepoController 로직 검증)
        // =================================================================

        // 1-1. Owner의 공개키 추출
        String ownerPublicKey = keyExchangeService.getPublicKeyJson(ownerKeyPair);

        // 1-2. 팀 키를 Owner의 공개키로 암호화 (Wrap)
        byte[] wrappedKeyForOwner = keyExchangeService.wrapAesKey(sharedTeamKey, ownerPublicKey);
        String encryptedKeyString = Base64.getEncoder().encodeToString(wrappedKeyForOwner);

        // 1-3. RepoService.createRepository 호출
        // (Controller가 호출하는 방식 그대로 파라미터 전달: ownerId 포함)
        String teamId = repoService.createRepository( // 기존: repoId
                "Secure Vault",
                "Top Secret Logic Check",
                owner.getUserId(),      // Controller에서 request.getOwnerId()로 넘기는 값
                encryptedKeyString      // Controller에서 request.getEncryptedTeamKey()로 넘기는 값
        );

        assertNotNull(teamId);
        System.out.println(">>> 1. 저장소 생성 완료 (ID: " + teamId + ")");


        // =================================================================
        // STEP 2: [Owner -> Member] 멤버 초대 (MemberController 로직 검증)
        // =================================================================

        // 2-1. Member의 공개키 추출
        String memberPublicKey = keyExchangeService.getPublicKeyJson(memberKeyPair);

        // 2-2. 팀 키를 Member의 공개키로 재암호화 (Wrap)
        byte[] wrappedKeyForMember = keyExchangeService.wrapAesKey(sharedTeamKey, memberPublicKey);
        String encryptedKeyForMemberStr = Base64.getEncoder().encodeToString(wrappedKeyForMember);

        // 2-3. InviteMemberRequest DTO 생성 및 초대
        InviteMemberRequest inviteReq = new InviteMemberRequest(member.getEmail(), encryptedKeyForMemberStr);

        // MemberService 호출
        memberService.inviteMember(teamId, inviteReq); // 기존: repoId

        System.out.println(">>> 2. 멤버 초대 완료 (키 공유 성공)");


        // =================================================================
        // STEP 3: [Member] 키 조회 및 복호화 (보안 무결성 검증)
        // =================================================================

        // 3-1. 서버에서 암호화된 팀 키 조회 (RepoService.getTeamKey)
        String retrievedEncryptedKey = repoService.getTeamKey(teamId, member.getUserId()); // 기존: repoId
        assertNotNull(retrievedEncryptedKey);

        // 3-2. Member의 개인키로 복호화 (Unwrap)
        byte[] decodedBytes = Base64.getDecoder().decode(retrievedEncryptedKey);
        KeysetHandle restoredKey = keyExchangeService.unwrapAesKey(decodedBytes, memberKeyPair);

        assertNotNull(restoredKey, "복호화된 키 핸들은 null이 아니어야 함");
        System.out.println(">>> 3. 키 복호화 성공 (Server is Blind 원칙 준수됨)");


        // =================================================================
        // STEP 4: [Data] 실제 데이터 암복호화 (기능 동작 검증)
        // =================================================================

        String originalData = "Nuclear Launch Code: 0000";

        // A. 원본 키로 암호화 (Owner가 수행했다고 가정)
        String cipherText = cryptoManager.encryptData(originalData, sharedTeamKey);

        // B. 복구된 키로 복호화 (Member가 수행)
        String decryptedData = cryptoManager.decryptData(cipherText, restoredKey);

        // 검증
        assertEquals(originalData, decryptedData, "복호화된 데이터가 원본과 일치해야 함");
        System.out.println(">>> 4. 데이터 암복호화 검증 완료: " + decryptedData);
        System.out.println(">>> [SUCCESS] 모든 기능 및 보안 메커니즘 정상 작동");
    }
}
