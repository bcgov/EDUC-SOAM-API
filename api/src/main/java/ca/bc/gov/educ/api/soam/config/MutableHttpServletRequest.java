package ca.bc.gov.educ.api.soam.config;

import lombok.val;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public final class MutableHttpServletRequest extends HttpServletRequestWrapper {
  // holds custom header and value mapping
  private final Map<String, String> customHeaders;

  public MutableHttpServletRequest(final HttpServletRequest request) {
    super(request);
    this.customHeaders = new HashMap<>();
  }

  public void putHeader(final String name, final String value) {
    this.customHeaders.put(name, value);
  }

  @Override
  public String getHeader(final String name) {
    // check the custom headers first
    val headerValue = this.customHeaders.get(name);

    if (StringUtils.isNotBlank(headerValue)) {
      return headerValue;
    }
    // else return from into the original wrapped object
    return ((HttpServletRequest) this.getRequest()).getHeader(name);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    // create a set of the custom header names
    final Set<String> set = new HashSet<>(this.customHeaders.keySet());

    // now add the headers from the wrapped request object
    final Enumeration<String> e = ((HttpServletRequest) this.getRequest()).getHeaderNames();
    while (e.hasMoreElements()) {
      // add the names of the request headers into the list
      final String n = e.nextElement();
      set.add(n);
    }
    // create an enumeration from the set and return
    return Collections.enumeration(set);
  }

  @Override
  public Enumeration<String> getHeaders(final String name) {
    final Set<String> headerValues = new HashSet<>();
    val customHeaderValue = this.customHeaders.get(name);
    if (StringUtils.isNotBlank(customHeaderValue)) {
      headerValues.add(customHeaderValue);
    } else {
      final Enumeration<String> underlyingHeaderValues = ((HttpServletRequest) this.getRequest()).getHeaders(name);
      while (underlyingHeaderValues.hasMoreElements()) {
        val element = underlyingHeaderValues.nextElement();
        if (StringUtils.isNotBlank(element)) {
          headerValues.add(element);
        }
      }
    }

    return Collections.enumeration(headerValues);
  }
}
