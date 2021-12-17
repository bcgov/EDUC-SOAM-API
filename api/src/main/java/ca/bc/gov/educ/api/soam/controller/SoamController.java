package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.endpoint.SoamEndpoint;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.service.SoamService;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Soam API controller
 *
 * @author Marco Villeneuve
 */

@RestController
public class SoamController implements SoamEndpoint {

  private final SoamService service;

  @Autowired
  public SoamController(final SoamService soamService) {
    this.service = soamService;
  }

  @Override
  public ResponseEntity<SoamLoginEntity> performBCSCLink(final MultiValueMap<String, String> formData, final String correlationID) {
    ServicesCardEntity serviceCard = setupServicesCard(formData);
    return ResponseEntity.ok(this.service.performLink(serviceCard, correlationID));
  }

  private ServicesCardEntity setupServicesCard(final MultiValueMap<String, String> formData){
    ServicesCardEntity serviceCard = null;
    if (formData.getFirst("did") != null) {
      serviceCard = new ServicesCardEntity();
      serviceCard.setBirthDate(formData.getFirst("birthDate"));
      serviceCard.setDid(formData.getFirst("did"));
      serviceCard.setEmail(formData.getFirst("email"));
      serviceCard.setGender(formData.getFirst("gender"));
      serviceCard.setIdentityAssuranceLevel(formData.getFirst("identityAssuranceLevel"));
      serviceCard.setGivenName(formData.getFirst("givenName"));
      serviceCard.setGivenNames(formData.getFirst("givenNames"));
      serviceCard.setPostalCode(formData.getFirst("postalCode"));
      serviceCard.setSurname(formData.getFirst("surname"));
      serviceCard.setUserDisplayName(formData.getFirst("userDisplayName"));
    }
    return serviceCard;
  }

  @Override
  public ResponseEntity<Void> performLogin(final MultiValueMap<String, String> formData, final String correlationID) {
    ServicesCardEntity serviceCard = setupServicesCard(formData);
    this.service.performLogin(formData.getFirst("identifierType"), formData.getFirst("identifierValue"), serviceCard, correlationID);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<SoamLoginEntity> getSoamLoginEntity(final String typeCode, final String typeValue, final String correlationID) {
    return ResponseEntity.ok(this.service.getSoamLoginEntity(typeCode, typeValue, correlationID));
  }

  @Override
  public ResponseEntity<SoamLoginEntity> getSoamLoginEntity(@AuthenticationPrincipal final Jwt token, final String correlationID) {
    return ResponseEntity.ok(this.service.getSoamLoginEntity(token.getClaimAsString("digitalIdentityID"), correlationID));
  }

  @Override
  public ResponseEntity<List<String>> getStsUserRolesByGuid(final String ssoGuid, final String correlationID) {
    val roles = this.service.getStsRolesBySSoGuid(ssoGuid, correlationID);
    return ResponseEntity.ok(roles);
  }
}
