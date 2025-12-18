package horizon.SeRVe.entity;

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

    @MapsId("repoId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id")
    private TeamRepository teamRepository;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // [보안 핵심] 멤버별로 암호화된 팀 키 저장
    @Column(name = "encrypted_team_key", columnDefinition = "TEXT", nullable = false)
    private String encryptedTeamKey;
}