package horizon.SeRVe.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @Column(name = "user_id")
    private String userId; // UUID

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "hashed_password", nullable = false)
    private String hashedPassword;

    // 클라이언트의 RSA/ECIES 공개키 (서버에 저장)
    @Column(name = "public_key", columnDefinition = "TEXT", nullable = false)
    private String publicKey;

    // 사용자 비밀번호로 암호화된 개인키 (서버는 복호화 불가능, 저장만 함)
    @Column(name = "encrypted_private_key", columnDefinition = "TEXT", nullable = false)
    private String encryptedPrivateKey;


    // --- 비즈니스 로직 ---
    public void updatePassword(String newHashedPassword, String newEncryptedPrivateKey) {
        this.hashedPassword = newHashedPassword;
        this.encryptedPrivateKey = newEncryptedPrivateKey;
    }

    // --- UserDetails 구현 ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public String getPassword() {
        return this.hashedPassword;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
