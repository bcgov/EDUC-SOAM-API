package ca.bc.gov.educ.api.soam.support;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import java.util.UUID;

public class JWSBuilder {
  private RsaJsonWebKey rsaJsonWebKey;
  private String claimsIssuer;
  private String claimsSubject;

  public JsonWebSignature build() {
    var claims = new JwtClaims();
    claims.setJwtId(UUID.randomUUID().toString()); // unique identifier for the JWT
    claims.setIssuer(claimsIssuer); // identifies the principal that issued the JWT
    claims.setSubject(claimsSubject); // identifies the principal that is the subject of the JWT
    claims.setAudience("https://host/api"); // identifies the recipients that the JWT is intended for
    claims.setExpirationTimeMinutesInTheFuture(10F); // identifies the expiration time on or after which the JWT MUST NOT be accepted for processing
    claims.setIssuedAtToNow(); // identifies the time at which the JWT was issued
    claims.setClaim("azp", "example-client-id"); // Authorized party - the party to which the ID Token was issued
    claims.setClaim("scope", "SOAM_LOGIN "); // Scope Values

    var jws = new JsonWebSignature();
    jws.setPayload(claims.toJson());
    jws.setKey(rsaJsonWebKey.getPrivateKey()); // the key to sign the JWS with
    jws.setAlgorithmHeaderValue(rsaJsonWebKey.getAlgorithm()); // Set the signature algorithm on the JWT/JWS that will integrity protect the claims
    jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId()); // a hint indicating which key was used to secure the JWS
    jws.setHeader("typ", "JWT"); // the media type of this JWS

    return jws;
  }

  public JWSBuilder subject(String subject) {
    this.claimsSubject = subject;
    return this;
  }

  public JWSBuilder rsaJsonWebKey(RsaJsonWebKey rsaJsonWebKey) {
    this.rsaJsonWebKey = rsaJsonWebKey;
    return this;
  }

  public JWSBuilder issuer(String issuer) {
    this.claimsIssuer = issuer;
    return this;
  }
}
