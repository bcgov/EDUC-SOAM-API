package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.endpoint.SoamEndpoint;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.service.SoamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Soam API controller
 *
 * @author Marco Villeneuve
 */

@RestController
@EnableResourceServer
public class SoamController implements SoamEndpoint {

  private final SoamService service;

  @Autowired
  public SoamController(final SoamService soamService) {
    this.service = soamService;
  }

  @Override
  public void performLogin(@RequestBody MultiValueMap<String, String> formData) {
    ServicesCardEntity serviceCard = null;
    if (formData.getFirst("did") != null) {
      serviceCard = new ServicesCardEntity();
      serviceCard.setBirthDate(formData.getFirst("birthDate"));
      serviceCard.setCity(formData.getFirst("city"));
      serviceCard.setCountry(formData.getFirst("country"));
      serviceCard.setDid(formData.getFirst("did"));
      serviceCard.setEmail(formData.getFirst("email"));
      serviceCard.setGender(formData.getFirst("gender"));
      serviceCard.setGivenName(formData.getFirst("givenName"));
      serviceCard.setGivenNames(formData.getFirst("givenNames"));
      serviceCard.setPostalCode(formData.getFirst("postalCode"));
      serviceCard.setProvince(formData.getFirst("province"));
      serviceCard.setStreetAddress(formData.getFirst("streetAddress"));
      serviceCard.setSurname(formData.getFirst("surname"));
      serviceCard.setUserDisplayName(formData.getFirst("userDisplayName"));
    }
    service.performLogin(formData.getFirst("identifierType"), formData.getFirst("identifierValue"), formData.getFirst("userID"), serviceCard);
  }

  @Override
  public SoamLoginEntity getSoamLoginEntity(@PathVariable String typeCode, @PathVariable String typeValue) {
    return service.getSoamLoginEntity(typeCode, typeValue);
  }

  @Override
  public String health() {
    return "OK";
  }
}
