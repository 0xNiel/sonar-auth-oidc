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

import org.junit.Test;
import org.sonar.api.Plugin;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.utils.Version;

public class AuthOidcPluginTest {
  private static final Version VERSION_7_4 = Version.create(7, 4);
  private static final Version VERSION_10_0 = Version.create(10, 0);

  @Test
  public void testExtensions() {
    Plugin.Context context = new Plugin.Context(SonarRuntimeImpl.forSonarQube(VERSION_7_4, SonarQubeSide.SERVER, SonarEdition.COMMUNITY));
    new AuthOidcPlugin().define(context);
    assertThat(context.getExtensions()).hasSize(20);
  }

  @Test
  public void testExtensionsForSonarQube10() {
    Plugin.Context context = new Plugin.Context(SonarRuntimeImpl.forSonarQube(VERSION_10_0, SonarQubeSide.SERVER, SonarEdition.COMMUNITY));
    new AuthOidcPlugin().define(context);
    assertThat(context.getExtensions()).hasSize(20);
  }
}
