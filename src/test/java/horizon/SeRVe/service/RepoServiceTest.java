package horizon.SeRVe.service;

import horizon.SeRVe.entity.Team;
import horizon.SeRVe.entity.User;
import horizon.SeRVe.repository.MemberRepository;
import horizon.SeRVe.repository.TeamRepository;
import horizon.SeRVe.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RepoServiceTest {

    @InjectMocks
    private RepoService repoService;

    @Mock
    private TeamRepository teamRepository; // 기존: TeamRepoRepository

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("저장소 생성 성공")
    void createRepository_Success() {
        // given
        String name = "MyProject";
        String description = "Test Repo";
        String ownerId = "user1";
        String encryptedTeamKey = "encryptedKey";

        Team mockTeam = new Team(name, description, ownerId); // 기존: TeamRepository(엔티티)
        mockTeam.setTeamId(UUID.randomUUID().toString()); // ID가 생성되었다고 가정

        User mockOwner = User.builder()
                .userId(ownerId)
                .email("owner@test.com")
                .hashedPassword("hashed")
                .publicKey("pubKey")
                .encryptedPrivateKey("encPrivKey")
                .build();

        given(teamRepository.findByName(name)).willReturn(Optional.empty()); // 중복 없음
        given(teamRepository.save(any(Team.class))).willReturn(mockTeam);
        given(userRepository.findById(ownerId)).willReturn(Optional.of(mockOwner));

        // when
        String teamId = repoService.createRepository(name, description, ownerId, encryptedTeamKey); // 기존: repoId

        // then
        assertEquals(mockTeam.getTeamId(), teamId);
    }

    @Test
    @DisplayName("중복된 이름으로 생성 시 실패")
    void createRepository_DuplicateName_Fail() {
        // given
        String name = "MyProject";
        given(teamRepository.findByName(name)).willReturn(Optional.of(new Team()));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            repoService.createRepository(name, "desc", "user1", "encKey");
        });
    }
}
