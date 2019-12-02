package ca.bc.gov.educ.api.soam.model;

public class SoamLoginEntity {

	private SoamStudent student;
	private Long digitalIdentityID;

	public SoamStudent getStudent() {
		return student;
	}

	public void setStudent(SoamStudent student) {
		this.student = student;
	}

	public Long getDigitalIdentityID() {
		return digitalIdentityID;
	}

	public void setDigitalIdentityID(Long digitalIdentityID) {
		this.digitalIdentityID = digitalIdentityID;
	}

}