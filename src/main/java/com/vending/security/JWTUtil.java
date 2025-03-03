package com.vending.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.vending.core.models.Utente;
import java.util.Date;
import java.util.Map;

public class JWTUtil {
    private static final String SECRET = "Pissir2025!";
    private static final long EXPIRATION_TIME = 864_000_000; // 10 days
    private final Algorithm algorithm;
    private static final Algorithm algo = Algorithm.HMAC256(SECRET);

    public JWTUtil() {
        this.algorithm = Algorithm.HMAC256(SECRET);
    }

    public String generateToken(Utente utente) {
        return JWT.create()
                .withSubject(String.valueOf(utente.getId()))
                .withClaim("role", utente.getRuolo())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(algorithm);
    }

    public boolean validateToken(String token) {
        try {
            JWT.require(algorithm).build().verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
   

    public String getRoleFromToken(String token) {
        DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
        return jwt.getClaim("role").asString();
    }
}