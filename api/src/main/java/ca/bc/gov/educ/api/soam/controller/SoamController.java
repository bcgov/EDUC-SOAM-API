package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.endpoint.SoamEndpoint;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.service.SoamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RestController;

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

  public ResponseEntity<Void> performLogin(MultiValueMap<String, String> formData) {
    ServicesCardEntity serviceCard = null;
    if (formData.getFirst("did") != null) {
      serviceCard = new ServicesCardEntity();
      serviceCard.setBirthDate(formData.getFirst("birthDate"));
      serviceCard.setCity(formData.getFirst("city"));
      serviceCard.setCountry(formData.getFirst("country"));
      serviceCard.setDid(formData.getFirst("did"));
      serviceCard.setEmail(formData.getFirst("email"));
      serviceCard.setGender(formData.getFirst("gender"));
      serviceCard.setIdentityAssuranceLevel(formData.getFirst("identityAssuranceLevel"));
      serviceCard.setGivenName(formData.getFirst("givenName"));
      serviceCard.setGivenNames(formData.getFirst("givenNames"));
      serviceCard.setPostalCode(formData.getFirst("postalCode"));
      serviceCard.setProvince(formData.getFirst("province"));
      serviceCard.setStreetAddress(formData.getFirst("streetAddress"));
      serviceCard.setSurname(formData.getFirst("surname"));
      serviceCard.setUserDisplayName(formData.getFirst("userDisplayName"));
    }
    service.performLogin(formData.getFirst("identifierType"), formData.getFirst("identifierValue"), serviceCard);
    return ResponseEntity.noContent().build();
  }

  public ResponseEntity<SoamLoginEntity> getSoamLoginEntity(String typeCode, String typeValue) {
    return ResponseEntity.ok(service.getSoamLoginEntity(typeCode, typeValue));
  }

}
