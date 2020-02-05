package ca.bc.gov.educ.api.soam.model.entity;

import java.util.Date;
import java.util.UUID;

import lombok.Data;

/**
 * Digital Identity Entity
 *
 * @author John Cox
 */

@Data
public class DigitalIDEntity {
    UUID digitalID;
    UUID studentID;
    String identityTypeCode;
    String identityValue;
    Date lastAccessDate;
    String lastAccessChannelCode;
    String createUser;
    Date createDate;
    String updateUser;
    Date updateDate;
}
