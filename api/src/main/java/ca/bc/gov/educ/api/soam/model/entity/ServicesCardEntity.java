package ca.bc.gov.educ.api.soam.model.entity;

import java.util.Date;
import java.util.UUID;

import lombok.Data;


@Data
public class ServicesCardEntity {
  UUID servicesCardInfoID;
  UUID digitalIdentityID;
  String did;
  String userDisplayName;
  String givenName;
  String givenNames;
  String surname;
  String birthDate;
  String gender;
  String email;
  String streetAddress;
  String identityAssuranceLevel;
  String city;
  String province;  
  String country; 
  String postalCode;   
  String createUser;
  Date createDate;
  String updateUser;
  Date updateDate;
}
