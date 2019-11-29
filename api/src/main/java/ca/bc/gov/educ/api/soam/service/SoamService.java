package ca.bc.gov.educ.api.soam.service;

import java.util.Collections;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.api.digitalID.model.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.codetable.CodeTableUtils;
import ca.bc.gov.educ.api.soam.exception.InvalidParameterException;
import ca.bc.gov.educ.api.soam.model.SoamFirstLoginEntity;
import ca.bc.gov.educ.api.soam.model.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.rest.RestUtils;
import ca.bc.gov.educ.api.student.model.StudentEntity;

@Service
public class SoamService {
	
	@Autowired
	private CodeTableUtils codeTableUtils;
	
	@Autowired
	private ApplicationProperties props;

	@Autowired
	RestUtils restUtils;

    public SoamLoginEntity performLogin(String identifierType, String identifierValue, String userID) {
    	validateSearchParameters(identifierType, identifierValue);
		RestTemplate restTemplate = restUtils.getRestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		ResponseEntity<DigitalIDEntity> response;
		try {
			//This is the initial call to determine if we have this digital identity
			response = restTemplate.exchange(props.getDigitalIdentifierApiURL() + "/" + identifierType + "/" + identifierValue, HttpMethod.GET, new HttpEntity<>("parameters", headers), DigitalIDEntity.class);
			//If we've reached here we do have a digital identity for this user, if they have a student ID in the digital ID record then we fetch the student
			if(response.getBody().getStudentID() != null) {
				ResponseEntity<StudentEntity> studentResponse;
				studentResponse = restTemplate.exchange(props.getStudentApiURL() + "/" + response.getBody().getStudentID(), HttpMethod.GET, new HttpEntity<>("parameters", headers), StudentEntity.class);
				return createSoamLoginEntity(studentResponse.getBody());
			}
		} catch (final HttpClientErrorException e) {
		    if(e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
		    	DigitalIDEntity entity = createDigitalIdentity(identifierType, identifierValue, userID);
				response = restTemplate.postForEntity(props.getDigitalIdentifierApiURL(), entity, DigitalIDEntity.class);
		    }else {
		    	throw new RuntimeException("Unexpected HTTP return code: " + e.getStatusCode() + " error message: " + e.getResponseBodyAsString());
		    }
		}
		
        return null;
    }
    
    private SoamLoginEntity createSoamLoginEntity(StudentEntity student) {
    	SoamLoginEntity entity = new SoamLoginEntity();
    	entity.setStudent(student);
    	return entity;
    }
    
    private DigitalIDEntity createDigitalIdentity(String identityTypeCode, String identityValue, String userID) {
    	DigitalIDEntity entity = new DigitalIDEntity();
    	entity.setIdentityTypeCode(identityTypeCode);
    	entity.setIdentityValue(identityValue);
    	entity.setLastAccessChannelCode(codeTableUtils.getAllAccessChannelCodes().get("OSPR").getAccessChannelCode());
    	entity.setLastAccessDate(new Date());
    	entity.setCreateUser(userID);
    	entity.setUpdateUser(userID);
    	return entity;
    }
    
    private void validateSearchParameters(String identifierType, String identifierValue) throws InvalidParameterException {
        if(identifierType==null || !codeTableUtils.getAllIdentifierTypeCodes().containsKey(identifierType)) {
            throw new InvalidParameterException("identifierType");
        }else  if(identifierValue==null || identifierValue.length()<1) {
            throw new InvalidParameterException("identifierValue");
        }
       
    }


}