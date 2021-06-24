package ca.bc.gov.educ.api.soam.resilience4j;

import ca.bc.gov.educ.api.soam.SoamApiResourceApplication;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.support.JWSBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.lang.JoseException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.jose4j.jwk.JsonWebKeySet;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = SoamApiResourceApplication.class)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    protected static final String DIGITAL_ID_API = "digitalIdApi";
    protected static final String STUDENT_API = "studentApi";
    protected static final String FAILED_WITH_RETRY = "failed_with_retry";
    protected static final String SUCCESS_WITHOUT_RETRY = "successful_without_retry";

    protected static RsaJsonWebKey rsaJsonWebKey;
    protected static String subject;
    protected static JWSBuilder jwsBuilder;

    @Autowired
    protected CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    protected RateLimiterRegistry rateLimiterRegistry;

    @Autowired
    protected BulkheadRegistry bulkheadRegistry;

    @Autowired
    protected RetryRegistry retryRegistry;

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ApplicationProperties props;

    @Value("${wiremock.server.baseUrl}")
    private String wireMockServerBaseUrl;

    @BeforeClass
    public static void setup() throws JoseException {
        rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        subject = UUID.randomUUID().toString();
        jwsBuilder = new JWSBuilder().subject(subject);
        rsaJsonWebKey.setKeyId("k1");
        rsaJsonWebKey.setAlgorithm(AlgorithmIdentifiers.RSA_USING_SHA256);
        rsaJsonWebKey.setUse("sig");
        jwsBuilder.rsaJsonWebKey(rsaJsonWebKey);
    }

    @Before
    public void init() {
        transitionToClosedState(DIGITAL_ID_API);
        transitionToClosedState(STUDENT_API);

        jwsBuilder.issuer(this.wireMockServerBaseUrl);

        WireMock.stubFor(
          WireMock.get(WireMock.urlEqualTo("/.well-known/jwks.json"))
            .willReturn(
              WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(new JsonWebKeySet(rsaJsonWebKey).toJson())
            )
        );
    }

    @After
    public void shutdown() throws IOException {
    }

    protected void transitionToOpenState(String circuitBreakerName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        circuitBreaker.transitionToOpenState();
    }

    protected void transitionToClosedState(String circuitBreakerName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        circuitBreaker.transitionToClosedState();
    }

    protected void checkHealthStatus(String circuitBreakerName, CircuitBreaker.State state) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        assertThat(circuitBreaker.getState()).isEqualTo(state);
    }

    protected float getCurrentCount(String kind, String backend) {
        Retry.Metrics metrics = retryRegistry.retry(backend).getMetrics();

        if (FAILED_WITH_RETRY.equals(kind)) {
            return metrics.getNumberOfFailedCallsWithRetryAttempt();
        }
        if (SUCCESS_WITHOUT_RETRY.equals(kind)) {
            return metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt();
        }

        return 0;
    }

    protected void checkMetrics(String kind, String backend, float count) {
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity("/actuator/prometheus", String.class);
        assertThat(metricsResponse.getBody()).isNotNull();
        String response = metricsResponse.getBody();
        assertThat(response).contains(getMetricName(kind, backend) + count);
    }

    protected static String getMetricName(String kind, String backend) {
        return "resilience4j_retry_calls_total{kind=\"" + kind + "\",name=\"" + backend + "\",} ";
    }

}
