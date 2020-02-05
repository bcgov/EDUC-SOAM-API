package ca.bc.gov.educ.api.soam.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Class holds all application properties
 *
 * @author Marco Villeneuve
 */
@Component
@Getter
@Setter
public class ApplicationProperties {

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
