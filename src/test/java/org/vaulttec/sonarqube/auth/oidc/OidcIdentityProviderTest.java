/*
 * OpenID Connect Authentication for SonarQube
 * Copyright (c) 2017 Torsten Juergeleit
 * mailto:torsten AT vaulttec DOT org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaulttec.sonarqube.auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;

import java.util.Arrays;
import java.util.Collections;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.authentication.Display;
import org.sonar.api.server.authentication.OAuth2IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.api.utils.System2;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.openid.connect.sdk.AuthenticationRequest;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import javax.servlet.http.HttpServletRequest;
import com.nimbusds.oauth2.sdk.id.Subject;

public class OidcIdentityProviderTest extends AbstractOidcTest {

  private static final String ISSUER_URI = "http://localhost/auth/realms/sso";
  private static final String CLIENT_ID = "sonarqube";
  private static final String CLIENT_SECRET = "secret";

  private MapSettings settings;
  private OidcConfiguration config;
  private OidcClient client;
  private UserIdentityFactory userIdentityFactory;
  private OidcIdentityProvider underTest;

  @Before
  public void setUp() {
    settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE));
    settings.setProperty(OidcConfiguration.ISSUER_URI, ISSUER_URI);
    settings.setProperty(OidcConfiguration.CLIENT_ID, CLIENT_ID);
    settings.setProperty(OidcConfiguration.CLIENT_SECRET, CLIENT_SECRET);
    settings.setProperty(OidcConfiguration.ENABLED, true);

    config = new OidcConfiguration(settings.asConfig());
    client = spy(new OidcClient(config));
    doReturn(getProviderMetadata(ISSUER_URI)).when(client).getProviderMetadata();
    doReturn(mock(IDTokenValidator.class)).when(client).createValidator(any(), any());
    userIdentityFactory = mock(UserIdentityFactory.class);
    underTest = new OidcIdentityProvider(config, client, userIdentityFactory);
  }

  @Test
  public void getKey() {
    assertThat(underTest.getKey()).isEqualTo("oidc");
  }

  @Test
  public void getName() {
    assertThat(underTest.getName()).isEqualTo("OpenID Connect");
  }

  @Test
  public void getDisplay() {
    Display display = underTest.getDisplay();
    assertThat(display.getIconPath()).isEqualTo("/static/authoidc/openid.svg");
    assertThat(display.getBackgroundColor()).isEqualTo("#F7931E");
  }

  @Test
  public void isEnabled() {
    assertTrue(underTest.isEnabled());
  }

  @Test
  public void isEnabledWhenDisabled() {
    settings.setProperty(OidcConfiguration.ENABLED, false);
    config = new OidcConfiguration(settings.asConfig());
    underTest = new OidcIdentityProvider(config, client, userIdentityFactory);
    assertFalse(underTest.isEnabled());
  }

  @Test
  public void init() {
    OAuth2IdentityProvider.InitContext context = mock(OAuth2IdentityProvider.InitContext.class);
    when(context.generateCsrfState()).thenReturn("state");
    when(context.getCallbackUrl()).thenReturn("http://localhost/callback");
    when(client.createAuthenticationRequest(anyString(), anyString())).thenReturn(new AuthenticationRequest.Builder(
        new ResponseType("code"),
        new Scope("openid", "email", "profile"),
        new ClientID("id"),
        URI.create("http://localhost/callback"))
        .endpointURI(URI.create("http://localhost/auth"))
        .state(new State("state"))
        .build());
    underTest.init(context);
  }

  @Test
  public void callback() throws MalformedURLException {
    config = mock(OidcConfiguration.class);
    client = spy(new OidcClient(config));
    doReturn(getProviderMetadata(ISSUER_URI)).when(client).getProviderMetadata();
    doReturn(mock(IDTokenValidator.class)).when(client).createValidator(any(), any());
    underTest = new OidcIdentityProvider(config, client, userIdentityFactory);

    doReturn(true).when(config).isEnabled();
    doReturn(OidcConfiguration.LOGIN_STRATEGY_PREFERRED_USERNAME).when(config).loginStrategy();

    OAuth2IdentityProvider.CallbackContext context = mock(OAuth2IdentityProvider.CallbackContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    doReturn("GET").when(request).getMethod();
    doReturn(Collections.emptyEnumeration()).when(request).getHeaderNames();
    doReturn("state=state&code=valid_code").when(request).getQueryString();
    doReturn(new StringBuffer("http://localhost/callback")).when(request).getRequestURL();
    doReturn(request).when(context).getRequest();
    doReturn("http://localhost/callback").when(context).getCallbackUrl();

    AuthorizationCode authCode = new AuthorizationCode("valid_code");
    doReturn(authCode).when(client).getAuthorizationCode(request);

    UserInfo userInfo = mock(UserInfo.class);
    doReturn(new Subject("123")).when(userInfo).getSubject();
    doReturn("john.doo").when(userInfo).getPreferredUsername();
    doReturn("John Doo").when(userInfo).getName();
    doReturn("john.doo@acme.com").when(userInfo).getEmailAddress();
    doReturn(userInfo).when(client).getUserInfo(eq(authCode), eq("http://localhost/callback"));

    UserIdentity userIdentity = UserIdentity.builder()
        .setProviderId("123")
        .setProviderLogin("john.doo")
        .setLogin("john.doo")
        .setName("John Doo")
        .setEmail("john.doo@acme.com")
        .build();
    doReturn(userIdentity).when(userIdentityFactory).create(userInfo);

    doNothing().when(context).verifyCsrfState();

    underTest.callback(context);

    verify(context).authenticate(userIdentity);
    verify(context).redirectToRequestedPage();
  }

  private UserInfo newUserInfo(boolean singleGroup, boolean string) {
    try {
      return UserInfo.parse("{\"sub\":\"8f63a486-6699-4f25-beef-118dd240bef8\"," +
          (singleGroup ?
              (string ? "\"group\":\"admins\"," : "\"group\":[\"admins\"],") :
              (string ? "\"groups\":\"admins, internal\"," : "\"groups\":[\"admins\",\"internal\"],"))
          + "\"iss\":\"http://localhost/auth/realms/sso\",\"typ\":\"ID\",\"preferred_username\":\"jdoo\","
          + "\"given_name\":\"John\",\"aud\":\"sonarqube\",\"acr\":\"1\",\"nbf\":0,\"azp\":\"sonarqube\","
          + "\"auth_time\":1514307002,\"name\":\"John Doo\",\"exp\":1514307302,"
          + "\"session_state\":\"f57b7a35-0de4-4ac1-8d8e-a93fc8e65cb2\",\"iat\":1514307002,"
          + "\"family_name\":\"Doo\",\"jti\":\"c4a1a958-21de-47b6-b860-d0417519de00\",\"email\":\"john.doo@acme.com\"}");
    } catch (ParseException e) {
      // ignore
    }
    return null;
  }
}
