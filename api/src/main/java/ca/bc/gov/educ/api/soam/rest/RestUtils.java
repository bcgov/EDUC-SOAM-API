package ca.bc.gov.educ.api.soam.rest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.message.types.GrantType;

import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;

/**
 * This class is used for REST calls
 * 
 * @author Marco Villeneuve
 *
 */
public class RestUtils {
	
	private static RestUtils restUtilsInstance;
	
	private static ApplicationProperties props;

	private RestUtils() {
		props = new ApplicationProperties();
	}
	
	public static RestUtils getInstance() {
		if(restUtilsInstance == null) {
			restUtilsInstance = new RestUtils();
		}
		return restUtilsInstance;
	}

	private String getToken(String scope) {
		try {
			OAuthClient client = new OAuthClient(new URLConnectionClient());

			OAuthClientRequest request = OAuthClientRequest.tokenLocation(props.getTokenURL())
					.setGrantType(GrantType.CLIENT_CREDENTIALS).setClientId(props.getClientID())
					.setScope(scope).setClientSecret(props.getClientSecret()).buildBodyMessage();

			return client.accessToken(request, OAuth.HttpMethod.POST, OAuthJSONAccessTokenResponse.class)
					.getAccessToken();
		} catch (Exception exn) {
			throw new RuntimeException("Could not get token: " + exn);
		}
	}

	/**
	 * Modify/add new methods like this one for your REST call 
	 * 
	 * @return
	 */
	public String getPEN() {
		try {
			// Sending get request
			URL url = new URL(props.getSoamURL());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setRequestProperty("Authorization", "Bearer " + getToken("READ_PEN_REQUEST"));
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestMethod("GET");

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String output;

			StringBuffer response = new StringBuffer();
			while ((output = in.readLine()) != null) {
				response.append(output);
			}

			in.close();
			return response.toString();
		} catch (Exception e) {
			throw new RuntimeException("Could not call SOAM API: " + e);
		}

	} 

}
