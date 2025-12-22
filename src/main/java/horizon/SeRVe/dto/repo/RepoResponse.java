package horizon.SeRVe.dto.repo;

import horizon.SeRVe.entity.RepoType;
import horizon.SeRVe.entity.Team;
import horizon.SeRVe.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RepoResponse {
    private String Teamid;
    private String name;
    private String description;
    private String type;
    private String ownerId;
    private String ownerEmail;

    /**
     * Entity -> DTO 변환
     * Team에는 ownerId(String)만 있으므로,
     * Service에서 조회한 User 객체(owner)를 함께 받아야 이메일을 채울 수 있음.
     */
    // 기존: TeamRepository → Team
    public static RepoResponse of(Team team, User owner) {
        return RepoResponse.builder()
                .Teamid(team.getTeamId())
                .name(team.getName())
                .description(team.getDescription())
                .type(team.getType() != null ? team.getType().name() : RepoType.TEAM.name())
                .ownerId(team.getOwnerId())
                .ownerEmail(owner.getEmail())
                .build();
    }
}
