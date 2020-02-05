package ca.bc.gov.educ.api.soam.model.entity;

import java.util.Date;
import java.util.UUID;

import lombok.Data;

@Data
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
  String createUser;
  Date createDate;
  String updateUser;
  Date updateDate;
}