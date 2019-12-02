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

import ca.bc.gov.educ.api.soam.model.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.service.SoamService;

/**
 * Soam API controller
 *
 * @author Marco Villeneuve
 */

@RestController
@RequestMapping("/")
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableResourceServer
public class SoamController {

    @Autowired
    private final SoamService service;

    SoamController(SoamService soam){
        this.service = soam;
    }

    @RequestMapping(
    		value="/login", 
    		method=RequestMethod.POST, 
    		consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @PreAuthorize("#oauth2.hasScope('SOAM_LOGIN')")
    public void performLogin(@RequestBody MultiValueMap<String, String> formData){
        service.performLogin(formData.get("identityType").get(0),formData.get("identifierValue").get(0),formData.get("userID").get(0));
    }
    
    @GetMapping("/{typeCode}/{typeValue}")
    @PreAuthorize("#oauth2.hasScope('SOAM_LOGIN')")
    public SoamLoginEntity getSoamLoginEntity(@PathVariable String typeCode, @PathVariable String typeValue){
        return service.getSoamLoginEntity(typeCode, typeValue);
    }

}
