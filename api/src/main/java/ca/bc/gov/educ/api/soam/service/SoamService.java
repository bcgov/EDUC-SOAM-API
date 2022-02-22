package ca.bc.gov.educ.api.soam.service;

import ca.bc.gov.educ.api.soam.codetable.CodeTableUtils;
import ca.bc.gov.educ.api.soam.exception.InvalidParameterException;
import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.entity.*;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.rest.RestUtils;
import ca.bc.gov.educ.api.soam.struct.v1.penmatch.PenMatchResult;
import ca.bc.gov.educ.api.soam.struct.v1.penmatch.PenMatchStudent;
import ca.bc.gov.educ.api.soam.util.JsonUtil;
import ca.bc.gov.educ.api.soam.util.SoamUtil;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SoamService {

  private DateTimeFormatter shortDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");

  private DateTimeFormatter longDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private static final String BCSC = "BCSC";

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
    DigitalIDEntity digitalIDEntity = this.manageUserSetup(identifierType, identifierValue, servicesCard, correlationID);
    if(digitalIDEntity != null && digitalIDEntity.getStudentID() == null && identifierType.equals(BCSC)) {
      attemptBCSCAutoMatch(servicesCard, digitalIDEntity, correlationID);
    }
  }

  @RateLimiter(name = "performLink")
  public Pair<SoamLoginEntity, HttpStatus> performLink(final ServicesCardEntity servicesCard, final String correlationID) {
    if(log.isDebugEnabled()) {
      log.debug("performLink: servicesCard: {}", JsonUtil.getJsonPrettyStringFromObject(servicesCard));
    }
    this.validateExtendedSearchParameters(BCSC, servicesCard.getDid());
    return this.manageLinkage(servicesCard, correlationID);
  }

  private void updateDigitalID(final ServicesCardEntity servicesCard, final DigitalIDEntity digitalIDEntity, final String correlationID) {
    this.restUtils.updateDigitalID(digitalIDEntity, correlationID);
    if (servicesCard != null) {
      this.createOrUpdateBCSC(servicesCard, digitalIDEntity, correlationID);
    }
  }

  private Pair<SoamLoginEntity, HttpStatus> manageLinkage(final ServicesCardEntity servicesCard, final String correlationID) {
    DigitalIDEntity digitalIDEntity = manageUserSetup(BCSC, servicesCard.getDid(), servicesCard, correlationID);
    HttpStatus status;
    if (digitalIDEntity == null) {
      throw new SoamRuntimeException("Unexpected error; digitalID is null after manageUserSetup");
    }
    if(digitalIDEntity.getStudentID() == null) {
       status = attemptBCSCAutoMatch(servicesCard, digitalIDEntity, correlationID);
    }else{
       status = HttpStatus.OK;
    }
    return Pair.of(populateAndReturnLoginEntity(digitalIDEntity, servicesCard, correlationID), status);
  }

  private DigitalIDEntity manageUserSetup(final String identifierType, final String identifierValue, final ServicesCardEntity servicesCard, final String correlationID) {
    val didResponseFromAPI = this.restUtils.getDigitalID(identifierType, identifierValue.toUpperCase(), correlationID);
    if (didResponseFromAPI.isPresent()) {
      this.updateDigitalID(servicesCard, didResponseFromAPI.get(), correlationID); //update Digital Id if we have one.
      return didResponseFromAPI.get();
    } else {
      val responseEntity = this.restUtils.createDigitalID(identifierType, identifierValue.toUpperCase(), correlationID);
      if (servicesCard != null && responseEntity != null) {
        this.createOrUpdateBCSC(servicesCard, responseEntity, correlationID);
      }
      return responseEntity;
    }
  }

  public void createOrUpdateBCSC(final ServicesCardEntity servicesCard, final DigitalIDEntity digitalIDEntity, final String correlationID) {
    log.debug("createOrUpdateBCSC: servicesCard DID: {}", servicesCard.getDid());
    servicesCard.setDigitalIdentityID(digitalIDEntity.getDigitalID());
    val servicesCardFromAPIResponse = this.restUtils.getServicesCard(servicesCard.getDid(), correlationID);
    if (servicesCardFromAPIResponse.isPresent()) {
      ServicesCardEntity scEntity = servicesCardFromAPIResponse.get();
      updateBCSCInfo(servicesCard, scEntity);
      this.updateBCSC(scEntity, correlationID);
    } else {
      servicesCard.setBirthDate(getBCSCDobString(servicesCard.getBirthDate()));
      this.restUtils.createServicesCard(servicesCard, correlationID);
    }

  }

  public LocalDate getValidShortDate(String dateStr) {
    try {
      return LocalDate.parse(dateStr, this.shortDateFormat);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private String getBCSCDobString(String dateOfBirth) {
    LocalDate dob;
    try {
      dob = getValidShortDate(dateOfBirth);
      if(dob != null) {
        return dob.format(this.longDateFormat);
      }
      return dateOfBirth;
    }catch (Exception e) {
      log.error("Invalid BCSC birth date: {}", dateOfBirth);
      throw new SoamRuntimeException("Invalid BCSC birth date: " + dateOfBirth);
    }
  }

  private void updateBCSCInfo(ServicesCardEntity servicesCard, ServicesCardEntity scEntity) {
    scEntity.setBirthDate(getBCSCDobString(servicesCard.getBirthDate()));
    scEntity.setEmail(servicesCard.getEmail());
    scEntity.setGender(servicesCard.getGender());
    scEntity.setGivenName(servicesCard.getGivenName());
    scEntity.setGivenNames(servicesCard.getGivenNames());
    scEntity.setPostalCode(servicesCard.getPostalCode());
    scEntity.setIdentityAssuranceLevel(servicesCard.getIdentityAssuranceLevel());
    scEntity.setSurname(servicesCard.getSurname());
    scEntity.setUserDisplayName(servicesCard.getUserDisplayName());
    scEntity.setDid(servicesCard.getDid());
  }

  private HttpStatus attemptBCSCAutoMatch(final ServicesCardEntity servicesCard, final DigitalIDEntity digitalIDEntity, final String correlationID) {
    log.debug("Attempting to auto match BCSC for DID: {}", servicesCard.getDid());
    PenMatchStudent penMatchStudent = new PenMatchStudent();
    penMatchStudent.setSurname(servicesCard.getSurname());
    penMatchStudent.setGivenName(servicesCard.getGivenName());
    if (servicesCard.getGivenNames() != null && servicesCard.getGivenName() != null) {
      penMatchStudent.setMiddleName(servicesCard.getGivenNames().replaceAll(servicesCard.getGivenName(), "").trim());
    } else if (servicesCard.getGivenNames() != null) {
      penMatchStudent.setMiddleName(servicesCard.getGivenNames());
    }
    penMatchStudent.setDob(servicesCard.getBirthDate().replace("-", ""));
    penMatchStudent.setSex(servicesCard.getGender());
    penMatchStudent.setPostal(servicesCard.getPostalCode());

    if(log.isDebugEnabled()){
      log.debug("Attempting to auto match BCSC with pen match student: {}", JsonUtil.getJsonPrettyStringFromObject(penMatchStudent));
    }

    Optional<PenMatchResult> optional = this.restUtils.postToMatchAPI(penMatchStudent);
    if(optional.isPresent()) {
      log.debug("Auto match result status: {} for DID: {}", optional.get().getPenStatus(), servicesCard.getDid());
      return evaluateAndLinkBCSCResult(optional.get(), digitalIDEntity, correlationID);
    }else{
      throw new SoamRuntimeException("Error occurred while calling Match API");
    }
  }

  private HttpStatus evaluateAndLinkBCSCResult(final PenMatchResult penMatchResult, final DigitalIDEntity digitalIDEntity, final String correlationID) {
    if(penMatchResult == null || penMatchResult.getPenStatus() == null) {
      throw new SoamRuntimeException("Error occurred while calling Match API");
    }
    switch (penMatchResult.getPenStatus()) {
      case "B1":
      case "C1":
      case "D1":
        removePreviousDigitalIdentityLinks(penMatchResult.getMatchingRecords().get(0).getStudentID(), correlationID);
        digitalIDEntity.setStudentID(penMatchResult.getMatchingRecords().get(0).getStudentID());
        digitalIDEntity.setAutoMatchedDate(LocalDateTime.now().toString());
        log.debug("Updating digital identity after auto match for digital identity: {} student ID: {}", digitalIDEntity.getDigitalID(), digitalIDEntity.getStudentID());
        this.restUtils.updateDigitalID(digitalIDEntity, correlationID);
        break;
      case "BM":
      case "CM":
      case "DM":
        return HttpStatus.MULTIPLE_CHOICES;
      default:
    }
    return HttpStatus.OK;
  }

  private void removePreviousDigitalIdentityLinks(final String studentID, final String correlationID){
    val didResponseFromAPI = this.restUtils.getDigitalIDByStudentID(studentID, correlationID);
    for(val didEntity : didResponseFromAPI) {
      didEntity.setStudentID(null);
      this.restUtils.updateDigitalID(didEntity, correlationID);
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
