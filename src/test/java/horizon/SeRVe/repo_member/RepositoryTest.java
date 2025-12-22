package horizon.SeRVe.repo_member;

import horizon.SeRVe.entity.*;
import horizon.SeRVe.repository.MemberRepository;
import horizon.SeRVe.repository.TeamRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest // JPA 컴포넌트만 로드하여 테스트 (인메모리 DB 자동 사용)
class RepositoryTest {

    @Autowired
    private TestEntityManager entityManager; // 테스트용 엔티티 관리자 (User 등을 미리 넣기 위함)

    @Autowired
    private TeamRepository teamRepository; // 기존: TeamRepoRepository

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("Team: 저장 및 OwnerId로 목록 조회 테스트") // 기존: TeamRepository
    void testTeamRepository() {
        // Given
        String ownerId = "owner-123";
        Team team = new Team("Test Repo", "Test Description", ownerId); // 기존: TeamRepository
        team.setType(RepoType.TEAM); // 타입 설정

        // When: 저장
        teamRepository.save(team);

        // Then: ownerId로 조회가 잘 되는지 확인
        List<Team> myTeams = teamRepository.findAllByOwnerId(ownerId); // 기존: myRepos

        assertFalse(myTeams.isEmpty());
        assertEquals("Test Repo", myTeams.get(0).getName());
        assertEquals(ownerId, myTeams.get(0).getOwnerId());

        System.out.println(">> Team 테스트 통과: 저장 및 조회 성공"); // 기존: TeamRepository
    }

    @Test
    @DisplayName("MemberRepository: 멤버 추가, 조회 및 복합키 동작 테스트")
    void testMemberRepository() {
        // 1. [준비] User 엔티티 미리 저장 (RepositoryMember가 참조해야 하므로)
        // (UserRepository가 없어도 TestEntityManager로 저장 가능합니다)
        User user = User.builder()
                .userId("member-user-uuid")
                .email("member@serve.com")
                .hashedPassword("hashed")
                .publicKey("pubKey")
                .encryptedPrivateKey("encPrivKey")
                .build();
        entityManager.persist(user);

        // 2. [준비] Team 미리 저장 (기존: TeamRepository)
        Team team = new Team("Secure Repo", "Doc", "owner-id");
        entityManager.persist(team);

        // 3. [테스트] RepositoryMember 생성 및 저장
        RepositoryMemberId memberId = new RepositoryMemberId(team.getTeamId(), user.getUserId());

        RepositoryMember member = RepositoryMember.builder()
                .id(memberId)
                .team(team) // 기존: teamRepository
                .user(user)
                .role(Role.MEMBER)
                .encryptedTeamKey("encrypted-key-sample")
                .build();

        memberRepository.save(member);

        // 4. [검증] findAllByUser: 내가 속한 방 조회가 되는지 (EntityGraph 동작 확인)
        List<RepositoryMember> membersByUser = memberRepository.findAllByUser(user);
        assertEquals(1, membersByUser.size());
        assertEquals("Secure Repo", membersByUser.get(0).getTeam().getName()); // 기존: getTeamRepository()
        System.out.println(">> findAllByUser 검증 완료: 연관된 저장소 이름 조회 성공");

        // 5. [검증] findAllByTeam: 방 멤버 목록 조회가 되는지
        // 기존: findAllByTeamRepository → findAllByTeam
        List<RepositoryMember> membersByTeam = memberRepository.findAllByTeam(team);
        assertEquals(1, membersByTeam.size());
        assertEquals("member@serve.com", membersByTeam.get(0).getUser().getEmail());
        System.out.println(">> findAllByTeam 검증 완료: 연관된 유저 이메일 조회 성공");

        // 6. [검증] existsByTeamAndUser: 중복 가입 체크 로직 확인
        // 기존: existsByTeamRepositoryAndUser → existsByTeamAndUser
        boolean exists = memberRepository.existsByTeamAndUser(team, user);
        assertTrue(exists);
        System.out.println(">> existsByTeamAndUser 검증 완료");
    }
}
