package ca.bc.gov.educ.api.soam.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DigitalIDEntity {
  private UUID digitalID;
  private String studentID;
  private String identityTypeCode;
  private String identityValue;
  private String autoMatched;
  private String lastAccessDate;
  private String lastAccessChannelCode;
  private String createUser;
  private String createDate;
  private String updateUser;
  private String updateDate;
}
