package com.capstone.confms.security.jwt;

import com.capstone.confms.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private int jwtExpirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String generateJwtToken(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        String username;
        Integer id = null;
        String firstName = null;
        String lastName = null;
        String email = null;
        String country = null;
        Boolean isActive = null;
        List<String> roles = null;

        if (principal instanceof UserDetailsImpl userDetails) {
            username = userDetails.getUsername();
            id = userDetails.getId();
            firstName = userDetails.getFirstName();
            lastName = userDetails.getLastName();
            email = userDetails.getEmail();
            country = userDetails.getCountry();
            isActive = userDetails.isEnabled();
            roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
        } else if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
            email = userDetails.getUsername();
        } else {
            username = authentication.getName();
            email = authentication.getName();
        }

        JwtBuilder builder = Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key());

        if (id != null) {
            builder.claim("id", id);
        }
        if (firstName != null) {
            builder.claim("firstName", firstName);
        }
        if (lastName != null) {
            builder.claim("lastName", lastName);
        }
        if (email != null) {
            builder.claim("email", email);
        }
        if (country != null) {
            builder.claim("country", country);
        }
        if (isActive != null) {
            builder.claim("isActive", isActive);
        }
        if (roles != null) {
            builder.claim("roles", roles);
        }

        return builder.compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
