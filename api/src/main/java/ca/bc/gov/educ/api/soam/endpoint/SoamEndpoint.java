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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/")
@OpenAPIDefinition(info = @Info(title = "API for SOAM.", description = "The SOAM API is used to support login functionality for the SOAM Keycloak Instance.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"SOAM_LOGIN"})})
public interface SoamEndpoint {

  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  @PreAuthorize("hasAuthority('SCOPE_SOAM_LOGIN')")
  ResponseEntity<Void> performLogin(@RequestParam MultiValueMap<String, String> formData, @RequestHeader String correlationID);

  @GetMapping("/{typeCode}/{typeValue}")
  @PreAuthorize("hasAuthority('SCOPE_SOAM_LOGIN')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND.")})
  ResponseEntity<SoamLoginEntity> getSoamLoginEntity(@PathVariable String typeCode, @PathVariable String typeValue, @RequestHeader String correlationID);

  @GetMapping("/userInfo")
  @PreAuthorize("hasAuthority('SCOPE_SOAM_USER_INFO')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND.")})
  ResponseEntity<SoamLoginEntity> getSoamLoginEntity(@AuthenticationPrincipal Jwt token, @RequestHeader String correlationID);

  @GetMapping("/{ssoGuid}/sts-user-roles")
  @PreAuthorize("hasAuthority('SCOPE_STS_ROLES')")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND.")})
  ResponseEntity<List<String>> getStsUserRolesByGuid(@PathVariable String ssoGuid, @RequestHeader String correlationID);

  @PostMapping(value = "/link", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  @PreAuthorize("hasAuthority('SCOPE_SOAM_LINK')")
  ResponseEntity<SoamLoginEntity> performBCSCLink(@RequestParam MultiValueMap<String, String> formData, @RequestHeader String correlationID);

}
