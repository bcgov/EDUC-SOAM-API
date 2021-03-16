package ca.bc.gov.educ.api.soam.service;

import ca.bc.gov.educ.api.soam.codetable.CodeTableUtils;
import ca.bc.gov.educ.api.soam.exception.InvalidParameterException;
import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.model.entity.StudentEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.rest.RestUtils;
import ca.bc.gov.educ.api.soam.util.SoamUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

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

  public void performLogin(final String identifierType, final String identifierValue, final ServicesCardEntity servicesCard) {
    this.validateExtendedSearchParameters(identifierType, identifierValue);
    this.manageLogin(identifierType, identifierValue, servicesCard);
  }

  private void updateDigitalID(final ServicesCardEntity servicesCard, final DigitalIDEntity digitalIDEntity) {
    this.restUtils.updateDigitalID(digitalIDEntity);
    if (servicesCard != null) {
      this.createOrUpdateBCSC(servicesCard, digitalIDEntity.getDigitalID());
    }
  }

  private void manageLogin(final String identifierType, final String identifierValue, final ServicesCardEntity servicesCard) {
    val didResponseFromAPI = this.restUtils.getDigitalID(identifierType, identifierValue.toUpperCase());
    if (didResponseFromAPI.isPresent()) {
      this.updateDigitalID(servicesCard, didResponseFromAPI.get()); //update Digital Id if we have one.
    } else {
      val responseEntity = this.restUtils.createDigitalID(identifierType, identifierValue.toUpperCase());
      if (servicesCard != null && responseEntity != null) {
        this.createOrUpdateBCSC(servicesCard, responseEntity.getDigitalID());
      }
    }
  }

  public void createOrUpdateBCSC(final ServicesCardEntity servicesCard, final UUID digitalIdentityID) {
    servicesCard.setDigitalIdentityID(digitalIdentityID);
    val servicesCardFromAPIResponse = this.restUtils.getServicesCard(servicesCard.getDid());
    if (servicesCardFromAPIResponse.isPresent()) {
      this.updateBCSC(servicesCardFromAPIResponse.get());
    } else {
      this.restUtils.createServicesCard(servicesCard);
    }
  }

  private void updateBCSC(final ServicesCardEntity servicesCardEntity) {
    assert servicesCardEntity != null;
    servicesCardEntity.setCreateDate(null);
    servicesCardEntity.setUpdateDate(null);
    this.restUtils.updateServicesCard(servicesCardEntity);
  }

  public SoamLoginEntity getSoamLoginEntity(final String identifierType, final String identifierValue) {
    this.validateSearchParameters(identifierType, identifierValue);
    final DigitalIDEntity digitalIDEntity = this.getDigitalIDEntityForLogin(identifierType, identifierValue);
    //If we've reached here we do have a digital identity for this user, if they have a student ID in the digital ID record then we fetch the student
    ServicesCardEntity serviceCardEntity = null;
    if (identifierType.equals(ApplicationProperties.BCSC)) {
      serviceCardEntity =
          this.restUtils.getServicesCard(identifierValue).orElseThrow();
    }
    if (digitalIDEntity.getStudentID() != null) {
      final StudentEntity studentResponse;
      studentResponse = this.restUtils.getStudentByStudentID(digitalIDEntity.getStudentID());
      return this.soamUtil.createSoamLoginEntity(studentResponse, digitalIDEntity.getDigitalID(), serviceCardEntity);
    } else {
      return this.soamUtil.createSoamLoginEntity(null, digitalIDEntity.getDigitalID(), serviceCardEntity);
    }

  }


  private DigitalIDEntity getDigitalIDEntityForLogin(final String identifierType, final String identifierValue) {
    //This is the initial call to determine if we have this digital identity
    val response = this.restUtils.getDigitalID(identifierType, identifierValue);
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
}
