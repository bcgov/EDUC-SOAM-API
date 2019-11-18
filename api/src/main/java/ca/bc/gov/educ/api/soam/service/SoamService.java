package ca.bc.gov.educ.api.soam.service;

import ca.bc.gov.educ.api.soam.model.StudentEntity;
import ca.bc.gov.educ.api.soam.model.DigitalIDEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.*;
import org.springframework.beans.factory.annotation.*;
import java.util.UUID;

@Service
public class SoamService {
/*
    @Value("${digitalID.api}")
    private String digitalIDUrl;

    @Value("${student.api}")
    private String studentUrl;
    */
    /*
    Access the existing REST APIs for Digital ID and Student objects
     */
    private RestTemplate restTemplate = new RestTemplate();

    public String getDigitalID(String idValue, String idType) {
        String url = "digitalIDUrl" + '/' + idType + '/' + idValue;
        return restTemplate.getForObject(url, String.class);
    }


    public String createDigitalID(DigitalIDEntity digitalID) {
        String url = "digitalID.ap";
        return restTemplate.postForObject(url, digitalID, String.class);
    }

    public String updateDigitalID(DigitalIDEntity digitalID) {
        String url = "digitalID.api";
        restTemplate.put(url, digitalID);
        return "Update Success!";
    }

    public String getStudent(Integer id) {
        String idString = String.valueOf(id);
        String url = "studentUrl" + idString;
        return restTemplate.getForObject(url, String.class);
    }

    public String getRandomPen() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0,9);
    }
}