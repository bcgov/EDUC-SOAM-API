package ca.bc.gov.educ.api.soam.service;

import ca.bc.gov.educ.api.soam.codetable.CodeTableUtils;
import ca.bc.gov.educ.api.soam.exception.InvalidParameterException;
import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.entity.*;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.rest.RestUtils;
import ca.bc.gov.educ.api.soam.util.SoamUtil;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SoamService {

  private final CodeTableUtils codeTableUtils;


  private final SoamUtil soamUtil;


  private final RestUtils restUtils;

  @Autowired
  public SoamService(final CodeTableUtils codeTableUtils, final SoamUtil util, final RestUtils restUtils) {
    this.codeTableUtils = codeTableUtils;
    this.soamUtil = util;
    this.restUtils = restUtils;
  }

  @RateLimiter(name = "performLogin")
  public void performLogin(final String identifierType, final String identifierValue, final ServicesCardEntity servicesCard, final String correlationID) {
    this.validateExtendedSearchParameters(identifierType, identifierValue);
    this.manageLogin(identifierType, identifierValue, servicesCard, correlationID);
  }

  private void updateDigitalID(final ServicesCardEntity servicesCard, final DigitalIDEntity digitalIDEntity, final String correlationID) {
    this.restUtils.updateDigitalID(digitalIDEntity, correlationID);
    if (servicesCard != null) {
      this.createOrUpdateBCSC(servicesCard, digitalIDEntity.getDigitalID(), correlationID);
    }
  }

  private void manageLogin(final String identifierType, final String identifierValue, final ServicesCardEntity servicesCard, final String correlationID) {
    val didResponseFromAPI = this.restUtils.getDigitalID(identifierType, identifierValue.toUpperCase(), correlationID);
    if (didResponseFromAPI.isPresent()) {
      this.updateDigitalID(servicesCard, didResponseFromAPI.get(), correlationID); //update Digital Id if we have one.
    } else {
      val responseEntity = this.restUtils.createDigitalID(identifierType, identifierValue.toUpperCase(), correlationID);
      if (servicesCard != null && responseEntity != null) {
        this.createOrUpdateBCSC(servicesCard, responseEntity.getDigitalID(), correlationID);
      }
    }
  }

  public void createOrUpdateBCSC(final ServicesCardEntity servicesCard, final UUID digitalIdentityID, final String correlationID) {
    servicesCard.setDigitalIdentityID(digitalIdentityID);
    val servicesCardFromAPIResponse = this.restUtils.getServicesCard(servicesCard.getDid(), correlationID);
    if (servicesCardFromAPIResponse.isPresent()) {
      this.updateBCSC(servicesCardFromAPIResponse.get(), correlationID);
    } else {
      this.restUtils.createServicesCard(servicesCard, correlationID);
    }
  }

  private void updateBCSC(final ServicesCardEntity servicesCardEntity, final String correlationID) {
    assert servicesCardEntity != null;
    servicesCardEntity.setCreateDate(null);
    servicesCardEntity.setUpdateDate(null);
    this.restUtils.updateServicesCard(servicesCardEntity, correlationID);
  }

  @RateLimiter(name = "getSoamLoginEntity")
  public SoamLoginEntity getSoamLoginEntity(final String identifierType, final String identifierValue, final String correlationID) {
    this.validateSearchParameters(identifierType, identifierValue);
    val digitalIDEntity = this.getDigitalIDEntityForLogin(identifierType, identifierValue, correlationID);
    //If we've reached here we do have a digital identity for this user, if they have a student ID in the digital ID record then we fetch the student
    ServicesCardEntity serviceCardEntity = null;
    if (identifierType.equals(ApplicationProperties.BCSC)) {
      serviceCardEntity =
        this.restUtils.getServicesCard(identifierValue, correlationID).orElseThrow();
    }
    return populateAndReturnLoginEntity(digitalIDEntity, serviceCardEntity, correlationID);
  }

  @RateLimiter(name = "getSoamLoginEntityDID")
  public SoamLoginEntity getSoamLoginEntity(final String digitalIdentityID, final String correlationID) {
    if (digitalIdentityID == null) {
      log.error("Invalid digital identity ID - null");
      throw new InvalidParameterException("digitalIdentityID");
    }
    val digitalIDEntity = this.getDigitalIDEntityForLogin(digitalIdentityID, correlationID);
    //If we've reached here we do have a digital identity for this user, if they have a student ID in the digital ID record then we fetch the student
    ServicesCardEntity serviceCardEntity = null;
    if (digitalIDEntity.getIdentityTypeCode().equals(ApplicationProperties.BCSC)) {
      serviceCardEntity =
        this.restUtils.getServicesCard(digitalIDEntity.getIdentityValue(), correlationID).orElseThrow();
    }
    return populateAndReturnLoginEntity(digitalIDEntity, serviceCardEntity, correlationID);
  }

  private SoamLoginEntity populateAndReturnLoginEntity(final DigitalIDEntity digitalIDEntity, final ServicesCardEntity serviceCardEntity, final String correlationID) {
    if (digitalIDEntity.getStudentID() != null) {
      StudentEntity studentResponse;
      String studentID = digitalIDEntity.getStudentID();
      do {// recursion to find the true student.
        studentResponse = this.restUtils.getStudentByStudentID(studentID, correlationID);
        if ("M".equals(studentResponse.getStatusCode())) {
          studentID = studentResponse.getTrueStudentID().toString();
        }
      } while ("M".equalsIgnoreCase(studentResponse.getStatusCode())); // go up the ladder to find true student.
      return this.soamUtil.createSoamLoginEntity(studentResponse, digitalIDEntity.getDigitalID(), serviceCardEntity);
    } else {
      return this.soamUtil.createSoamLoginEntity(null, digitalIDEntity.getDigitalID(), serviceCardEntity);
    }
  }

  private DigitalIDEntity getDigitalIDEntityForLogin(final String identifierType, final String identifierValue, final String correlationID) {
    //This is the initial call to determine if we have this digital identity
    val response = this.restUtils.getDigitalID(identifierType, identifierValue, correlationID);
    if (response.isEmpty()) {
      throw new SoamRuntimeException("Digital ID was null - unexpected error");
    }
    return response.get();
  }

  private DigitalIDEntity getDigitalIDEntityForLogin(final String digitalIdentityID, final String correlationID) {
    //This is the initial call to determine if we have this digital identity
    val response = this.restUtils.getDigitalID(digitalIdentityID, correlationID);
    if (response.isEmpty()) {
      throw new SoamRuntimeException("Digital ID was null - unexpected error");
    }
    return response.get();
  }

  private void validateExtendedSearchParameters(final String identifierType, final String identifierValue) {
    this.validateSearchParameters(identifierType, identifierValue);
  }

  private void validateSearchParameters(final String identifierType, final String identifierValue) {
    if (identifierType == null || !this.codeTableUtils.getAllIdentifierTypeCodes().containsKey(identifierType)) {
      log.error("Invalid Identifier Type :: {}", identifierType);
      throw new InvalidParameterException("identifierType");
    } else if (identifierValue == null || identifierValue.length() < 1) {
      throw new InvalidParameterException("identifierValue");
    }
  }

  public List<String> getStsRolesBySSoGuid(final String ssoGuid, final String correlationID) {
    val stsPrincipalEntityOptional = this.restUtils.getStsLoginPrincipal(ssoGuid, correlationID);
    if (stsPrincipalEntityOptional.isEmpty()) {
      return Collections.emptyList();
    } else {
      return stsPrincipalEntityOptional.map(stsLoginPrincipalEntity -> stsLoginPrincipalEntity.getIsdRoles().stream().map(StsRolesEntity::getRole).collect(Collectors.toList())).orElse(Collections.emptyList());
    }
  }
}
