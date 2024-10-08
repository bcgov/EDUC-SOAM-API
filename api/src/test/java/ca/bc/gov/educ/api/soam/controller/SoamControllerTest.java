package ca.bc.gov.educ.api.soam.controller;

import ca.bc.gov.educ.api.soam.model.entity.*;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.struct.v1.student.StudentSearchWrapper;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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


    this.mockMvc.perform(multipart("/login")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "SOAM_LOGIN")))
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .header("correlationID", this.guid)
      .params(map)
      .accept(MediaType.APPLICATION_FORM_URLENCODED)).andDo(print()).andExpect(status().isNoContent());

    verify(this.webClient, atMost(invocations + 1)).put();
  }

  @Test
  public void performLink_givenValidPayloadWithServicesCard_shouldReturnSoamLoginEntity() throws Exception { //THIS ONE?
    final var invocations = mockingDetails(this.webClient).getInvocations().size();

    final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("did", this.guid.toUpperCase());
    map.add("birthDate", "19841102");
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
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL() + "/list"), any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getStudentApiURL()), any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersUriMock.uri(any(URI.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.headers(any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(ServicesCardEntity.class))
      .thenReturn(Mono.just(servicesCardEntity));
    when(this.responseMock.bodyToMono(DigitalIDEntity.class))
      .thenReturn(Mono.just(this.getDigitalIdentity()));
    when(this.responseMock.bodyToMono(List.class))
            .thenReturn(Mono.just(createStudentSearchResult()));
    when(this.responseMock.bodyToMono(StudentSearchWrapper.class))
            .thenReturn(Mono.just(createWrappedStudentResult()));
    when(this.responseMock.bodyToMono(StudentEntity.class))
            .thenReturn(Mono.just(createStudentResult()));
    when(this.responseMock.bodyToMono(any(ParameterizedTypeReference.class)))
            .thenReturn(Mono.just(Arrays.asList(getDigitalIdentity())));
    when(this.requestBodyUriMock.uri(this.props.getPenMatchApiURL())).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getStudentApiURL())).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getDigitalIdentifierApiURL())).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getDigitalIdentifierApiURL() + "/list")).thenReturn(this.requestBodyUriMock);

    when(this.webClient.post()).thenReturn(this.requestBodyUriMock);
    when(this.webClient.put()).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(this.props.getServicesCardApiURL()), any(Function.class)))
      .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
      .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(this.props.getDigitalIdentifierApiURL() + "/list"), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(eq(this.props.getStudentApiURL()), any(Function.class)))
            .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.headers(any()))
      .thenReturn(this.returnMockBodySpec());
    when(this.requestHeadersMock.retrieve())
      .thenReturn(this.responseMock);
    when(this.requestBodyUriMock.uri(this.props.getPenMatchApiURL())).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getStudentApiURL())).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getDigitalIdentifierApiURL())).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.uri(this.props.getDigitalIdentifierApiURL() + "/list")).thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.header(any(), any())).thenReturn(this.returnMockBodySpec());
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class))).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

    this.mockMvc.perform(multipart("/link")
      .with(jwt().jwt((jwt) -> jwt.claim("scope", "SOAM_LINK")))
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .header("correlationID", this.guid)
      .params(map)
      .accept(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());
    verify(this.webClient, atMost(invocations + 4)).put();

  }

  @Test
  public void performLogin_givenValidPayloadWithServicesCard_shouldReturnNoContent() throws Exception {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();

    final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("identifierType", "BASIC");
    map.add("identifierValue", this.guid.toUpperCase());
    map.add("did", this.guid.toUpperCase());
    map.add("birthDate", "19841102");
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
    when(this.requestHeadersMock.headers(any()))
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
    when(this.requestBodyUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
      .thenReturn(this.requestBodyUriMock);
    when(this.requestBodyUriMock.headers(any()))
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
      .header("correlationID", this.guid)
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
        .header("correlationID", this.guid)
        .accept(MediaType.APPLICATION_JSON))
      .andDo(print()).andExpect(status().isOk());
    verify(this.webClient, atMost(invocations + 2)).put();
  }

  @Test
  public void getStsRolesBySsoGuid_givenSsoGuid_shouldReturnOk() throws Exception {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    val entity = this.createStsLoginPrincipalEntity();
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getStsApiURL() + "/"), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(StsLoginPrincipalEntity.class))
      .thenReturn(Mono.just(entity));

    this.mockMvc.perform(get("/123/sts-user-roles")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "STS_ROLES")))
        .contentType(MediaType.APPLICATION_JSON)
        .header("correlationID", this.guid)
        .accept(MediaType.APPLICATION_JSON))
      .andDo(print()).andExpect(status().isOk());
    verify(this.webClient, atMost(invocations + 1)).get();
  }

  @Test
  public void getStsRolesBySsoGuid_givenSsoGuid_withWrongScope_shouldReturnForbidden() throws Exception {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    val entity = this.createStsLoginPrincipalEntity();
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getStsApiURL() + "/"), any(Function.class)))
        .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
        .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
        .thenReturn(this.requestHeadersMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
        .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
        .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(StsLoginPrincipalEntity.class))
        .thenReturn(Mono.just(entity));

    this.mockMvc.perform(get("/123/sts-user-roles")
            .with(jwt().jwt((jwt) -> jwt.claim("scope", "WRONG_SCOPE")))
            .contentType(MediaType.APPLICATION_JSON)
            .header("correlationID", this.guid)
            .accept(MediaType.APPLICATION_JSON))
        .andDo(print()).andExpect(status().isForbidden());
    verify(this.webClient, atMost(invocations + 1)).get();
  }

  @Test
  public void getStsRolesBySsoGuid_givenSsoGuidNotPresent_shouldReturnOk() throws Exception {
    final var invocations = mockingDetails(this.webClient).getInvocations().size();
    when(this.webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getStsApiURL() + "/"), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(), any()))
      .thenReturn(this.requestHeadersMock);
    when(this.requestBodyMock.body(any(), (Class<?>) any(Object.class)))
      .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve())
      .thenReturn(this.responseMock);
    when(this.responseMock.bodyToMono(StsLoginPrincipalEntity.class))
      .thenReturn(Mono.empty());

    this.mockMvc.perform(get("/12355555/sts-user-roles")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "STS_ROLES")))
        .contentType(MediaType.APPLICATION_JSON)
        .header("correlationID", this.guid)
        .accept(MediaType.APPLICATION_JSON))
      .andDo(print()).andExpect(status().isOk());
    verify(this.webClient, atMost(invocations + 1)).get();
  }

  private StsLoginPrincipalEntity createStsLoginPrincipalEntity() {
    val isdRoles = new ArrayList<StsRolesEntity>();
    isdRoles.add(new StsRolesEntity("123", "ROLE_1", "ROLE_1"));
    val stsLoginPrincipalEntity = new StsLoginPrincipalEntity();
    stsLoginPrincipalEntity.setPrincipalID("123");
    stsLoginPrincipalEntity.setIsdRoles(isdRoles);
    return stsLoginPrincipalEntity;
  }

  @Test
  public void getSoamLoginEntity_givenValidPayloadWithServicesCardViaDid_shouldReturnOk() throws Exception {
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

    this.mockMvc.perform(get("/userInfo")
        .with(jwt().jwt((jwt) -> jwt.claim("scope", "SOAM_USER_INFO")
          .claim("digitalIdentityID", this.getDigitalIdentity().getDigitalID().toString())))
        .contentType(MediaType.APPLICATION_JSON)
        .header("correlationID", this.guid)
        .accept(MediaType.APPLICATION_JSON))
      .andDo(print()).andExpect(status().isOk());
    verify(this.webClient, atMost(invocations + 2)).put();
  }

  private DigitalIDEntity getDigitalIdentity() {
    final DigitalIDEntity entity = DigitalIDEntity.builder()
      .digitalID(UUID.randomUUID())
      .identityTypeCode("BASIC")
      .autoMatchedDate(null)
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
    identityTypes.add(IdentityTypeCodeEntity
      .builder()
      .effectiveDate(LocalDateTime.now().toString())
      .expiryDate(LocalDateTime.MAX.toString())
      .identityTypeCode("BCSC")
      .build());
    return identityTypes.toArray(identityTypeCodeEntities);
  }

  private ServicesCardEntity createServiceCardEntity() {
    final ServicesCardEntity serviceCard = new ServicesCardEntity();
    serviceCard.setBirthDate("1984-11-02");
    serviceCard.setDid("DIGITALID");
    serviceCard.setEmail("abc@gmail.com");
    serviceCard.setGender("M");
    serviceCard.setIdentityAssuranceLevel("1");
    serviceCard.setGivenName("Given");
    serviceCard.setGivenNames(null);
    serviceCard.setPostalCode("V8W 2E1");
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
    final IdentityTypeCodeEntity[] identityTypeCodeEntities = new IdentityTypeCodeEntity[2];
    identityTypeCodeEntities[0] = IdentityTypeCodeEntity.builder()
      .identityTypeCode("BASIC")
      .displayOrder(1)
      .build();
    identityTypeCodeEntities[1] = IdentityTypeCodeEntity.builder()
      .identityTypeCode("BCSC")
      .displayOrder(2)
      .build();
    return identityTypeCodeEntities;
  }

  private List<StudentEntity> createStudentSearchResult() {
    List<StudentEntity> students = new ArrayList<>();
    StudentEntity stud = new StudentEntity();
    stud.setStudentID(UUID.randomUUID());
    students.add(stud);
    return students;
  }

  private StudentSearchWrapper createWrappedStudentResult() {
    StudentSearchWrapper wrap = new StudentSearchWrapper();
    StudentEntity stud = new StudentEntity();
    stud.setStudentID(UUID.randomUUID());
    wrap.setContent(Arrays.asList(stud));
    return wrap;
  }

  private StudentEntity createStudentResult() {
    StudentEntity stud = new StudentEntity();
    stud.setStudentID(UUID.randomUUID());
    return stud;
  }
}
