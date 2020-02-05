package ca.bc.gov.educ.api.soam.model;

import java.util.Date;
import java.util.UUID;

import lombok.Data;


@Data
public class SoamServicesCard {
  UUID servicesCardInfoID;
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
  String postalCode;   
  String createUser;
  Date createDate;
  String updateUser;
  Date updateDate;
}
