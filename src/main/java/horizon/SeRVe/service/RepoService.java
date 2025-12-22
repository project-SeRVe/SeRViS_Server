package horizon.SeRVe.service;

import horizon.SeRVe.dto.repo.RepoResponse;
import horizon.SeRVe.entity.*;
import horizon.SeRVe.repository.MemberRepository;
import horizon.SeRVe.repository.TeamRepository;
import horizon.SeRVe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepoService {

    private final TeamRepository teamRepository; // 기존: TeamRepoRepository
    private final MemberRepository memberRepository; // [추가] 멤버 관리용
    private final UserRepository userRepository;     // [추가] 유저 조회용

    // 저장소 생성 로직 (encryptedTeamKey 파라미터만 추가)
    @Transactional
    public String createRepository(String name, String description, String ownerId, String encryptedTeamKey) {
        // 1. 중복 이름 체크
        if (teamRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 저장소 이름입니다.");
        }

        // 2. 저장소 생성 및 저장
        Team team = new Team(name, description, ownerId); // 기존: TeamRepository
        team.setType(RepoType.TEAM); // 타입 설정 추가
        Team saved = teamRepository.save(team);

        // [추가된 보안 로직] 3. 생성자(Owner)를 ADMIN 멤버로 등록
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        RepositoryMemberId memberId = new RepositoryMemberId(saved.getTeamId(), owner.getUserId());

        RepositoryMember adminMember = RepositoryMember.builder()
                .id(memberId)
                .team(saved) // 기존: teamRepository
                .user(owner)
                .role(Role.ADMIN) // 관리자 권한
                .encryptedTeamKey(encryptedTeamKey) // 암호화된 팀 키 저장
                .build();

        memberRepository.save(adminMember);

        return saved.getTeamId(); // 기존처럼 ID 반환
    }

    // [추가] 내 저장소 목록 조회
    @Transactional(readOnly = true)
    public List<RepoResponse> getMyRepos(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

        return memberRepository.findAllByUser(user).stream()
                .map(member -> {
                    Team team = member.getTeam(); // 기존: getTeamRepository()
                    // Owner 정보 조회 (team에는 ownerId 스트링만 있으므로)
                    User owner = userRepository.findById(team.getOwnerId())
                            .orElse(User.builder()
                                    .email("Unknown")
                                    .publicKey("")
                                    .encryptedPrivateKey("")
                                    .hashedPassword("")
                                    .build());
                    return RepoResponse.of(team, owner);
                })
                .collect(Collectors.toList());
    }

    // [추가] 팀 키 조회 (RAG 암호화용)
    @Transactional(readOnly = true)
    public String getTeamKey(String teamId, String userId) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소가 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

        // 기존: findByTeamRepositoryAndUser → findByTeamAndUser
        RepositoryMember member = memberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("해당 저장소의 멤버가 아닙니다."));

        return member.getEncryptedTeamKey();
    }

    // [추가] 저장소 삭제
    @Transactional
    public void deleteRepo(String teamId, String userId) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소가 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보가 없습니다."));

        RepositoryMember member = memberRepository.findByTeamAndUser(team, user)
                .orElseThrow(() -> new SecurityException("멤버가 아닙니다."));

        if (member.getRole() != Role.ADMIN) {
            throw new SecurityException("저장소 삭제 권한이 없습니다.");
        }

        teamRepository.delete(team);
    }
}
