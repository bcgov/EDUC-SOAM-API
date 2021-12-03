package ca.bc.gov.educ.api.soam.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StsRolesEntity implements Serializable {
  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = -6418813121220247401L;
  /**
   * The Principal id.
   */
  String principalId;
  /**
   * The Role.
   */
  String role;
  /**
   * The Role group.
   */
  String roleGroup;
}
