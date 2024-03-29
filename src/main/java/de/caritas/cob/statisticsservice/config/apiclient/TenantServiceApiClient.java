package de.caritas.cob.statisticsservice.config.apiclient;

import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;

/** Extension of the generated UserService API client to adapt the handling of parameter values. */
public class TenantServiceApiClient
    extends de.caritas.cob.statisticsservice.tenantservice.generated.ApiClient {

  public TenantServiceApiClient(RestTemplate restTemplate) {
    super(restTemplate);
  }

  /**
   * Changes the behavior of mapping multiple parameter values to exclude null values for objects
   * which are not {@link Collection} for filter query params.
   *
   * @param collectionFormat The format to convert to
   * @param name The name of the parameter
   * @param value The parameter's value
   * @return a Map containing non-null String value(s) of the input parameter
   */
  @Override
  public MultiValueMap<String, String> parameterToMultiValueMap(
      CollectionFormat collectionFormat, String name, Object value) {

    if (TenantServiceHelper.noValidFilterParams(name, value)) {
      return super.parameterToMultiValueMap(collectionFormat, name, value);
    }

    return TenantServiceHelper.obtainQueryParameters(value);
  }
}
