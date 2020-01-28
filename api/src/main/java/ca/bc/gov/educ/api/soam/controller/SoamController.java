package ca.bc.gov.educ.api.soam.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RestController;

import ca.bc.gov.educ.api.soam.endpoint.SoamEndpoint;
import ca.bc.gov.educ.api.soam.model.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.service.SoamService;

/**
 * Soam API controller
 *
 * @author Marco Villeneuve
 */

@RestController
public class SoamController implements SoamEndpoint {

    private final SoamService service;

    SoamController(@Autowired final SoamService soamService) {
        this.service = soamService;
    }

	public void performLogin(@RequestBody MultiValueMap<String, String> formData){
        service.performLogin(formData.getFirst("identifierType"),formData.getFirst("identifierValue"),formData.getFirst("userID"));
    }
    
    public SoamLoginEntity getSoamLoginEntity(@PathVariable String typeCode, @PathVariable String typeValue){
        return service.getSoamLoginEntity(typeCode, typeValue);
    }
    
}
