package ca.bc.gov.educ.api.soam.service;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;

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
import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.model.SoamStudent;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.rest.RestUtils;
import ca.bc.gov.educ.api.student.model.StudentEntity;

@Service
public class SoamService {

  private static final String PARAMETERS_ATTRIBUTE = "parameters";

  private final CodeTableUtils codeTableUtils;

  private final ApplicationProperties props;

  private final RestUtils restUtils;

  public SoamService(@Autowired final CodeTableUtils codeTableUtils, @Autowired final ApplicationProperties props, @Autowired final RestUtils restUtils) {
    this.codeTableUtils = codeTableUtils;
    this.props = props;
    this.restUtils = restUtils;
  }

  public void performLogin(String identifierType, String identifierValue, String userID) {
    validateExtendedSearchParameters(identifierType, identifierValue, userID);
    RestTemplate restTemplate = restUtils.getRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

    ResponseEntity<DigitalIDEntity> response;
    try {
      //This is the initial call to determine if we have this digital identity
      response = restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=" + identifierType + "&identityvalue=" + identifierValue, HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
    } catch (final HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        //Digital Identity does not exist, let's create it
        DigitalIDEntity entity = createDigitalIdentity(identifierType, identifierValue, userID);
        restTemplate.postForEntity(props.getDigitalIdentifierApiURL(), entity, DigitalIDEntity.class);
        return;
      } else {
        throw new SoamRuntimeException(getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
    try {
      //Update the last used date
      DigitalIDEntity digitalIDEntity = response.getBody();
      digitalIDEntity.setLastAccessDate(new Date());
      digitalIDEntity.setCreateDate(null);
      digitalIDEntity.setUpdateDate(null);
      restTemplate.put(props.getDigitalIdentifierApiURL(), digitalIDEntity, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
    } catch (final HttpClientErrorException e) {
      throw new SoamRuntimeException(getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
    }
  }

  public SoamLoginEntity getSoamLoginEntity(String identifierType, String identifierValue) {
    validateSearchParameters(identifierType, identifierValue);
    RestTemplate restTemplate = restUtils.getRestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

    ResponseEntity<DigitalIDEntity> response;
    DigitalIDEntity digitalIDEntity = null;
    try {
      //This is the initial call to determine if we have this digital identity
      response = restTemplate.exchange(props.getDigitalIdentifierApiURL() + "?identitytype=" + identifierType + "&identityvalue=" + identifierValue, HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), DigitalIDEntity.class);
      digitalIDEntity = response.getBody();
      if (digitalIDEntity == null) {
        throw new SoamRuntimeException("Digital ID was null - unexpected error");
      }
    } catch (final HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        //This should not occur
        throw new SoamRuntimeException("Digital identity was not found. IdentifierType: " + identifierType + " IdentifierValue: " + identifierValue);
      } else {
        throw new SoamRuntimeException(getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
    try {
      //If we've reached here we do have a digital identity for this user, if they have a student ID in the digital ID record then we fetch the student
      if (digitalIDEntity.getStudentID() != null) {
        ResponseEntity<StudentEntity> studentResponse;
        studentResponse = restTemplate.exchange(props.getStudentApiURL() + "/" + response.getBody().getStudentID(), HttpMethod.GET, new HttpEntity<>(PARAMETERS_ATTRIBUTE, headers), StudentEntity.class);
        return createSoamLoginEntity(studentResponse.getBody(), digitalIDEntity.getDigitalID());
      } else {
        return createSoamLoginEntity(null, digitalIDEntity.getDigitalID());
      }
    } catch (final HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        throw new SoamRuntimeException("Student was not found. URL was: " + props.getStudentApiURL() + "/" + response.getBody().getStudentID());
      } else {
        throw new SoamRuntimeException(getErrorMessageString(e.getStatusCode(), e.getResponseBodyAsString()));
      }
    }
  }

  private String getErrorMessageString(HttpStatus status, String body) {
    return "Unexpected HTTP return code: " + status + " error message: " + body;
  }

  private SoamLoginEntity createSoamLoginEntity(StudentEntity student, UUID digitalIdentifierID) {
    SoamLoginEntity entity = new SoamLoginEntity();

    if (student != null) {
      SoamStudent soamStudent = new SoamStudent();

      soamStudent.setCreateDate(student.getCreateDate());
      soamStudent.setCreateUser(student.getCreateUser());
      soamStudent.setDataSourceCode(student.getDataSourceCode());
      soamStudent.setDeceasedDate(student.getDeceasedDate());
      soamStudent.setDob(student.getDob());
      soamStudent.setEmail(student.getEmail());
      soamStudent.setGenderCode(student.getGenderCode());
      soamStudent.setLegalFirstName(student.getLegalFirstName());
      soamStudent.setLegalLastName(student.getLegalLastName());
      soamStudent.setLegalMiddleNames(student.getLegalMiddleNames());
      soamStudent.setPen(student.getPen());
      soamStudent.setSexCode(student.getSexCode());
      soamStudent.setStudentID(student.getStudentID());
      soamStudent.setUpdateDate(student.getUpdateDate());
      soamStudent.setUpdateUser(student.getUpdateUser());
      soamStudent.setUsualFirstName(student.getUsualFirstName());
      soamStudent.setUsualLastName(student.getUsualLastName());
      soamStudent.setUsualMiddleNames(student.getUsualMiddleNames());

      entity.setStudent(soamStudent);
    }
    entity.setDigitalIdentityID(digitalIdentifierID);

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

  private void validateExtendedSearchParameters(String identifierType, String identifierValue, String userID) {
    if (identifierType == null || !codeTableUtils.getAllIdentifierTypeCodes().containsKey(identifierType)) {
      throw new InvalidParameterException("identifierType");
    } else if (identifierValue == null || identifierValue.length() < 1) {
      throw new InvalidParameterException("identifierValue");
    } else if (userID == null || userID.length() < 1) {
      throw new InvalidParameterException("userID");
    }
  }

  private void validateSearchParameters(String identifierType, String identifierValue) {
    if (identifierType == null || !codeTableUtils.getAllIdentifierTypeCodes().containsKey(identifierType)) {
      throw new InvalidParameterException("identifierType");
    } else if (identifierValue == null || identifierValue.length() < 1) {
      throw new InvalidParameterException("identifierValue");
    }
  }


}
