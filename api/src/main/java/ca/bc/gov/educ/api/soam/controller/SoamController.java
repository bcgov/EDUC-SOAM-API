package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.model.StudentEntity;
import ca.bc.gov.educ.api.soam.service.SoamService;
import org.codehaus.jackson.map.util.JSONPObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.*;

/**
 * Digital Identity controller
 *
 * @author Nathan Denny
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
/*
    @PreAuthorize("#oauth2.hasScope('SOAM_LOGIN')")
    @PostMapping("/login")
    public StudentEntity soamLogin(@RequestBody String identityValue, String identityType) throws Exception{// not digital ID
        JSONObject response = service.getDigitalID(identityValue, identityType);

        //400 (user feedback), 404, 500, 502, 503, 504

        if(response == null){
            service.createDigitalID(identityValue);
            return null;
        }
        */

        /*
        If the Digital ID does exist, update LastAccessTime and LastAccessChannel then get Student based on SudentID
         *//*
        else {
            service.updateDigitalID(identityValue);
            StudentEntity student = service.getStudent(response);
            return student;
        }

    }*/

    @PreAuthorize("#oauth2.hasScope('READ_STUDENT')")
    @GetMapping("/pen")
    public String penTest(){
        return service.getRandomPen();
    }

}
