package horizon.SeRVe.repo_member;

import com.google.crypto.tink.KeysetHandle;
import horizon.SeRVe.dto.member.InviteMemberRequest;
import horizon.SeRVe.dto.member.MemberResponse;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // 테스트 후 DB 롤백 (데이터 오염 방지)
class ServiceTest {

    @Autowired private RepoService repoService;
    @Autowired private MemberService memberService;
    @Autowired private UserRepository userRepository;

    // 기존 보안 모듈
    @Autowired private KeyExchangeService keyExchangeService;
    @Autowired private CryptoManager cryptoManager;

    // 테스트용 데이터
    private User owner;
    private User member;
    private KeysetHandle ownerKeyPair;
    private KeysetHandle memberKeyPair;
    private KeysetHandle teamAesKey;

    @BeforeEach
    void setUp() throws Exception {
        // 1. [사용자 생성] Owner와 Member를 가짜로 DB에 저장
        owner = User.builder()
                .userId("owner-uuid")
                .email("owner@test.com")
                .hashedPassword("hashed")
                .publicKey("ownerPubKey")
                .encryptedPrivateKey("ownerEncPrivKey")
                .build();
        member = User.builder()
                .userId("member-uuid")
                .email("member@test.com")
                .hashedPassword("hashed")
                .publicKey("memberPubKey")
                .encryptedPrivateKey("memberEncPrivKey")
                .build();
        userRepository.save(owner);
        userRepository.save(member);

        // 2. [키 생성] 클라이언트 측 키 쌍 생성 (시뮬레이션)
        ownerKeyPair = keyExchangeService.generateClientKeyPair();
        memberKeyPair = keyExchangeService.generateClientKeyPair();

        // 3. [팀 키 생성] 공유할 AES 키 생성
        teamAesKey = cryptoManager.generateAesKey();
    }

    @Test
    @DisplayName("시나리오: 저장소 생성부터 멤버 초대, 키 공유까지 보안 흐름 완벽 검증")
    void fullSecurityScenarioTest() throws Exception {
        System.out.println("=== [TEST START] Service Layer Security Integration ===");

        // --- Step 1: 저장소 생성 (Owner) ---
        System.out.println("1. [Owner] 자신의 공개키로 팀 키를 암호화하여 저장소 생성 요청");

        String ownerPublicKey = keyExchangeService.getPublicKeyJson(ownerKeyPair);
        byte[] wrappedKeyForOwner = keyExchangeService.wrapAesKey(teamAesKey, ownerPublicKey);
        String encryptedKeyOwner = Base64.getEncoder().encodeToString(wrappedKeyForOwner);

        // RepoService 호출 (String 파라미터 4개 버전)
        String teamId = repoService.createRepository( // 기존: repoId
                "Top Secret Project",
                "Classified Documents",
                owner.getUserId(),
                encryptedKeyOwner
        );

        assertNotNull(teamId);
        System.out.println("   >> 저장소 생성 성공! ID: " + teamId);


        // --- Step 2: 멤버 초대 (Owner -> Member) ---
        System.out.println("2. [Owner] Member의 공개키로 팀 키를 재암호화하여 초대 요청");

        String memberPublicKey = keyExchangeService.getPublicKeyJson(memberKeyPair);
        byte[] wrappedKeyForMember = keyExchangeService.wrapAesKey(teamAesKey, memberPublicKey);
        String encryptedKeyMember = Base64.getEncoder().encodeToString(wrappedKeyForMember);

        // MemberService 호출
        InviteMemberRequest inviteReq = new InviteMemberRequest(member.getEmail(), encryptedKeyMember);
        memberService.inviteMember(teamId, inviteReq); // 기존: repoId

        // 검증: 멤버가 잘 추가되었는지 확인
        List<MemberResponse> members = memberService.getMembers(teamId); // 기존: repoId
        assertEquals(2, members.size()); // Owner + Member
        System.out.println("   >> 멤버 초대 성공! 현재 멤버 수: " + members.size());


        // --- Step 3: 키 조회 및 복호화 (Member) ---
        System.out.println("3. [Member] 서버에서 암호화된 키를 받아 복호화 시도");

        // MemberService를 통해 키 조회
        String retrievedEncryptedKey = repoService.getTeamKey(teamId, member.getUserId()); // 기존: repoId

        // Member의 개인키로 복호화 (Unwrap)
        byte[] decodedBytes = Base64.getDecoder().decode(retrievedEncryptedKey);
        KeysetHandle restoredKey = keyExchangeService.unwrapAesKey(decodedBytes, memberKeyPair);

        assertNotNull(restoredKey);
        System.out.println("   >> 키 복호화 성공! (원본 AES 키 획득)");


        // --- Step 4: 실제 데이터 암복호화 정합성 검사 ---
        System.out.println("4. [Data] 복구된 키로 데이터 암복호화 테스트");

        String secretMessage = "This is a strictly confidential blueprint.";

        // A. 원본 키로 암호화
        String cipherText = cryptoManager.encryptData(secretMessage, teamAesKey);

        // B. 복구된 키로 복호화
        String decryptedMessage = cryptoManager.decryptData(cipherText, restoredKey);

        assertEquals(secretMessage, decryptedMessage);
        System.out.println("   >> 데이터 일치 확인: " + decryptedMessage);
        System.out.println("=== [TEST PASS] 모든 기능 정상 작동 ===");
    }
}
