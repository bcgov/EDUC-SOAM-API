package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.model.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.StudentEntity;
import ca.bc.gov.educ.api.soam.service.SoamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.validation.annotation.Validated;
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

    @PreAuthorize("#oauth2.hasScope('POST_SOAM')")
    @PostMapping()
    public StudentEntity soamLogin(@Validated @RequestBody DigitalIDEntity digitalID) throws Exception{
        DigitalIDEntity response = service.getDigitalID(digitalID.getDigitalID());

        /*
        If the Digital ID does not exist, create new one with NULL Student ID
         */
        if(response == null){
            service.createDigitalID(digitalID);
            return null;
        }
        /*
        If the Digital ID does exist, update LastAccessTime and LastAccessChannel then get Student based on SudentID
         */
        else {
            service.updateDigitalID(digitalID);
            StudentEntity student = service.getStudent(digitalID.getStudentID());
            return student;
        }

    }


}