package ca.bc.gov.educ.api.soam.util;

import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class SoamUtil {

  public DigitalIDEntity createDigitalIdentity(String identityTypeCode, String identityValue, String userID) {
    DigitalIDEntity entity = new DigitalIDEntity();
    entity.setIdentityTypeCode(identityTypeCode);
    entity.setIdentityValue(identityValue);
    entity.setLastAccessChannelCode("OSPR");
    entity.setLastAccessDate(LocalDateTime.now().toString());
    entity.setCreateUser(userID);
    entity.setUpdateUser(userID);
    return entity;
  }

  public DigitalIDEntity getUpdatedDigitalId(DigitalIDEntity digitalIDEntity) {
    digitalIDEntity.setLastAccessDate(LocalDateTime.now().toString());
    digitalIDEntity.setCreateDate(null);
    digitalIDEntity.setUpdateDate(null);
    return digitalIDEntity;
  }
}
