package horizon.SeRVe.team.service;

import horizon.SeRVe.team.dto.repo.RepoResponse;
import horizon.SeRVe.team.entity.*;
import horizon.SeRVe.team.feign.AuthServiceClient;
import horizon.SeRVe.team.repository.MemberRepository;
import horizon.SeRVe.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepoService {

    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final AuthServiceClient authServiceClient;

    // 저장소 생성
    @Transactional
    public String createRepository(String name, String description, String ownerId, String encryptedTeamKey) {
        if (teamRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 저장소 이름입니다.");
        }

        Team team = new Team(name, description, ownerId);
        team.setType(RepoType.TEAM);
        Team saved = teamRepository.save(team);

        // 생성자(Owner)를 ADMIN 멤버로 등록 (User JPA 참조 없이 userId로 직접 생성)
        RepositoryMemberId memberId = new RepositoryMemberId(saved.getTeamId(), ownerId);

        RepositoryMember adminMember = RepositoryMember.builder()
                .id(memberId)
                .team(saved)
                .role(Role.ADMIN)
                .encryptedTeamKey(encryptedTeamKey)
                .build();

        memberRepository.save(adminMember);

        return saved.getTeamId();
    }

    // 내 저장소 목록 조회
    @Transactional(readOnly = true)
    public List<RepoResponse> getMyRepos(String userId) {
        return memberRepository.findAllByUserId(userId).stream()
                .map(member -> {
                    Team team = member.getTeam();
                    String ownerEmail;
                    try {
                        ownerEmail = authServiceClient.getUserInfo(team.getOwnerId()).getEmail();
                    } catch (Exception e) {
                        ownerEmail = "Unknown";
                    }
                    return RepoResponse.of(team, ownerEmail, member.getRole());
                })
                .collect(Collectors.toList());
    }

    // 팀 키 조회 (RAG 암호화용)
    @Transactional(readOnly = true)
    public String getTeamKey(String teamId, String userId) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소가 없습니다."));

        RepositoryMember member = memberRepository.findByTeamAndUserId(team, userId)
                .orElseThrow(() -> new SecurityException(
                    String.format("팀 '%s'의 멤버가 아닙니다. 팀 ADMIN에게 초대를 요청하세요. (ADMIN: %s)",
                            team.getName(),
                            getAdminEmail(team))
                ));

        String encryptedKey = member.getEncryptedTeamKey();
        if (encryptedKey == null || encryptedKey.isEmpty()) {
            throw new IllegalStateException(
                String.format("팀 키가 설정되지 않았습니다. 팀 '%s'의 ADMIN에게 재초대를 요청하세요. (ADMIN: %s)",
                        team.getName(),
                        getAdminEmail(team))
            );
        }

        return encryptedKey;
    }

    // ADMIN 이메일 조회 헬퍼
    private String getAdminEmail(Team team) {
        return memberRepository.findAllByTeam(team).stream()
                .filter(m -> m.getRole() == Role.ADMIN)
                .findFirst()
                .map(m -> {
                    try {
                        return authServiceClient.getUserInfo(m.getUserId()).getEmail();
                    } catch (Exception e) {
                        return "Unknown";
                    }
                })
                .orElse("Unknown");
    }

    // 저장소 삭제
    @Transactional
    public void deleteRepo(String teamId, String userId) {
        Team team = teamRepository.findByTeamId(teamId)
                .orElseThrow(() -> new IllegalArgumentException("저장소가 없습니다."));

        RepositoryMember member = memberRepository.findByTeamAndUserId(team, userId)
                .orElseThrow(() -> new SecurityException("멤버가 아닙니다."));

        if (member.getRole() != Role.ADMIN) {
            throw new SecurityException("저장소 삭제 권한이 없습니다.");
        }

        teamRepository.delete(team);
    }
}
