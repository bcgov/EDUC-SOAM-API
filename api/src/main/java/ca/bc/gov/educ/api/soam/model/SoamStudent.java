package ca.bc.gov.educ.api.soam.model;

import java.util.Date;

public class SoamStudent {
	private Long studentID;
	private String pen;
	private String legalFirstName;
	private String legalMiddleNames;
	private String legalLastName;
	private Date dob;
	private char sexCode;
	private char genderCode;
	private String dataSourceCode;
	private String usualFirstName;
	private String usualMiddleNames;
	private String usualLastName;
	private String email;
	private Date deceasedDate;
	private String createUser;
	private Date createDate;
	private String updateUser;
	private Date updateDate;

	public Long getStudentID() {
		return studentID;
	}

	public void setStudentID(Long studentID) {
		this.studentID = studentID;
	}

	public String getPen() {
		return pen;
	}

	public void setPen(String pen) {
		this.pen = pen;
	}

	public String getLegalFirstName() {
		return legalFirstName;
	}

	public void setLegalFirstName(String legalFirstName) {
		this.legalFirstName = legalFirstName;
	}

	public String getLegalMiddleNames() {
		return legalMiddleNames;
	}

	public void setLegalMiddleNames(String legalMiddleNames) {
		this.legalMiddleNames = legalMiddleNames;
	}

	public String getLegalLastName() {
		return legalLastName;
	}

	public void setLegalLastName(String legalLastName) {
		this.legalLastName = legalLastName;
	}

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public char getSexCode() {
		return sexCode;
	}

	public void setSexCode(char sexCode) {
		this.sexCode = sexCode;
	}

	public char getGenderCode() {
		return genderCode;
	}

	public void setGenderCode(char genderCode) {
		this.genderCode = genderCode;
	}

	public String getDataSourceCode() {
		return dataSourceCode;
	}

	public void setDataSourceCode(String dataSourceCode) {
		this.dataSourceCode = dataSourceCode;
	}

	public String getUsualFirstName() {
		return usualFirstName;
	}

	public void setUsualFirstName(String usualFirstName) {
		this.usualFirstName = usualFirstName;
	}

	public String getUsualMiddleNames() {
		return usualMiddleNames;
	}

	public void setUsualMiddleNames(String usualMiddleNames) {
		this.usualMiddleNames = usualMiddleNames;
	}

	public String getUsualLastName() {
		return usualLastName;
	}

	public void setUsualLastName(String usualLastName) {
		this.usualLastName = usualLastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Date getDeceasedDate() {
		return deceasedDate;
	}

	public void setDeceasedDate(Date deceasedDate) {
		this.deceasedDate = deceasedDate;
	}

	public String getCreateUser() {
		return createUser;
	}

	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getUpdateUser() {
		return updateUser;
	}

	public void setUpdateUser(String updateUser) {
		this.updateUser = updateUser;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

}
