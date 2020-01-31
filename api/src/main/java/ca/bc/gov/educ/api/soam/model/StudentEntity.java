package ca.bc.gov.educ.api.soam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentEntity {
  UUID studentID;
  String pen;
  String legalFirstName;
  String legalMiddleNames;
  String legalLastName;
  Date dob;
  char sexCode;
  char genderCode;
  String dataSourceCode;
  String usualFirstName;
  String usualMiddleNames;
  String usualLastName;
  String email;
  Date deceasedDate;
  Date createDate;
  Date updateDate;
  String createUser;
  String updateUser;
}
