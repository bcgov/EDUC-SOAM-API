package ca.bc.gov.educ.api.soam.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ca.bc.gov.educ.api.soam.service.SoamService;

/**
 * Soam API controller
 *
 * @author Marco Villeneuve
 */

@RestController
@RequestMapping("soam")
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableResourceServer
public class SoamController {

    @Autowired
    private final SoamService service;

    SoamController(SoamService soam){
        this.service = soam;
    }

    @PreAuthorize("#oauth2.hasScope('SOAM_LOGIN')")
    @GetMapping("/pen")
    public String performLogin(){
        return service.getRandomPen();
    }

}