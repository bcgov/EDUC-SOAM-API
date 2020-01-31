package ca.bc.gov.educ.api.soam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
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
  private Date lastAccessDate;
  private String lastAccessChannelCode;
  private String createUser;
  private Date createDate;
  private String updateUser;
  private Date updateDate;
}
