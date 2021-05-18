package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.model.entity.AccessChannelCodeEntity;
import ca.bc.gov.educ.api.soam.model.entity.DigitalIDEntity;
import ca.bc.gov.educ.api.soam.model.entity.IdentityTypeCodeEntity;
import ca.bc.gov.educ.api.soam.model.entity.ServicesCardEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class SoamControllerTest {

  private final String guid = UUID.randomUUID().toString();
  @Autowired
  WebClient webClient;
  @Autowired
  ApplicationProperties props;
  /**
   * The Mock mvc.
   */
  @Autowired
  private MockMvc mockMvc;
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

  @Before
  public void before() {
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
  public void performLogin_givenValidPayload_shouldReturnNoContent() throws Exception {

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
    when(this.requestBodyUriMock.uri(this.props.getDigitalIdentifierApiURL()))
        .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
        .thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
        .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
        .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(DigitalIDEntity.class)).thenReturn(Mono.just(entity));


    this.mockMvc.perform(multipart("/login")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "SOAM_LOGIN")))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .params(map)
        .accept(MediaType.APPLICATION_FORM_URLENCODED)).andDo(print()).andExpect(status().isNoContent());

    verify(this.webClient, atMost(invocations + 1)).put();
  }

  @Test
  public void performLogin_givenValidPayloadWithServicesCard_shouldReturnNoContent() throws Exception {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();

    final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("identifierType", "BASIC");
    map.add("identifierValue", this.guid.toUpperCase());
    map.add("did", this.guid.toUpperCase());
    map.add("birthDate", "1984-11-02");
    map.add("city", "Victoria");
    map.add("country", "CAN");
    map.add("email", "abc@gmail.com");
    map.add("gender", "M");
    map.add("identityAssuranceLevel", "1");
    map.add("givenName", "Given");

    final ServicesCardEntity servicesCardEntity = this.createServiceCardEntity();
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getServicesCardApiURL()), any(Function.class)))
        .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
        .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
        .thenReturn(this.requestHeadersMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
        .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
        .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
        .thenReturn(Mono.just(servicesCardEntity));
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
        .thenReturn(Mono.just(this.getDigitalIdentity()));

    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(this.props.getServicesCardApiURL()), any(Function.class)))
        .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getDigitalIdentifierApiURL()))
        .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
        .thenReturn(this.returnMockBodySpec());
    when(this.requestHeadersMock.retrieve())
        .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
        .thenReturn(Mono.just(servicesCardEntity));
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
        .thenReturn(Mono.just(this.getDigitalIdentity()));


    this.mockMvc.perform(multipart("/login")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "SOAM_LOGIN")))
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .params(map)
        .accept(MediaType.APPLICATION_FORM_URLENCODED)).andDo(print()).andExpect(status().isNoContent());
    verify(this.webClient, atMost(invocations + 2)).put();

  }

  @Test
  public void getSoamLoginEntity_givenValidPayloadWithServicesCard_shouldReturnOk() throws Exception {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    final ServicesCardEntity servicesCardEntity = this.createServiceCardEntity();
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getServicesCardApiURL()), any(Function.class)))
        .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
        .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
        .thenReturn(this.requestHeadersMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
        .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
        .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
        .thenReturn(Mono.just(servicesCardEntity));
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
        .thenReturn(Mono.just(this.getDigitalIdentity()));

    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getServicesCardApiURL()))
        .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getDigitalIdentifierApiURL()))
        .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any()))
        .thenReturn(this.returnMockBodySpec());
    when(this.requestHeadersMock.retrieve())
        .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
        .thenReturn(Mono.just(servicesCardEntity));
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
        .thenReturn(Mono.just(this.getDigitalIdentity()));


    this.mockMvc.perform(get("/BASIC/" + this.guid)
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "SOAM_LOGIN")))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
        .andDo(print()).andExpect(status().isOk());
    verify(this.webClient, atMost(invocations + 2)).put();
  }

  private DigitalIDEntity getDigitalIdentity() {
    final DigitalIDEntity entity = DigitalIDEntity.builder()
        .identityTypeCode("BASIC")
        .identityValue(this.guid)
        .lastAccessChannelCode("OSPR")
        .lastAccessDate(LocalDateTime.now().toString())
        .build();

    return entity;
  }

  IdentityTypeCodeEntity[] getIdentityTypeCodeMap() {
    final IdentityTypeCodeEntity[] identityTypeCodeEntities = new IdentityTypeCodeEntity[1];
    final var identityTypes = new ArrayList<IdentityTypeCodeEntity>();
    identityTypes.add(IdentityTypeCodeEntity
        .builder()
        .effectiveDate(LocalDateTime.now().toString())
        .expiryDate(LocalDateTime.MAX.toString())
        .identityTypeCode("BASIC")
        .build());
    return identityTypes.toArray(identityTypeCodeEntities);
  }

  private ServicesCardEntity createServiceCardEntity() {
    final ServicesCardEntity serviceCard = new ServicesCardEntity();
    serviceCard.setBirthDate("1984-11-02");
    serviceCard.setCity("Victoria");
    serviceCard.setCountry("CAN");
    serviceCard.setDid("DIGITALID");
    serviceCard.setEmail("abc@gmail.com");
    serviceCard.setGender("M");
    serviceCard.setIdentityAssuranceLevel("1");
    serviceCard.setGivenName("Given");
    serviceCard.setGivenNames(null);
    serviceCard.setPostalCode("V8W 2E1");
    serviceCard.setProvince("BC");
    serviceCard.setStreetAddress("Courtney Street");
    serviceCard.setSurname("Surname");
    serviceCard.setUserDisplayName("displayName");
    return serviceCard;
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
}
