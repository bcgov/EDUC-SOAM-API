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

@RequestMapping("/tenant")
@OpenAPIDefinition(info = @Info(title = "API for SOAM.", description = "The SOAM API is used to support login functionality for the SOAM Keycloak Instance.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"SOAM_LOGIN"})})
public interface TenantEndpoint {

  @GetMapping()
  @PreAuthorize("hasAuthority('SCOPE_SOAM_TENANT')")
  ResponseEntity<Void> determineTenantAccess(@RequestParam(name = "clientID") String clientID, @RequestParam(name = "tenantID") String tenantID, @RequestHeader String correlationID);

}
