package ca.bc.gov.educ.api.soam.codetable;

import ca.bc.gov.educ.api.soam.model.entity.AccessChannelCodeEntity;
import ca.bc.gov.educ.api.soam.model.entity.IdentityTypeCodeEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@Service
@Slf4j
public class CodeTableUtils {

  private final WebClient webClient;

  private final ApplicationProperties props;

  @Autowired
  public CodeTableUtils(final WebClient webClient, final ApplicationProperties props) {
    this.webClient=webClient;
    this.props = props;
  }

  @Cacheable("accessChannelCodes")
  public Map<String, AccessChannelCodeEntity> getAllAccessChannelCodes() {
    log.info("Fetching all access channel codes");
    //RestTemplate restTemplate = restUtils.getRestTemplate();
    //HttpHeaders headers = new HttpHeaders();
    //headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

   // response = restTemplate.exchange(
    //        props.getDigitalIdentifierApiURL() + "/accessChannelCodes", HttpMethod.GET,
     //       new HttpEntity<>("parameters", headers), AccessChannelCodeEntity[].class);
    return this.webClient.get()
            .uri(this.props.getDigitalIdentifierApiURL(),uri -> uri
                    .path("/accessChannelCodes").build())
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(AccessChannelCodeEntity.class)
            .collectList()
            .block().stream().collect(Collectors.toConcurrentMap(AccessChannelCodeEntity::getAccessChannelCode, Function.identity()));

  }

  @Cacheable("identityTypeCodes")
  public Map<String, IdentityTypeCodeEntity> getAllIdentifierTypeCodes() {
    log.info("Fetching all identity type codes");
    //RestTemplate restTemplate = restUtils.getRestTemplate();
    //HttpHeaders headers = new HttpHeaders();
    //headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

    //response = restTemplate.exchange(
    //        props.getDigitalIdentifierApiURL() + "/identityTypeCodes", HttpMethod.GET,
    //        new HttpEntity<>("parameters", headers), IdentityTypeCodeEntity[].class);
    return Objects.requireNonNull(this.webClient.get()
        .uri(this.props.getDigitalIdentifierApiURL(), uri -> uri
            .path("/identityTypeCodes")
            .build())
        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .retrieve()
        .bodyToFlux(IdentityTypeCodeEntity.class)
        .collectList()
        .block()).stream().collect(Collectors.toConcurrentMap(IdentityTypeCodeEntity::getIdentityTypeCode, Function.identity()));

  }

}
