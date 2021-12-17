package ca.bc.gov.educ.api.soam.struct.v1.penmatch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Pen match record.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PenMatchRecord {
  /**
   * The Matching pen.
   */
  private String matchingPEN;
  /**
   * The Student id.
   */
  private String studentID;
}
