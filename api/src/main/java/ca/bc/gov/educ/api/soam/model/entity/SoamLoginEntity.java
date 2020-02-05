package ca.bc.gov.educ.api.soam.model.entity;

import java.util.UUID;

import ca.bc.gov.educ.api.soam.model.SoamServicesCard;
import ca.bc.gov.educ.api.soam.model.SoamStudent;

public class SoamLoginEntity {

	private SoamStudent student;
	private SoamServicesCard serviceCard;
	private UUID digitalIdentityID;

	public SoamStudent getStudent() {
		return student;
	}

	public void setStudent(SoamStudent student) {
		this.student = student;
	}

	public UUID getDigitalIdentityID() {
		return digitalIdentityID;
	}

	public void setDigitalIdentityID(UUID digitalIdentityID) {
		this.digitalIdentityID = digitalIdentityID;
	}

	public SoamServicesCard getServiceCard() {
		return serviceCard;
	}

	public void setServiceCard(SoamServicesCard serviceCard) {
		this.serviceCard = serviceCard;
	}

}