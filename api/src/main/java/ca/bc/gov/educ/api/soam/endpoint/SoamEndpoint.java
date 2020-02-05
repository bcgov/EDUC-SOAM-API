package ca.bc.gov.educ.api.soam.endpoint;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import ca.bc.gov.educ.api.soam.model.entity.SoamLoginEntity;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RequestMapping("/")
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableResourceServer
@OpenAPIDefinition(info = @Info(title = "API for SOAM.", description = "The SOAM API is used to support login functionality for the SOAM Keycloak Instance.", version = "1"), security = {@SecurityRequirement(name = "OAUTH2", scopes = {"SOAM_LOGIN"})})
public interface SoamEndpoint {

    @RequestMapping(
    		value="/login", 
    		method=RequestMethod.POST, 
    		consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("#oauth2.hasScope('SOAM_LOGIN')")
    void performLogin(@RequestBody MultiValueMap<String, String> formData);

    @GetMapping("/{typeCode}/{typeValue}")
    @PreAuthorize("#oauth2.hasScope('SOAM_LOGIN')")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND.")})
    SoamLoginEntity getSoamLoginEntity(@PathVariable String typeCode, @PathVariable String typeValue);
    
}
