package ca.bc.gov.educ.api.soam.struct.v1.student;

import ca.bc.gov.educ.api.soam.model.entity.StudentEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentSearchWrapper implements Serializable {
  private List<StudentEntity> content;
}
