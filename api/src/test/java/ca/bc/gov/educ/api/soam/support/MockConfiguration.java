package ca.bc.gov.educ.api.soam.support;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Profile("test")
@Configuration
public class MockConfiguration {
  @Bean
  @Primary
  public WebClient webClient() {
    return Mockito.mock(WebClient.class);
  }
}
