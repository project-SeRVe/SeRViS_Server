package horizon.SeRVe.team.dto.repo;

import horizon.SeRVe.team.entity.RepoType;
import horizon.SeRVe.team.entity.Role;
import horizon.SeRVe.team.entity.Team;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RepoResponse {
    private String id;
    private String name;
    private String description;
    private String type;
    private String ownerId;
    private String ownerEmail;
    private String role;

    public static RepoResponse of(Team team, String ownerEmail, Role role) {
        return RepoResponse.builder()
                .id(team.getTeamId())
                .name(team.getName())
                .description(team.getDescription())
                .type(team.getType() != null ? team.getType().name() : RepoType.TEAM.name())
                .ownerId(team.getOwnerId())
                .ownerEmail(ownerEmail)
                .role(role.name())
                .build();
    }
}
