package MachinaEar.iam.controllers.managers;

import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import MachinaEar.iam.controllers.Role;
import MachinaEar.iam.controllers.repositories.IdentityRepository;
import MachinaEar.iam.controllers.repositories.GrantRepository;
import MachinaEar.iam.entities.Identity;
import MachinaEar.iam.security.Argon2Utility;
import MachinaEar.iam.security.JwtManager;

@ApplicationScoped
public class PhoenixIAMManager {

    @Inject IdentityRepository identities;
    @Inject GrantRepository grants;
    @Inject JwtManager jwt;

    public TokenPair login(String email, char[] password) {
        Identity user = identities.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Unknown email"));
        if (!user.isActive()) throw new IllegalStateException("User disabled");
        if (!Argon2Utility.verify(user.getPasswordHash(), password))
            throw new SecurityException("Bad credentials");

        Set<Role> roles = user.getRoles();
        if (roles == null || roles.isEmpty())
            roles = new HashSet<>(grants.findRolesByIdentity(user.getId()));

        String access = jwt.generateAccessToken(user, roles, 30); // 30 min
        String refresh = jwt.generateRefreshToken(user, 7);       // 7 jours
        return new TokenPair(access, refresh);
    }

    public TokenPair refresh(String refreshToken) {
        try {
            var claims = jwt.validate(refreshToken);
            if (!"refresh".equals(claims.getStringClaim("typ")))
                throw new SecurityException("Not a refresh token");
            String subject = claims.getSubject();
            Identity user = identities.findByEmail(subject)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown subject"));

            Set<Role> roles = user.getRoles();
            if (roles == null || roles.isEmpty())
                roles = new HashSet<>(grants.findRolesByIdentity(user.getId()));

            String access = jwt.generateAccessToken(user, roles, 30);
            String refresh = jwt.generateRefreshToken(user, 7);
            return new TokenPair(access, refresh);
        } catch (Exception e) {
            throw new SecurityException("Invalid refresh token");
        }
    }

    // DTO Java 17 (record)
    public static record TokenPair(String accessToken, String refreshToken) {}
}
