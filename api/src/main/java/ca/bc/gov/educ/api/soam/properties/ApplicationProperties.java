package ca.bc.gov.educ.api.soam.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class holds all application properties
 * 
 * @author Marco Villeneuve
 *
 */
@Component
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
	@Value("${codetable.api.url}")
	private String codetableApiURL;

	public String getClientID() {
		return clientID;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public String getTokenURL() {
		return tokenURL;
	}

	public String getDigitalIdentifierApiURL() {
		return digitalIdentifierApiURL;
	}

	public String getStudentApiURL() {
		return studentApiURL;
	}

	public String getCodetableApiURL() {
		return codetableApiURL;
	}

}