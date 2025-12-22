package horizon.SeRVe.repo_member;

import com.google.crypto.tink.KeysetHandle;
import horizon.SeRVe.dto.member.InviteMemberRequest;
import horizon.SeRVe.dto.repo.CreateRepoRequest;
import horizon.SeRVe.entity.RepositoryMember;
import horizon.SeRVe.entity.RepositoryMemberId;
import horizon.SeRVe.entity.Role;
import horizon.SeRVe.entity.Team;
import horizon.SeRVe.entity.User;
import horizon.SeRVe.security.crypto.CryptoManager;
import horizon.SeRVe.security.crypto.KeyExchangeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class DtoEntitySecurityTest {

    // 기존 보안 모듈 (직접 생성하여 테스트)
    private KeyExchangeService keyExchangeService;
    private CryptoManager cryptoManager;

    @BeforeEach
    void setUp() {
        keyExchangeService = new KeyExchangeService();
        cryptoManager = new CryptoManager();
    }

    @Test
    @DisplayName("DTO와 Entity 구조가 보안 모듈의 키 교환 흐름을 정상적으로 지원하는지 검증")
    void verifySecurityFlowWithDtoAndEntity() throws Exception {

        // 1. [준비] 클라이언트 키 및 팀 키 생성 (기존 모듈 활용)
        KeysetHandle ownerKeyPair = keyExchangeService.generateClientKeyPair();
        String ownerPublicKey = keyExchangeService.getPublicKeyJson(ownerKeyPair);

        KeysetHandle teamAesKey = cryptoManager.generateAesKey(); // 원본 팀 키

        // 2. [DTO 검증] CreateRepoRequest 생성 시뮬레이션
        // Owner가 자신의 공개키로 팀 키를 암호화(Wrap)해서 DTO에 담는 과정
        byte[] wrappedKeyBytes = keyExchangeService.wrapAesKey(teamAesKey, ownerPublicKey);
        String encryptedTeamKey = Base64.getEncoder().encodeToString(wrappedKeyBytes);

        CreateRepoRequest createReq = new CreateRepoRequest("Test Repo", "Desc", encryptedTeamKey);

        assertNotNull(createReq.getEncryptedTeamKey());
        assertEquals(encryptedTeamKey, createReq.getEncryptedTeamKey());
        System.out.println("1. DTO 생성 및 암호화 키 전달 확인 완료");


        // 3. [Entity 검증] DTO -> Entity 데이터 이관 시뮬레이션
        // 실제 Service 코드가 없으므로, 여기서 수동으로 매핑하여 구조적 적합성 확인
        User owner = User.builder()
                .userId("owner-id")
                .email("owner@test.com")
                .hashedPassword("hashed")
                .publicKey("pubKey")
                .encryptedPrivateKey("encPrivKey")
                .build();
        Team team = new Team("Test Repo", "Desc", owner.getUserId()); // 기존: TeamRepository

        // 복합키 생성
        RepositoryMemberId memberId = new RepositoryMemberId(team.getTeamId(), owner.getUserId());

        // Entity 생성 (빌더 패턴 활용)
        RepositoryMember memberEntity = RepositoryMember.builder()
                .id(memberId)
                .team(team) // 기존: teamRepository
                .user(owner)
                .role(Role.ADMIN)
                .encryptedTeamKey(createReq.getEncryptedTeamKey()) // DTO에서 꺼내서 넣음
                .build();

        assertNotNull(memberEntity.getEncryptedTeamKey());
        System.out.println("2. Entity 매핑 및 필드 저장 확인 완료");


        // 4. [호환성 검증] Entity에 저장된 키 복구 테스트
        // 서버에서 꺼낸 encryptedTeamKey를 클라이언트가 복호화할 수 있는지 확인
        String storedEncryptedKey = memberEntity.getEncryptedTeamKey();
        byte[] decodedKeyBytes = Base64.getDecoder().decode(storedEncryptedKey);

        // Owner의 개인키로 복호화 (Unwrap)
        KeysetHandle restoredKey = keyExchangeService.unwrapAesKey(decodedKeyBytes, ownerKeyPair);

        assertNotNull(restoredKey);
        System.out.println("3. Entity에 저장된 키를 정상적으로 복호화(Unwrap) 성공");

        // 5. [데이터 암호화 검증] 복구된 키로 실제 암호화/복호화가 되는지 최종 확인
        String plainText = "Secret Data";
        String cipherText = cryptoManager.encryptData(plainText, restoredKey);
        String decryptedText = cryptoManager.decryptData(cipherText, restoredKey);

        assertEquals(plainText, decryptedText);
        System.out.println("4. 복구된 키로 데이터 암/복호화 검증 완료");
    }

    @Test
    @DisplayName("멤버 초대 DTO(InviteMemberRequest) 보안 흐름 검증")
    void verifyInviteMemberFlow() throws Exception {
        // 1. [준비] 초대할 Member의 키 쌍 생성
        KeysetHandle memberKeyPair = keyExchangeService.generateClientKeyPair();
        String memberPublicKey = keyExchangeService.getPublicKeyJson(memberKeyPair);

        KeysetHandle teamAesKey = cryptoManager.generateAesKey();

        // 2. [DTO 검증] Member의 공개키로 팀 키 암호화하여 Invite DTO 생성
        byte[] wrappedKeyBytes = keyExchangeService.wrapAesKey(teamAesKey, memberPublicKey);
        String encryptedKeyForMember = Base64.getEncoder().encodeToString(wrappedKeyBytes);

        InviteMemberRequest inviteReq = new InviteMemberRequest("member@test.com", encryptedKeyForMember);

        assertEquals("member@test.com", inviteReq.getEmail());

        // 3. [복호화 검증] DTO에 담긴 키를 Member가 풀 수 있는지 확인
        byte[] decoded = Base64.getDecoder().decode(inviteReq.getEncryptedTeamKey());
        KeysetHandle restoredKey = keyExchangeService.unwrapAesKey(decoded, memberKeyPair);

        assertNotNull(restoredKey);
        System.out.println("멤버 초대용 DTO 키 전달 및 복호화 테스트 통과");
    }
}
