package horizon.SeRVe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "edge_nodes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EdgeNode implements UserDetails {

    @Id
    @Column(name = "node_id")
    private String nodeId; // UUID

    @Column(nullable = false, unique = true)
    private String serialNumber; // 로봇 시리얼 번호 (로그인 ID 역할)

    @Column(nullable = false)
    private String hashedToken; // 암호화된 API 토큰 (비밀번호 역할)

    @Column(name = "public_key", columnDefinition = "TEXT", nullable = false)
    private String publicKey; // 로봇의 공개키 (팀 키 암호화 전송용)

    // 로봇은 반드시 특정 팀(공장/사이트)에 소속됨
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    // --- UserDetails 구현 (Spring Security 연동용) ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_ROBOT"));
    }

    @Override
    public String getPassword() { return this.hashedToken; }

    @Override
    public String getUsername() { return this.serialNumber; }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}