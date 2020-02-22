package ca.bc.gov.educ.api.soam.model.entity;

import ca.bc.gov.educ.api.soam.model.SoamServicesCard;
import ca.bc.gov.educ.api.soam.model.SoamStudent;
import lombok.Data;

import java.util.UUID;

@Data
public class SoamLoginEntity {

	private SoamStudent student;
	private SoamServicesCard serviceCard;
	private UUID digitalIdentityID;
}
