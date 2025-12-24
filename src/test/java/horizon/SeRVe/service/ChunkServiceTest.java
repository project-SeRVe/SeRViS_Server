package horizon.SeRVe.service;

import horizon.SeRVe.dto.chunk.*;
import horizon.SeRVe.entity.*;
import horizon.SeRVe.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ChunkServiceTest {

    @Autowired
    private ChunkService chunkService;

    @Autowired
    private VectorChunkRepository vectorChunkRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    private User adminUser;
    private User memberUser;
    private User outsider;
    private Team team;
    private String fileName = "설비매뉴얼.pdf";

    @BeforeEach
    void setUp() {
        // 사용자 생성
        adminUser = User.builder()
                .userId(UUID.randomUUID().toString())
                .email("admin@test.com")
                .hashedPassword("password")
                .publicKey("dummy-public-key")
                .encryptedPrivateKey("dummy-encrypted-private-key")
                .build();
        userRepository.save(adminUser);

        memberUser = User.builder()
                .userId(UUID.randomUUID().toString())
                .email("member@test.com")
                .hashedPassword("password")
                .publicKey("dummy-public-key")
                .encryptedPrivateKey("dummy-encrypted-private-key")
                .build();
        userRepository.save(memberUser);

        outsider = User.builder()
                .userId(UUID.randomUUID().toString())
                .email("outsider@test.com")
                .hashedPassword("password")
                .publicKey("dummy-public-key")
                .encryptedPrivateKey("dummy-encrypted-private-key")
                .build();
        userRepository.save(outsider);

        // 팀 생성
        team = new Team("Test Team", "Test Description", adminUser.getUserId());
        teamRepository.save(team);

        // 멤버 추가
        RepositoryMember adminMember = RepositoryMember.builder()
                .id(new RepositoryMemberId(team.getTeamId(), adminUser.getUserId()))
                .team(team)
                .user(adminUser)
                .role(Role.ADMIN)
                .encryptedTeamKey("dummy-encrypted-team-key-admin")
                .build();
        memberRepository.save(adminMember);

        RepositoryMember normalMember = RepositoryMember.builder()
                .id(new RepositoryMemberId(team.getTeamId(), memberUser.getUserId()))
                .team(team)
                .user(memberUser)
                .role(Role.MEMBER)
                .encryptedTeamKey("dummy-encrypted-team-key-member")
                .build();
        memberRepository.save(normalMember);
    }

    @Test
    @DisplayName("ADMIN은 청크를 업로드할 수 있다 (문서 자동 생성)")
    void uploadChunks_byAdmin_success() {
        // Given
        String encodedBlob = Base64.getEncoder().encodeToString("test data".getBytes());
        ChunkUploadItem item1 = new ChunkUploadItem(0, encodedBlob);
        ChunkUploadItem item2 = new ChunkUploadItem(1, encodedBlob);
        ChunkUploadRequest request = new ChunkUploadRequest(fileName, List.of(item1, item2));

        // When
        chunkService.uploadChunks(team.getTeamId(), fileName, adminUser.getUserId(), request);

        // Then: Document가 자동 생성되었는지 확인
        Document document = documentRepository.findByTeamAndOriginalFileName(team, fileName).orElseThrow();
        assertThat(document.getOriginalFileName()).isEqualTo(fileName);

        // Then: 청크가 저장되었는지 확인
        List<VectorChunk> chunks = vectorChunkRepository.findByDocumentId(document.getDocumentId());
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(chunks.get(1).getChunkIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("MEMBER는 청크를 업로드할 수 없다 (ADMIN 권한 필요)")
    void uploadChunks_byMember_throwsException() {
        // Given
        String encodedBlob = Base64.getEncoder().encodeToString("test data".getBytes());
        ChunkUploadRequest request = new ChunkUploadRequest(
                fileName,
                List.of(new ChunkUploadItem(0, encodedBlob))
        );

        // When & Then
        assertThatThrownBy(() -> chunkService.uploadChunks(
                team.getTeamId(), fileName, memberUser.getUserId(), request))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("ADMIN 권한이 필요합니다");
    }

    @Test
    @DisplayName("기존 청크를 업데이트하면 version이 증가한다")
    void uploadChunks_update_incrementsVersion() {
        // Given: 초기 청크 생성
        String encodedBlob1 = Base64.getEncoder().encodeToString("data v1".getBytes());
        ChunkUploadRequest request1 = new ChunkUploadRequest(
                fileName,
                List.of(new ChunkUploadItem(0, encodedBlob1))
        );
        chunkService.uploadChunks(team.getTeamId(), fileName, adminUser.getUserId(), request1);

        Document document = documentRepository.findByTeamAndOriginalFileName(team, fileName).orElseThrow();
        VectorChunk chunk = vectorChunkRepository
                .findByDocumentIdAndChunkIndex(document.getDocumentId(), 0)
                .orElseThrow();
        int initialVersion = chunk.getVersion();

        // When: 동일한 chunk_index로 업데이트
        String encodedBlob2 = Base64.getEncoder().encodeToString("data v2".getBytes());
        ChunkUploadRequest request2 = new ChunkUploadRequest(
                fileName,
                List.of(new ChunkUploadItem(0, encodedBlob2))
        );
        chunkService.uploadChunks(team.getTeamId(), fileName, adminUser.getUserId(), request2);

        // Then
        VectorChunk updatedChunk = vectorChunkRepository
                .findByDocumentIdAndChunkIndex(document.getDocumentId(), 0)
                .orElseThrow();
        assertThat(updatedChunk.getVersion()).isGreaterThan(initialVersion);
        assertThat(new String(updatedChunk.getEncryptedBlob())).isEqualTo("data v2");
    }

    @Test
    @DisplayName("MEMBER는 청크를 다운로드할 수 있다")
    void getChunks_byMember_success() {
        // Given: 청크 생성
        String encodedBlob = Base64.getEncoder().encodeToString("test data".getBytes());
        ChunkUploadRequest request = new ChunkUploadRequest(
                fileName,
                List.of(new ChunkUploadItem(0, encodedBlob))
        );
        chunkService.uploadChunks(team.getTeamId(), fileName, adminUser.getUserId(), request);

        // When: MEMBER가 다운로드
        List<ChunkResponse> chunks = chunkService.getChunks(
                team.getTeamId(), fileName, memberUser.getUserId());

        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
    }

    @Test
    @DisplayName("팀 멤버가 아니면 청크를 다운로드할 수 없다")
    void getChunks_byOutsider_throwsException() {
        // Given: 청크 생성
        String encodedBlob = Base64.getEncoder().encodeToString("test data".getBytes());
        ChunkUploadRequest request = new ChunkUploadRequest(
                fileName,
                List.of(new ChunkUploadItem(0, encodedBlob))
        );
        chunkService.uploadChunks(team.getTeamId(), fileName, adminUser.getUserId(), request);

        // When & Then
        assertThatThrownBy(() -> chunkService.getChunks(
                team.getTeamId(), fileName, outsider.getUserId()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("팀 멤버가 아닙니다");
    }

    @Test
    @DisplayName("ADMIN은 청크를 삭제할 수 있다 (논리적 삭제)")
    void deleteChunk_byAdmin_success() {
        // Given: 청크 생성
        String encodedBlob = Base64.getEncoder().encodeToString("test data".getBytes());
        ChunkUploadRequest request = new ChunkUploadRequest(
                fileName,
                List.of(new ChunkUploadItem(0, encodedBlob))
        );
        chunkService.uploadChunks(team.getTeamId(), fileName, adminUser.getUserId(), request);

        // When: 삭제
        chunkService.deleteChunk(team.getTeamId(), fileName, 0, adminUser.getUserId());

        // Then: 논리적 삭제 확인
        Document document = documentRepository.findByTeamAndOriginalFileName(team, fileName).orElseThrow();
        VectorChunk chunk = vectorChunkRepository
                .findByDocumentIdAndChunkIndex(document.getDocumentId(), 0)
                .orElseThrow();
        assertThat(chunk.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("MEMBER는 청크를 삭제할 수 없다")
    void deleteChunk_byMember_throwsException() {
        // Given: 청크 생성
        String encodedBlob = Base64.getEncoder().encodeToString("test data".getBytes());
        ChunkUploadRequest request = new ChunkUploadRequest(
                fileName,
                List.of(new ChunkUploadItem(0, encodedBlob))
        );
        chunkService.uploadChunks(team.getTeamId(), fileName, adminUser.getUserId(), request);

        // When & Then
        assertThatThrownBy(() -> chunkService.deleteChunk(
                team.getTeamId(), fileName, 0, memberUser.getUserId()))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("ADMIN 권한이 필요합니다");
    }

    @Test
    @DisplayName("팀별 증분 동기화는 팀의 모든 문서에서 변경된 청크를 반환한다")
    void syncTeamChunks_returnsAllChangedChunksInTeam() {
        // Given: 2개 문서에 각각 청크 생성
        String fileName1 = "file1.pdf";
        String fileName2 = "file2.pdf";

        String encodedBlob = Base64.getEncoder().encodeToString("test data".getBytes());
        chunkService.uploadChunks(team.getTeamId(), fileName1, adminUser.getUserId(),
                new ChunkUploadRequest(fileName1, List.of(new ChunkUploadItem(0, encodedBlob))));
        chunkService.uploadChunks(team.getTeamId(), fileName2, adminUser.getUserId(),
                new ChunkUploadRequest(fileName2, List.of(new ChunkUploadItem(0, encodedBlob))));

        // When: 팀 전체 동기화 (lastVersion = -1이면 전체 조회, @Version은 0부터 시작)
        List<ChunkSyncResponse> allChunks = chunkService.syncTeamChunks(
                team.getTeamId(), -1, adminUser.getUserId());

        // Then: 2개 문서의 청크 모두 반환
        assertThat(allChunks).hasSize(2);
    }
}
