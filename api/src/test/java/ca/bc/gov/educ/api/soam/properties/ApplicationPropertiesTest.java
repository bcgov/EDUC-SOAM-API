package ca.bc.gov.educ.api.soam.properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
public class ApplicationPropertiesTest {
  @Autowired
  ApplicationProperties properties;

  @Test
  public void testPropValuesToBeNotNull() {
    assertThat(this.properties).isNotNull();
    assertThat(this.properties.getDigitalIdentifierApiURL()).isNotNull();
    assertThat(this.properties.getClientID()).isNotNull();
    assertThat(this.properties.getClientSecret()).isNotNull();
    assertThat(this.properties.getServicesCardApiURL()).isNotNull();
    assertThat(this.properties.getTokenURL()).isNotNull();
    assertThat(this.properties.getStudentApiURL()).isNotNull();
  }
}
