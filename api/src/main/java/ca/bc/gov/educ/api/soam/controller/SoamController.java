package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.endpoint.SoamEndpoint;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.service.SoamService;
import ca.bc.gov.educ.api.soam.util.SoamUtil;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    Pair<SoamLoginEntity, HttpStatus> pair = this.service.performLink(serviceCard, correlationID);
    return ResponseEntity.status(pair.getRight()).body(pair.getLeft());
  }

  private ServicesCardEntity setupServicesCard(final MultiValueMap<String, String> formData){
    ServicesCardEntity serviceCard = null;
    if (formData.getFirst("did") != null) {
      serviceCard = new ServicesCardEntity();
      serviceCard.setBirthDate(formData.getFirst("birthDate"));
      serviceCard.setDid(SoamUtil.toUpperCaseNullSafe(formData.getFirst("did")));
      serviceCard.setEmail(SoamUtil.toUpperCaseNullSafe(formData.getFirst("email")));
      String genderVal = formData.getFirst("gender");
      if(!StringUtils.isEmpty(genderVal)){
        serviceCard.setGender(genderVal.substring(0,1).toUpperCase());
      }
      serviceCard.setIdentityAssuranceLevel(SoamUtil.toUpperCaseNullSafe(formData.getFirst("identityAssuranceLevel")));
      serviceCard.setGivenName(SoamUtil.toUpperCaseNullSafe(formData.getFirst("givenName")));
      serviceCard.setGivenNames(SoamUtil.toUpperCaseNullSafe(formData.getFirst("givenNames")));
      String postalVal = formData.getFirst("postalCode");
      if(!StringUtils.isEmpty(postalVal)) {
        serviceCard.setPostalCode(postalVal.replace(" ","").toUpperCase());
      }
      serviceCard.setSurname(SoamUtil.toUpperCaseNullSafe(formData.getFirst("surname")));
      serviceCard.setUserDisplayName(SoamUtil.toUpperCaseNullSafe(formData.getFirst("userDisplayName")));
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
