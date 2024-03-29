package de.caritas.cob.statisticsservice.api.tenant;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessToken.Access;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AllTenantAccessTenantResolverTest {
  public static final long TECHNICAL_CONTEXT = 0L;
  @Mock
  HttpServletRequest authenticatedRequest;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  KeycloakAuthenticationToken token;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  AccessToken accessToken;

  @Mock
  Access access;

  @InjectMocks
  AllTenantAccessTenantResolver allTenantAccessTenantResolver;

  @Test
  void resolve_should_ResolveTechnicalTenantId_ForTenantSuperAdminUserRole() {
    // given
    when(authenticatedRequest.getUserPrincipal()).thenReturn(token);
    when(token.getAccount()
        .getKeycloakSecurityContext().getToken()).thenReturn(accessToken);
    when(accessToken.getRealmAccess().getRoles()).thenReturn(Sets.newLinkedHashSet("tenant-admin"));
    var resolved = allTenantAccessTenantResolver.resolve(authenticatedRequest);
    // then
    assertThat(resolved).contains(TECHNICAL_CONTEXT);
  }

  @Test
  void resolve_should_NotResolveTenantId_When_NonTechnicalUserRole() {
    // given
    when(authenticatedRequest.getUserPrincipal()).thenReturn(token);
    when(token.getAccount()
        .getKeycloakSecurityContext().getToken()).thenReturn(accessToken);
    when(accessToken.getRealmAccess().getRoles()).thenReturn(Sets.newLinkedHashSet("another-role"));
    var resolved = allTenantAccessTenantResolver.resolve(authenticatedRequest);
    // then
    assertThat(resolved).isEmpty();
  }
}