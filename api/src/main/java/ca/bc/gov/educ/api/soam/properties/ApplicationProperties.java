package ca.bc.gov.educ.api.soam.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class holds all application properties
 *
 * @author Marco Villeneuve
 */
@Component
@Getter
public class ApplicationProperties {

  public static final String BCSC = "BCSC";
  @Value("${client.id}")
	private String clientID;
	@Value("${client.secret}")
	private String clientSecret;
	@Value("${token.url}")
	private String tokenURL;
	@Value("${digitalid.api.url}")
	private String digitalIdentifierApiURL;
	@Value("${student.api.url}")
	private String studentApiURL;
	@Value("${servicescard.api.url}")
	private String servicesCardApiURL;
}
