package ca.bc.gov.educ.api.soam.model;

import ca.bc.gov.educ.api.student.model.StudentEntity;

public class SoamLoginEntity {

	private StudentEntity student;
	private SoamFirstLoginEntity firstLoginEntity;

	public StudentEntity getStudent() {
		return student;
	}

	public void setStudent(StudentEntity student) {
		this.student = student;
	}

	public SoamFirstLoginEntity getFirstLoginEntity() {
		return firstLoginEntity;
	}

	public void setFirstLoginEntity(SoamFirstLoginEntity firstLoginEntity) {
		this.firstLoginEntity = firstLoginEntity;
	}

}