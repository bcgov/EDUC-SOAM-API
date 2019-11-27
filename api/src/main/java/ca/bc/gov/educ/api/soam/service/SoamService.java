package ca.bc.gov.educ.api.soam.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.api.soam.model.SoamLoginEntity;

@Service
public class SoamService {

    private RestTemplate restTemplate = new RestTemplate();

    public SoamLoginEntity performLogin(String idValue, String idType) {
    	
        return restTemplate.getForObject(url, String.class);
    }


}