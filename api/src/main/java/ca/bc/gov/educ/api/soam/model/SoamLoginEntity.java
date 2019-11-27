package ca.bc.gov.educ.api.soam.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;

public class SoamLoginEntity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "digital_identity_id")
    String digitalID;

    @Column(name = "student_id")
    Integer studentID;

    @NotNull(message="identityTypeCode cannot be null")
    @Column(name = "identity_type_code")
    String identityTypeCode;

    @NotNull(message="identityValue cannot be null")
    @Column(name = "identity_value")
    String identityValue;

    @PastOrPresent
    @NotNull(message="lastAccessTime cannot be null")
    @Column(name = "last_access_time")
    Date lastAccessDate;

    @NotNull(message="lastAccessChannelCode cannot be null")
    @Column(name = "last_access_channel_code")
    String lastAccessChannelCode;

    @NotNull(message= "createUser cannot be null")
    @Column(name = "create_user", updatable = false)
    String createUser;

    @NotNull(message="createDate cannot be null")
    @PastOrPresent
    @Column(name = "create_date", updatable = false)
    Date createDate;

    @Column(name = "update_user", updatable = false)
    String updateUser;

    @PastOrPresent
    @Column(name = "update_date", updatable = false)
    Date updateDate;
}