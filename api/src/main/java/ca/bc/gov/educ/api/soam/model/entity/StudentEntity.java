package ca.bc.gov.educ.api.soam.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  String dob;
  char sexCode;
  char genderCode;
  String dataSourceCode;
  String usualFirstName;
  String usualMiddleNames;
  String usualLastName;
  String email;
  String deceasedDate;
  String createDate;
  String updateDate;
  String createUser;
  String updateUser;
}
