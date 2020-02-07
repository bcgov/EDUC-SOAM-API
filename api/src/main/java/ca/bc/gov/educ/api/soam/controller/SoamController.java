package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.service.SoamService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

/**
 * Soam API controller
 * <b> DO NOT IMPLEMENT AN INTERFACE </b>
 * @author Marco Villeneuve
 */

@RestController
@EnableResourceServer
@RequestMapping("/")
@OpenAPIDefinition(info = @Info(title = "API for SOAM.", description = "The SOAM API is used to support login functionality for the SOAM Keycloak Instance.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"SOAM_LOGIN"})})
public class SoamController {

  private final SoamService service;

  @Autowired
  public SoamController(final SoamService soamService) {
    this.service = soamService;
  }

  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  @PreAuthorize("#oauth2.hasScope('SOAM_LOGIN')")
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

  @GetMapping("/{typeCode}/{typeValue}")
  @PreAuthorize("#oauth2.hasScope('SOAM_LOGIN')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND.")})
  public SoamLoginEntity getSoamLoginEntity(@PathVariable String typeCode, @PathVariable String typeValue) {
    return service.getSoamLoginEntity(typeCode, typeValue);
  }

  @GetMapping("/health")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
  public String health() {
    return "OK";
  }
}
