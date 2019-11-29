package ca.bc.gov.educ.api.soam.model;

public class SoamLoginEntity {

	private SoamStudent student;
	private SoamDigitalIdentity soamDigitalIdentity;

	public SoamStudent getStudent() {
		return student;
	}

	public void setStudent(SoamStudent student) {
		this.student = student;
	}

	public SoamDigitalIdentity getSoamDigitalIdentity() {
		return soamDigitalIdentity;
	}

	public void setSoamDigitalIdentity(SoamDigitalIdentity soamDigitalIdentity) {
		this.soamDigitalIdentity = soamDigitalIdentity;
	}

}