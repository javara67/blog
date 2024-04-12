package com.zeus.springbootblog.config.jwt;

import com.zeus.springbootblog.domain.User;
import com.zeus.springbootblog.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TokenProviderTest {
    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtProperties jwtProperties;

    // 토큰을 생성하는 메서드를 테스트하는 메서드
    @DisplayName("generateToken(): 유저 정보와 만료 기간을 전달해 토큰을 만들 수 있다.")
    @Test
    void generateToken() {
        // given
        // 토큰에 테스트 유저 정보를 추가
        User testUser = userRepository.save(User.builder()
                .email("user@email.com")
                .password("test")
                .build());

        // when
        // 토큰 제공자의 토큰을 만든다.
        // 만료시간은 현재시간으로부터 14일간 뒤로, 만료되지 않은 토큰을 생성
        String token = tokenProvider.generateToken(testUser, Duration.ofDays(14));

        // then
        // 토큰을 복호화 한다.
        // 토큰을 만들 때 클레임으로 넣어둔 id값이 만든 유저 id와 동일한지 확인한다.
        Long userId = Jwts.parser()
                .setSigningKey(jwtProperties.getSecretKey())
                .parseClaimsJws(token)
                .getBody()
                .get("id", Long.class);

        assertThat(userId).isEqualTo(testUser.getId());
    }
    // 토큰이 유효한 토큰인지 검증하는 메서드인 validToken() 메서드를 테스트
    @DisplayName("validToken(): 만료된 토큰인 경우에 유효성 검증에 실패한다.")
    @Test
    void validToken_invalidToken() {
        // given
        // 토큰을 생성한다.
        // 만료시간은 1970년1월1일부터 현재까지 시간을 밀리초 값(new Date().getTime())에서
        // 7일간의 밀리초를  빼 만료된 토큰으로 생성한다.
        String token = JwtFactory.builder()
                .expiration(new Date(new Date().getTime() - Duration.ofDays(7).toMillis()))
                .build()
                .createToken(jwtProperties);

        // when
        // 토큰 제공자의 유효한 토큰인지 검증한 뒤 결과값을 받는다.
        boolean result = tokenProvider.validToken(token);

        // then
        assertThat(result).isFalse();
    }


    @DisplayName("validToken(): 유효한 토큰인 경우에 유효성 검증에 성공한다.")
    @Test
    void validToken_validToken() {
        // given
        String token = JwtFactory.withDefaultValues()
                .createToken(jwtProperties);

        // when
        boolean result = tokenProvider.validToken(token);

        // then
        assertThat(result).isTrue();
    }


    @DisplayName("getAuthentication(): 토큰 기반으로 인증정보를 가져올 수 있다.")
    @Test
    void getAuthentication() {
        // given
        //
        String userEmail = "user@email.com";
        String token = JwtFactory.builder()
                .subject(userEmail)
                .build()
                .createToken(jwtProperties);

        // when
        // 토큰 제공자의 인증 객체를 받는다.
        Authentication authentication = tokenProvider.getAuthentication(token);

        // then
        // 반환받은 인증 객체의 subject의 값(user@email.com)이 같은지 확인한다.
        assertThat(((UserDetails) authentication.getPrincipal()).getUsername()).isEqualTo(userEmail);
    }
    // 토큰 기반으로 유저 id를 가져오는 메서드를 테스트
    @DisplayName("getUserId(): 토큰으로 유저 ID를 가져올 수 있다.")
    @Test
    void getUserId() {
        // given
        // 토큰을 생성한다. 클레임을 추가한다. 키는 "id", 값은 1인 유저 id이다.
        Long userId = 1L;
        String token = JwtFactory.builder()
                .claims(Map.of("id", userId))
                .build()
                .createToken(jwtProperties);

        // when
        Long userIdByToken = tokenProvider.getUserId(token);

        // then
        // 설정한 유저 id가 1과 같은지 확인한다.
        assertThat(userIdByToken).isEqualTo(userId);
    }
}