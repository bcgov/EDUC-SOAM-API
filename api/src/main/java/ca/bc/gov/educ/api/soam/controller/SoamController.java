package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.endpoint.SoamEndpoint;
import ca.bc.gov.educ.api.soam.model.SoamLoginEntity;
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

    SoamController(@Autowired final SoamService soamService) {
        this.service = soamService;
    }

	public void performLogin(@RequestBody MultiValueMap<String, String> formData){
        service.performLogin(formData.getFirst("identifierType"),formData.getFirst("identifierValue"),formData.getFirst("userID"));
    }
    
    public SoamLoginEntity getSoamLoginEntity(@PathVariable String typeCode, @PathVariable String typeValue){
        return service.getSoamLoginEntity(typeCode, typeValue);
    }

    @Override
    public String health() {
        return "OK";
    }

}
