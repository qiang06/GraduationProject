package com.myproject.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {

    @Value("${spring.security.jwt.key}")
    String key;

    @Value("${spring.security.jwt.expire}")
    int expire;

    /**
     * 创建 JWT 令牌
     *
     * @param details UserDetails
     * @param id "int"
     * @param username “String”
     * @return 返回一个JWT令牌
     */
    public String creatJwt(UserDetails details, int id, String username) {

        Algorithm algorithm = Algorithm.HMAC256(key);

        Date expireTime = this.expireTime();

        return JWT.create()
                .withClaim("id", id)
                .withClaim("name", username)
                .withClaim("authorities", details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .withExpiresAt(expireTime)
                .withIssuedAt(new Date())
                .sign(algorithm);

    }

    /**
     * 获取 JWT 令牌过期时间
     *
     * @return 返回一个token的过期时间(Date)
     */
    public Date expireTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, expire * 24);
        return calendar.getTime();
    }

    /**
     * 验证token的格式以及是否过期
     * @param headerToken 请求头中的token
     * @return 通过验证符合要求的token
     */
    public DecodedJWT resolveJwt(String headerToken) {
        String token = converseToken(headerToken);
        if (token == null) return null;
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try {
            DecodedJWT verify = jwtVerifier.verify(token);
            Date expiresAt = verify.getExpiresAt();
            return new Date().after(expiresAt) ? null : verify;
        }catch (JWTVerificationException e) {
            return null;
        }


    }

    /**
     * 验证 JWT 令牌
     * @param headerToken 请求头中的token
     * @return 返回一个干净的token
     */
    private String converseToken(String headerToken) {
        if (headerToken == null || !headerToken.startsWith("Bearer "))
            return null;
        return headerToken.substring(7);

    }

    public UserDetails toUser(DecodedJWT jwt) {
        Map<String, Claim> claims = jwt.getClaims();
        return User
                .withUsername(claims.get("name").asString())
                .password("*********")
                .authorities(claims.get("authorities").asArray(String.class))
                .build();
    }

    public Integer toId(DecodedJWT jwt) {
        Map<String, Claim> claims = jwt.getClaims();
        return claims.get("id").asInt();
    }

}
