package ca.bc.gov.educ.api.soam.util;

import ca.bc.gov.educ.api.soam.model.SoamServicesCard;
import ca.bc.gov.educ.api.soam.model.SoamStudent;
import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.model.entity.SoamLoginEntity;
import ca.bc.gov.educ.api.soam.model.entity.StudentEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class SoamUtil {

  public DigitalIDEntity createDigitalIdentity(String identityTypeCode, String identityValue) {
    DigitalIDEntity entity = new DigitalIDEntity();
    entity.setIdentityTypeCode(identityTypeCode);
    entity.setIdentityValue(identityValue);
    entity.setLastAccessChannelCode("OSPR");
    entity.setLastAccessDate(LocalDateTime.now().minusMinutes(1).toString());
    entity.setCreateUser("SOAM");
    entity.setUpdateUser("SOAM");
    return entity;
  }

  public DigitalIDEntity getUpdatedDigitalId(DigitalIDEntity digitalIDEntity) {
    digitalIDEntity.setLastAccessDate(LocalDateTime.now().minusMinutes(1).toString());
    digitalIDEntity.setCreateDate(null);
    digitalIDEntity.setUpdateDate(null);
    return digitalIDEntity;
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
      serviceCard.setCity(serviceCardEntity.getCity());
      serviceCard.setCountry(serviceCardEntity.getCountry());
      serviceCard.setDid(serviceCardEntity.getDid());
      serviceCard.setEmail(serviceCardEntity.getEmail());
      serviceCard.setGender(serviceCardEntity.getGender());
      serviceCard.setGivenName(serviceCardEntity.getGivenName());
      serviceCard.setGivenNames(serviceCardEntity.getGivenNames());
      serviceCard.setPostalCode(serviceCardEntity.getPostalCode());
      serviceCard.setIdentityAssuranceLevel(serviceCardEntity.getIdentityAssuranceLevel());
      serviceCard.setProvince(serviceCardEntity.getProvince());
      serviceCard.setStreetAddress(serviceCardEntity.getStreetAddress());
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
  }
}
