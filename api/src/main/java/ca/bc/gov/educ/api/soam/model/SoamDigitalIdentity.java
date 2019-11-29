package ca.bc.gov.educ.api.soam.model;

public class SoamDigitalIdentity {

	private Long digitalIdentityID;
	private String firstName;
	private String lastName;
	private String middleNames;
	private String emailAddress;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getMiddleNames() {
		return middleNames;
	}

	public void setMiddleNames(String middleNames) {
		this.middleNames = middleNames;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public Long getDigitalIdentityID() {
		return digitalIdentityID;
	}

	public void setDigitalIdentityID(Long digitalIdentityID) {
		this.digitalIdentityID = digitalIdentityID;
	}

}