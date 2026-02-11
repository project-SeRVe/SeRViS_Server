package horizon.SeRVe.team.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "repository_members")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RepositoryMember {

    @EmbeddedId
    private RepositoryMemberId id;

    @MapsId("teamId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    // name 생략 → 논리 이름이 EmbeddedId의 userId와 동일하게 유지되어 충돌 없음
    @Column(insertable = false, updatable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "encrypted_team_key", columnDefinition = "TEXT", nullable = false)
    private String encryptedTeamKey;
}
