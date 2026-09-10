package com.after.backend.auth.application;

import com.after.backend.auth.config.AuthProperties;
import com.after.backend.user.domain.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final AuthProperties properties;

    public JwtService(
            JwtEncoder jwtEncoder,
            AuthProperties properties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public AccessToken issueAccessToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(
                properties.jwt().accessTokenTtl()
        );

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("email", user.getEmail())
                .build();

        String value = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        return new AccessToken(
                value,
                properties.jwt().accessTokenTtl().toSeconds()
        );
    }
}