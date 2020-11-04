package ca.bc.gov.educ.api.soam.support;

import ca.bc.gov.educ.api.soam.rest.RestUtils;
import ca.bc.gov.educ.api.soam.util.SoamUtil;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Profile("test")
@Configuration
public class MockConfiguration {
  @Bean
  @Primary
  public RestTemplate restTemplate() {
    return Mockito.mock(RestTemplate.class);
  }

  @Bean
  @Primary
  public RestUtils restUtils() {
    return Mockito.mock(RestUtils.class);
  }

}
