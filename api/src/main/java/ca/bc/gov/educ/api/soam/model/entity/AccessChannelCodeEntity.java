package ca.bc.gov.educ.api.soam.model.entity;

import java.util.Date;

import lombok.Data;

@Data
public class AccessChannelCodeEntity {
  String accessChannelCode;
  String label;
  String description;
  Integer displayOrder;
  Date effectiveDate;
  Date expiryDate;
  String createUser;
  Date createDate;
  String updateUser;
  Date updateDate;
}
