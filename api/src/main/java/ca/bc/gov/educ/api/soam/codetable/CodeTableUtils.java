package ca.bc.gov.educ.api.soam.codetable;

import java.util.Collections;
import java.util.HashMap;

import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.api.codetable.model.AccessChannelCodeEntity;
import ca.bc.gov.educ.api.codetable.model.IdentityTypeCodeEntity;
import ca.bc.gov.educ.api.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.api.soam.rest.RestUtils;

@Service
public class CodeTableUtils {
	
	private static Logger logger = Logger.getLogger(CodeTableUtils.class);

	@Autowired
	private RestUtils restUtils;
	
	@Autowired
	private ApplicationProperties props;

	@Cacheable("accessChannelCodes")
	public HashMap<String, AccessChannelCodeEntity> getAllAccessChannelCodes() {
		logger.info("Fetching all access channel codes");
		RestTemplate restTemplate = restUtils.getRestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		ResponseEntity<AccessChannelCodeEntity[]> response;
		response = restTemplate.exchange(
				props.getCodetableApiURL() + "/accessChannel", HttpMethod.GET,
				new HttpEntity<>("parameters", headers), AccessChannelCodeEntity[].class);
		
		HashMap<String, AccessChannelCodeEntity> map = new HashMap<String, AccessChannelCodeEntity>();
		if(response != null && response.getBody() != null) {
			for(AccessChannelCodeEntity entity: response.getBody()) {
				map.put(entity.getAccessChannelCode(), entity);
			}
		}
		
		return map;
	}
	
	@Cacheable("identityTypeCodes")
	public HashMap<String, IdentityTypeCodeEntity> getAllIdentifierTypeCodes() {
		logger.info("Fetching all access channel codes");
		RestTemplate restTemplate = restUtils.getRestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

		ResponseEntity<IdentityTypeCodeEntity[]> response;
		response = restTemplate.exchange(
				props.getCodetableApiURL() + "/identityType", HttpMethod.GET,
				new HttpEntity<>("parameters", headers), IdentityTypeCodeEntity[].class);
		
		HashMap<String, IdentityTypeCodeEntity> map = new HashMap<String, IdentityTypeCodeEntity>();
		if(response != null && response.getBody() != null) {
			for(IdentityTypeCodeEntity entity: response.getBody()) {
				map.put(entity.getIdentityTypeCode(), entity);
			}
		}
		
		return map;
	}
	
}
