package de.caritas.cob.statisticsservice.api.service;

import de.caritas.cob.statisticsservice.api.service.securityheader.TenantHeaderSupplier;
import de.caritas.cob.statisticsservice.config.cache.SessionCacheManagerConfig;
import de.caritas.cob.statisticsservice.userstatisticsservice.generated.web.UserStatisticsControllerApi;
import de.caritas.cob.statisticsservice.api.service.securityheader.SecurityHeaderSupplier;
import de.caritas.cob.statisticsservice.userstatisticsservice.generated.web.model.SessionStatisticsResultDTO;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserStatisticsService {

  private final @NonNull UserStatisticsControllerApi userStatisticsControllerApi;
  private final @NonNull SecurityHeaderSupplier securityHeaderSupplier;
  private final @NonNull TenantHeaderSupplier tenantHeaderSupplier;

  /**
   * Retrieve session via id.
   *
   * @param sessionId the session id
   * @return an {@link SessionStatisticsResultDTO} instance
   */
  @Cacheable(value = SessionCacheManagerConfig.SESSION_CACHE, key = "#sessionId")
  public SessionStatisticsResultDTO retrieveSessionViaSessionId(Long sessionId) {
    return retrieveSession(sessionId, null);
  }

  /**
   * Retrieve session via Rocket.Chat group id.
   *
   * @param rcGroupId the Rocket.Chat group id
   * @return an {@link SessionStatisticsResultDTO} instance
   */
  @Cacheable(value = SessionCacheManagerConfig.SESSION_CACHE, key = "#rcGroupId")
  public SessionStatisticsResultDTO retrieveSessionViaRcGroupId(String rcGroupId) {
    return retrieveSession(null, rcGroupId);
  }

  private SessionStatisticsResultDTO retrieveSession(Long sessionId, String rcGroupId) {
    addDefaultHeaders(userStatisticsControllerApi.getApiClient());
    return userStatisticsControllerApi
        .getSession(sessionId, rcGroupId);
  }

  private void addDefaultHeaders(
      de.caritas.cob.statisticsservice.userstatisticsservice.generated.ApiClient apiClient) {
    var headers = this.securityHeaderSupplier.getCsrfHttpHeaders();
    tenantHeaderSupplier.addTechnicalTenantHeaderIfMultitenancyEnabled(headers);
    headers.forEach((key, value) -> apiClient.addDefaultHeader(key, value.iterator().next()));
  }

}
