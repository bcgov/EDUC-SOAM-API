package ca.bc.gov.educ.api.soam.model;

import java.util.Date;
import java.util.UUID;

import lombok.Data;


@Data
public class SoamServicesCard {
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
  String city;
  String province;  
  String country;
  String identityAssuranceLevel; 
  String postalCode;   
  String createUser;
  Date createDate;
  String updateUser;
  Date updateDate;
}