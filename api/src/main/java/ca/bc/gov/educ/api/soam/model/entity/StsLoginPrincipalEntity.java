package ca.bc.gov.educ.api.soam.model.entity;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * The type Isd login principal.
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@NoArgsConstructor
public class StsLoginPrincipalEntity implements Serializable {
  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = -3923367190666388407L;
  /**
   * The Principal id.
   */
  @NonNull
  private String principalID;
  /**
   * The Isd roles.
   */
  private List<StsRolesEntity> isdRoles;
}
