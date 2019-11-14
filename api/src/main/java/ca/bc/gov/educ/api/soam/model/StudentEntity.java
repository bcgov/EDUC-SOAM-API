package ca.bc.gov.educ.api.soam.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.util.Date;

/**
 * Digital Identity Entity
 *
 * @author Nathan Denny
 */

@Entity
@Data
@Table(name = "Student")
public class StudentEntity {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "student_id")
    Integer studentID;

    @NotNull(message="PEN cannot be null")
    @Column(name = "pen")
    String pen;

    @Column(name = "legal_first_name")
    String legalFirstName;

    @Column(name = "legal_middle_names")
    String legalMiddleNames;

    @Column(name = "legal_last_name")
    String legalLastName;

    @PastOrPresent
    @Column(name = "dob")
    Date dob;

    @Column(name = "sex_code")
    String sexCode;

    @Column(name = "gender_code")
    String genderCode;

    @Column(name = "data_source_code")
    String dataSourceCode;

    @Column(name = "usual_first_name")
    String usualFirstName;

    @Column(name = "usual_middle_name")
    String usualMiddleName;

    @Column(name = "usual_last_name")
    String usualLastName;

    @Column(name = "email")
    String email;

    @PastOrPresent
    @Column(name = "deceased_date")
    Date deceasedDate;

    @Column(name = "create_user", updatable=false)
    @NotNull(message="createUser cannot be null")
    String createUser;

    @PastOrPresent
    @Column(name = "create_date", updatable=false)
    @NotNull(message="createDate cannot be null")
    Date createDate;

    @Column(name = "update_user")
    @NotNull(message="updateUser cannot be null")
    String updateUser;

    @PastOrPresent
    @Column(name = "update_date")
    @NotNull(message="updateDate cannot be null")
    Date updateDate;
}