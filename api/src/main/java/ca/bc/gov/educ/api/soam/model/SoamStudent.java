package ca.bc.gov.educ.api.soam.model;

import lombok.Data;

import java.util.UUID;

@Data
public class SoamStudent {
	private UUID studentID;
	private String pen;
	private String legalFirstName;
	private String legalMiddleNames;
	private String legalLastName;
	private String dob;
	private char sexCode;
	private char genderCode;
	private String dataSourceCode;
	private String usualFirstName;
	private String usualMiddleNames;
	private String usualLastName;
	private String email;
	private String deceasedDate;
	private String createUser;
	private String createDate;
	private String updateUser;
	private String updateDate;

}
