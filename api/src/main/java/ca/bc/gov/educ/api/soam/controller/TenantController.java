package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.endpoint.SoamEndpoint;
import ca.bc.gov.educ.api.soam.endpoint.TenantEndpoint;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.service.SoamService;
import ca.bc.gov.educ.api.soam.service.TenantService;
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
public class TenantController implements TenantEndpoint {

  private final TenantService service;

  @Autowired
  public TenantController(final TenantService service) {
    this.service = service;
  }


  @Override
  public ResponseEntity<Void> determineTenantAccess(String clientID, String tenantID, String correlationID) {
    this.service.determineTenantAccess(clientID, tenantID, correlationID);
    return ResponseEntity.noContent().build();
  }

}
