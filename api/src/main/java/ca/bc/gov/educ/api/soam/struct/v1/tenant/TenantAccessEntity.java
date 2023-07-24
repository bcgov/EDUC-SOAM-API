package ca.bc.gov.educ.api.soam.struct.v1.tenant;

public class TenantAccessEntity {

	private String clientID;
	private String tenantID;
	private String isValid;

	public String getClientID() {
		return clientID;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}

	public String getTenantID() {
		return tenantID;
	}

	public void setTenantID(String tenantID) {
		this.tenantID = tenantID;
	}

	public String getIsValid() {
		return isValid;
	}

	public void setIsValid(String isValid) {
		this.isValid = isValid;
	}
}
