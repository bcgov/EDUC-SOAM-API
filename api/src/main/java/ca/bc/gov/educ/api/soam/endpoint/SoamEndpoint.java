package ca.bc.gov.educ.api.soam.endpoint;

import ca.bc.gov.educ.api.soam.model.entity.SoamLoginEntity;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/")
@OpenAPIDefinition(info = @Info(title = "API for SOAM.", description = "The SOAM API is used to support login functionality for the SOAM Keycloak Instance.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"SOAM_LOGIN"})})
public interface SoamEndpoint {

  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  @PreAuthorize("hasAuthority('SCOPE_SOAM_LOGIN')")
  ResponseEntity<Void> performLogin(@RequestBody MultiValueMap<String, String> formData);

  @GetMapping("/{typeCode}/{typeValue}")
  @PreAuthorize("hasAuthority('SCOPE_SOAM_LOGIN')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND.")})
  ResponseEntity<SoamLoginEntity> getSoamLoginEntity(@PathVariable String typeCode, @PathVariable String typeValue);

}
