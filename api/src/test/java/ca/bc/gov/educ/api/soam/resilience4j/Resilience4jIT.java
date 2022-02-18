package ca.bc.gov.educ.api.soam.resilience4j;

import ca.bc.gov.educ.api.soam.model.entity.AccessChannelCodeEntity;
import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.entity.IdentityTypeCodeEntity;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.vavr.collection.Stream;
import org.jose4j.lang.JoseException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.StatusAssertions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Function;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;

@TestPropertySource(locations="classpath:application-it.properties")
public class Resilience4jIT extends AbstractIntegrationTest {
	private final String guid = UUID.randomUUID().toString();
	@Autowired
	WebClient webClient;
	@Mock
	private WebClient.RequestHeadersSpec requestHeadersMock;
	@Mock
	private WebClient.RequestHeadersUriSpec requestHeadersUriMock;
	@Mock
	private WebClient.RequestBodySpec requestBodyMock;
	@Mock
	private WebClient.RequestBodyUriSpec requestBodyUriMock;
	@Mock
	private WebClient.ResponseSpec responseMock;

	private static String ISSUER_ID = "test";

	@Before
	public void before() {
		transitionToClosedState(DIGITAL_ID_API);
		MockitoAnnotations.openMocks(this);
		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class))).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.header(any(), any())).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToFlux(AccessChannelCodeEntity.class)).thenReturn(Flux.just(this.getAccessChannelArray()));

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
			.thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.header(any(), any())).thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);
		when(this.responseMock.bodyToFlux(IdentityTypeCodeEntity.class))
			.thenReturn(Flux.just(this.getIdentityTypeCodeArray()));
	}

	@Test
	public void performLogin_givenDigitalIdApiFailed_shouldOpenCircuitBreaker() throws JoseException {
		var valueMap = this.mockDigitalIdApiWithFailedResponse();

		var token = jwsBuilder.build().getCompactSerialization();
		Stream.rangeClosed(1,7).forEach((count) -> {
			webTestClient.post().uri("/login").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData(valueMap))
				.exchange().expectStatus()
				.is5xxServerError();
		});

		// Then
		checkHealthStatus(DIGITAL_ID_API, State.OPEN);
	}

	@Test
	public void performLogin_givenDigitalIdApiFailed_shouldRetryThreeTimes() throws JoseException {
		float currentCount = getCurrentCount(FAILED_WITH_RETRY, DIGITAL_ID_API);
		var valueMap = this.mockDigitalIdApiWithFailedResponse();
		var token = jwsBuilder.build().getCompactSerialization();
		webTestClient.post().uri("/login").header("Authorization", "Bearer " + token)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(BodyInserters.fromFormData(valueMap))
			.exchange().expectStatus()
			.is5xxServerError();

		checkMetrics(FAILED_WITH_RETRY, DIGITAL_ID_API, currentCount + 1);
	}

	@Test
	public void performLogin_givenDigitalIdApiSuccessful_shouldCloseCircuitBreaker() throws JoseException {
		transitionToOpenState(DIGITAL_ID_API);
		circuitBreakerRegistry.circuitBreaker(DIGITAL_ID_API).transitionToHalfOpenState();

		var valueMap = this.mockDigitalIdApiWithSuccessfulResponse();
		var token = jwsBuilder.build().getCompactSerialization();

		// When
		Stream.rangeClosed(1,2).forEach((count) -> {
			webTestClient.post().uri("/login").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(BodyInserters.fromFormData(valueMap))
				.exchange().expectStatus()
				.isNoContent();
		});

		// Then
		checkHealthStatus(DIGITAL_ID_API, State.CLOSED);
	}

	@Test
	public void performLogin_givenMultipleRequests_shouldLimitConcurrentExecution() throws JoseException {
		Stream.rangeClosed(1,2).forEach((count) -> {
			this.bulkheadRegistry.bulkhead("digitalIdApi").acquirePermission();
		});
		var valueMap = this.mockDigitalIdApiWithSuccessfulResponse();
		var token = jwsBuilder.build().getCompactSerialization();

		var pool = Executors.newFixedThreadPool(5);
		var futures = new ArrayList<CompletableFuture<StatusAssertions>>();
		for(int i=0; i < 2; i++) {
			futures.add(CompletableFuture.supplyAsync(
				() -> webTestClient.post().uri("/login").header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(BodyInserters.fromFormData(valueMap))
					.exchange().expectStatus(), pool));
		}

		var statuses = new ArrayList<Integer>();
		futures.forEach(future -> {
			try {
				future.get().value(statuses::add);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		});

		assertThat(statuses).contains(BAD_GATEWAY.value());
		Stream.rangeClosed(1,2).forEach((count) -> {
			this.bulkheadRegistry.bulkhead("digitalIdApi").releasePermission();
		});
	}

	@Test
	public void performLogin_givenMultipleRequests_shouldDeclineRequests() throws JoseException {
		var valueMap = this.mockDigitalIdApiWithSuccessfulResponse();
		var token = jwsBuilder.build().getCompactSerialization();

		var pool = Executors.newFixedThreadPool(5);
		var futures = new ArrayList<CompletableFuture<StatusAssertions>>();
		for(int i=0; i < 5; i++) {
			futures.add(CompletableFuture.supplyAsync(
				() -> webTestClient.post().uri("/login").header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(BodyInserters.fromFormData(valueMap))
					.exchange().expectStatus(), pool));
		}

		var statuses = new ArrayList<Integer>();
		futures.forEach(future -> {
			try {
				future.get().value(statuses::add);
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		});

		assertThat(statuses).contains(TOO_MANY_REQUESTS.value());

	}

	private MultiValueMap<String, String> mockDigitalIdApiWithFailedResponse() {
		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("identifierType", "BASIC");
		map.add("identifierValue", this.guid);

		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
			.thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.header(any(), any()))
			.thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve())
			.thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(DigitalIDEntity.class))
			.thenThrow(new WebClientResponseException(500, "test service", HttpHeaders.EMPTY, new byte[0], null, null));

		return map;
	}

	private MultiValueMap<String, String> mockDigitalIdApiWithSuccessfulResponse() {
		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("identifierType", "BASIC");
		map.add("identifierValue", this.guid);
		final var invocations = mockingDetails(this.webClient).getInvocations().size();
		final DigitalIDEntity entity = this.getDigitalIdentity();
		when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
		when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
			.thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.header(any(), any()))
			.thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve())
			.thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(DigitalIDEntity.class))
			.thenReturn(Mono.just(entity));

		when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
			.thenReturn(this.requestBodyUriMock);
		when(this.requestBodyUriMock.header(any(), any()))
			.thenReturn(this.returnMockBodySpec());
		when(this.requestBodyUriMock.headers(any()))
			.thenReturn(this.returnMockBodySpec());
		when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
			.thenReturn(this.requestHeadersMock);
		when(this.requestHeadersMock.retrieve())
			.thenReturn(this.responseMock);
		when(this.responseMock.bodyToMono(DigitalIDEntity.class)).thenReturn(Mono.just(entity));

		return map;
	}

	private WebClient.RequestBodySpec returnMockBodySpec() {
		return this.requestBodyMock;
	}

	AccessChannelCodeEntity[] getAccessChannelArray() {
		final AccessChannelCodeEntity[] accessChannelCodeEntities = new AccessChannelCodeEntity[1];
		accessChannelCodeEntities[0] = AccessChannelCodeEntity
			.builder()
			.effectiveDate(LocalDateTime.now().toString())
			.expiryDate(LocalDateTime.MAX.toString())
			.accessChannelCode("OSPR")
			.build();
		return accessChannelCodeEntities;
	}

	IdentityTypeCodeEntity[] getIdentityTypeCodeArray() {
		final IdentityTypeCodeEntity[] identityTypeCodeEntities = new IdentityTypeCodeEntity[1];
		identityTypeCodeEntities[0] = IdentityTypeCodeEntity.builder()
			.identityTypeCode("BASIC")
			.displayOrder(1)
			.build();
		return identityTypeCodeEntities;
	}

	private DigitalIDEntity getDigitalIdentity() {
		final DigitalIDEntity entity = DigitalIDEntity.builder()
			.identityTypeCode("BASIC")
      .autoMatched("N")
			.identityValue(this.guid)
			.lastAccessChannelCode("OSPR")
			.lastAccessDate(LocalDateTime.now().toString())
			.build();

		return entity;
	}
}
