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

@Service
public class SoamService {

    /*
    Access the existing REST APIs for Digital ID and Student objects
     */
    private RestTemplate restTemplate = new RestTemplate();

    public DigitalIDEntity getDigitalID(String id) {
        String url = "${digitalID.api}/" + id;
        return restTemplate.getForObject(url, DigitalIDEntity.class);
    }

    public DigitalIDEntity createDigitalID(DigitalIDEntity digitalID) {
        String url = "${digitalID.api}";
        return restTemplate.postForObject(url, digitalID, DigitalIDEntity.class);
    }

    public String updateDigitalID(DigitalIDEntity digitalID) {
        String url = "${digitalID.api}";
        restTemplate.put(url, digitalID);
        return "Update Success!";
    }

    public StudentEntity getStudent(Integer id) {
        String url = "${student.api}/" + id;
        return restTemplate.getForObject(url, StudentEntity.class);
    }
}