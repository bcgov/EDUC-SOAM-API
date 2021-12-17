package ca.bc.gov.educ.api.soam.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The type Json util.
 */
public class JsonUtil {
  /**
   * Instantiates a new Json util.
   */
  private JsonUtil() {
  }
  public static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Gets json pretty string from object.
   *
   * @param payload the payload
   * @return the json pretty string from object
   */
  public static String getJsonPrettyStringFromObject(Object payload) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      return "";
    }
  }

}
