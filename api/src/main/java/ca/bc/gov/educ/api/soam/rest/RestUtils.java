package ca.bc.gov.educ.api.soam.rest;

import java.util.List;

import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;

/**
 * This class is used for REST calls
 * 
 * @author Marco Villeneuve
 *
 */
@Component
public class RestUtils {

	private static Logger logger = Logger.getLogger(RestUtils.class);

	@Autowired
	private ApplicationProperties props;
	
	public RestTemplate getRestTemplate() {
		return getRestTemplate(null);
	}

	public RestTemplate getRestTemplate(List<String> scopes) {
		logger.debug("Calling get token method");
		ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
		resourceDetails.setClientId(props.getClientID());
		resourceDetails.setClientSecret(props.getClientSecret());
		resourceDetails.setAccessTokenUri(props.getTokenURL());
		if(scopes != null) {
			resourceDetails.setScope(scopes);
		}
		return new OAuth2RestTemplate(resourceDetails, new DefaultOAuth2ClientContext());
	}

}
