package horizon.SeRVe.common.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * JWT 토큰 생성 및 검증 (Common 모듈용)
 *
 * 기존 모놀리식 버전과의 차이점:
 * - UserDetailsService 의존성 제거
 * - getAuthentication()이 DB 조회 없이 JWT claims만으로 Authentication 생성
 * - principal = userId (String), details = {email, userId}
 *
 * Auth 모듈에서는 이 클래스를 사용하되, SecurityConfig에서
 * AuthJwtAuthenticationFilter를 통해 full UserDetails를 로드합니다.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:MySuperSecretKeyForHorizonServeProject2025MustBeLongEnoughToWork}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 24시간
    private long tokenValidityInMilliseconds;

    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 토큰 생성
    public String createToken(String userId, String email) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("userId", userId);

        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 인증 정보 조회 (DB 조회 없이 JWT claims만 사용)
     *
     * principal = userId (String)
     * details = Map{email, userId}
     *
     * Team/Core 서비스에서는 이 메서드를 그대로 사용합니다.
     * Auth 서비스에서는 AuthJwtAuthenticationFilter가 이 메서드 대신
     * UserDetailsService를 통해 full User 엔티티를 로드합니다.
     */
    public Authentication getAuthentication(String token) {
        String email = getUserEmail(token);
        String userId = getUserId(token);

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        auth.setDetails(Map.of("email", email, "userId", userId));
        return auth;
    }

    // 토큰에서 이메일 추출
    public String getUserEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    // 토큰에서 userId 추출
    public String getUserId(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("userId", String.class);
    }

    // 헤더에서 토큰 추출
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 유효성 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }
}
