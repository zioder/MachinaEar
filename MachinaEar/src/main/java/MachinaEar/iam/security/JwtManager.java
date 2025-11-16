package MachinaEar.iam.security;

import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import MachinaEar.iam.entities.Identity;
import MachinaEar.iam.controllers.Role;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@ApplicationScoped
public class JwtManager {

    private final RSAKey rsaJwk;
    private final JWKSet jwkSet;

    public JwtManager() {
        try {
            RSAKey k = new RSAKeyGenerator(2048)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(UUID.randomUUID().toString())
                    .algorithm(JWSAlgorithm.RS256)
                    .generate();
            this.rsaJwk = k; // contient privée + publique
            this.jwkSet = new JWKSet(k.toPublicJWK());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to init RSA keys", e);
        }
    }

    public String generateAccessToken(Identity identity, Set<Role> roles, long minutes) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(IdentityUtility.subject(identity))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(minutes * 60)))
                .claim("roles", roles.stream().map(Enum::name).toArray(String[]::new))
                .build();
        return sign(claims);
    }

    public String generateRefreshToken(Identity identity, long days) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(IdentityUtility.subject(identity))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(days * 86400)))
                .claim("typ", "refresh")
                .build();
        return sign(claims);
    }

    private String sign(JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaJwk.getKeyID())
                    .type(JOSEObjectType.JWT).build();
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new RSASSASigner(rsaJwk.toPrivateKey()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JWTClaimsSet validate(String token) throws Exception {
        SignedJWT jwt = SignedJWT.parse(token);
        boolean ok = jwt.verify(new RSASSAVerifier(rsaJwk.toRSAPublicKey()));
        if (!ok) throw new JOSEException("Invalid signature");
        return jwt.getJWTClaimsSet();
    }

    public String publicJwkSetJson() {
        return JSONObjectUtils.toJSONString(jwkSet.toJSONObject());
    }
}
