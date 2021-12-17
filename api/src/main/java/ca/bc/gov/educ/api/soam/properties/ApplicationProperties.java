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
  public static final String API_NAME="SOAM-API";
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
  @Value("${ramp.up.http.startup}")
  private Boolean isHttpRampUp;
  @Value("${sts.api.url}")
  private String stsApiURL;
  @Value("${url.api.pen.match}")
  private String penMatchApiURL;
}
