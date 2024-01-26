package ca.bc.gov.educ.api.soam.util;

import ca.bc.gov.educ.api.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.api.soam.model.SoamServicesCard;
import ca.bc.gov.educ.api.soam.model.SoamStudent;
import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.model.entity.StudentEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Component
@Slf4j
public class SoamUtil {

  private DateTimeFormatter shortDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd");

  private DateTimeFormatter longDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public DigitalIDEntity createDigitalIdentity(String identityTypeCode, String identityValue) {
    DigitalIDEntity entity = new DigitalIDEntity();
    entity.setIdentityTypeCode(identityTypeCode);
    entity.setIdentityValue(identityValue);
    entity.setLastAccessChannelCode("OSPR");
    entity.setLastAccessDate(LocalDateTime.now().toString());
    entity.setCreateUser("SOAM");
    entity.setUpdateUser("SOAM");
    return entity;
  }

  public DigitalIDEntity getUpdatedDigitalId(DigitalIDEntity digitalIDEntity) {
    digitalIDEntity.setLastAccessDate(LocalDateTime.now().toString());
    digitalIDEntity.setCreateDate(null);
    digitalIDEntity.setUpdateDate(null);
    return digitalIDEntity;
  }

  public String getBCSCDobString(String dateOfBirth) {
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

  private LocalDate getValidShortDate(String dateStr) {
    try {
      return LocalDate.parse(dateStr, this.shortDateFormat);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public SoamLoginEntity createSoamLoginEntity(StudentEntity student, UUID digitalIdentifierID, ServicesCardEntity serviceCardEntity) {
    SoamLoginEntity entity = new SoamLoginEntity();

    setStudentEntity(student, entity);

    setServicesCard(digitalIdentifierID, serviceCardEntity, entity);

    entity.setDigitalIdentityID(digitalIdentifierID);

    return entity;
  }

  private void setServicesCard(UUID digitalIdentifierID, ServicesCardEntity serviceCardEntity, SoamLoginEntity entity) {
    if (serviceCardEntity != null) {
      SoamServicesCard serviceCard = new SoamServicesCard();
      serviceCard.setServicesCardInfoID(serviceCardEntity.getServicesCardInfoID());
      serviceCard.setDigitalIdentityID(digitalIdentifierID);
      serviceCard.setBirthDate(serviceCardEntity.getBirthDate());
      serviceCard.setDid(serviceCardEntity.getDid());
      serviceCard.setEmail(serviceCardEntity.getEmail());
      serviceCard.setGender(serviceCardEntity.getGender());
      serviceCard.setGivenName(serviceCardEntity.getGivenName());
      serviceCard.setGivenNames(serviceCardEntity.getGivenNames());
      serviceCard.setPostalCode(serviceCardEntity.getPostalCode());
      serviceCard.setIdentityAssuranceLevel(serviceCardEntity.getIdentityAssuranceLevel());
      serviceCard.setSurname(serviceCardEntity.getSurname());
      serviceCard.setUserDisplayName(serviceCardEntity.getUserDisplayName());
      serviceCard.setUpdateDate(serviceCardEntity.getUpdateDate());
      serviceCard.setUpdateUser(serviceCardEntity.getUpdateUser());
      serviceCard.setCreateDate(serviceCardEntity.getCreateDate());
      serviceCard.setCreateUser(serviceCardEntity.getCreateUser());

      entity.setServiceCard(serviceCard);
    }
  }

  private void setStudentEntity(StudentEntity student, SoamLoginEntity entity) {
    if (student != null) {
      SoamStudent soamStudent = new SoamStudent();
      soamStudent.setCreateDate(student.getCreateDate());
      soamStudent.setCreateUser(student.getCreateUser());
      soamStudent.setDeceasedDate(student.getDeceasedDate());
      soamStudent.setDob(student.getDob());
      soamStudent.setEmail(student.getEmail());
      if (student.getGenderCode() != null) {
        soamStudent.setGenderCode(student.getGenderCode().charAt(0));
      }
      soamStudent.setLegalFirstName(student.getLegalFirstName());
      soamStudent.setLegalLastName(student.getLegalLastName());
      soamStudent.setLegalMiddleNames(student.getLegalMiddleNames());
      soamStudent.setPen(student.getPen());
      if (student.getSexCode() != null) {
        soamStudent.setSexCode(student.getSexCode().charAt(0));
      }
      soamStudent.setStudentID(student.getStudentID());
      soamStudent.setUpdateDate(student.getUpdateDate());
      soamStudent.setUpdateUser(student.getUpdateUser());
      soamStudent.setUsualFirstName(student.getUsualFirstName());
      soamStudent.setUsualLastName(student.getUsualLastName());
      soamStudent.setUsualMiddleNames(student.getUsualMiddleNames());

      entity.setStudent(soamStudent);
    }
  }

  public static String toUpperCaseNullSafe(String val){
    if(StringUtils.isEmpty(val)){
      return null;
    }
    return val.toUpperCase();
  }
}
