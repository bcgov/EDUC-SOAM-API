package ca.bc.gov.educ.api.soam.codetable;

import ca.bc.gov.educ.api.soam.model.entity.AccessChannelCodeEntity;
import ca.bc.gov.educ.api.soam.model.entity.IdentityTypeCodeEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest

public class CodeTableUtilsTest {

  @Autowired
  ApplicationProperties props;

  @Autowired
  CodeTableUtils codeTableUtils;

  @Autowired
  WebClient webClient;

  @Mock
  private WebClient.RequestHeadersSpec requestHeadersMock;

  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriMock;

  @Mock
  private WebClient.ResponseSpec responseMock;

  @Before
  public void setUp() throws Exception{
    openMocks(this);
  }


  @Test
  public void testGetAllAccessChannelCodes_givenApiCallSuccess_shouldReturnMap() throws JsonProcessingException {
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    //when(restTemplate.exchange(eq(props.getDigitalIdentifierApiURL() + "/accessChannelCodes"), eq(HttpMethod.GET), any(), eq(AccessChannelCodeEntity[].class))).thenReturn(getAccessChannelMap());
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(this.props.getDigitalIdentifierApiURL()), any(Function.class))).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any())).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

    when(this.responseMock.bodyToFlux(AccessChannelCodeEntity.class))
            .thenReturn(Flux.just(this.getAccessChannelArray()));

    var results = codeTableUtils.getAllAccessChannelCodes();
    assertThat(results).size().isEqualTo(1);
    assertThat(results.get("OSPR")).isNotNull();
    assertThat(results.get("OSPR").getEffectiveDate()).isNotNull();
  }

  @Test
  public void getAllIdentifierTypeCodes_givenApiCallSuccess_shouldReturnMap() {
    //when(restUtils.getRestTemplate()).thenReturn(restTemplate);
    //when(restTemplate.exchange(eq(props.getDigitalIdentifierApiURL() + "/identityTypeCodes"), eq(HttpMethod.GET), any(), eq(IdentityTypeCodeEntity[].class))).thenReturn(getIdentityTypeCodeMap());
    //verify(restTemplate, atLeastOnce()).exchange(eq(props.getDigitalIdentifierApiURL() + "/identityTypeCodes"), eq(HttpMethod.GET), any(), eq(IdentityTypeCodeEntity[].class));
    //props.getDigitalIdentifierApiURL(),any(Function.class)
    when(webClient.get()).thenReturn(this.requestHeadersUriMock);
    when(this.requestHeadersUriMock.uri(eq(props.getDigitalIdentifierApiURL()),any(Function.class)))
            .thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.header(any(),any())).thenReturn(this.requestHeadersMock);
    when(this.requestHeadersMock.retrieve()).thenReturn(this.responseMock);

    when(this.responseMock.bodyToMono(IdentityTypeCodeEntity[].class))
            .thenReturn(Mono.just(this.getIdentityTypeCodeMap()));

    var results = codeTableUtils.getAllIdentifierTypeCodes();
    assertThat(results).size().isEqualTo(0);

  }

  AccessChannelCodeEntity[] getAccessChannelArray() {
    AccessChannelCodeEntity[] accessChannelCodeEntities = new AccessChannelCodeEntity[1];
    accessChannelCodeEntities[0]=AccessChannelCodeEntity
            .builder()
            .effectiveDate(LocalDateTime.now().toString())
            .expiryDate(LocalDateTime.MAX.toString())
            .accessChannelCode("OSPR")
            .build();
    return accessChannelCodeEntities;
  }

  IdentityTypeCodeEntity[] getIdentityTypeCodeMap() {
    IdentityTypeCodeEntity[] identityTypeCodeEntities = new IdentityTypeCodeEntity[0];
    return new ArrayList<IdentityTypeCodeEntity>().toArray(identityTypeCodeEntities);
  }
}
