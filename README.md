# OpenID Connect (OIDC) Plugin for SonarQube
[![Build Status](https://github.com/vaulttec/sonar-auth-oidc/actions/workflows/build.yml/badge.svg)](https://github.com/vaulttec/sonar-auth-oidc/actions/workflows/build.yml) [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=org.vaulttec.sonarqube.auth.oidc%3Asonar-auth-oidc-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.vaulttec.sonarqube.auth.oidc%3Asonar-auth-oidc-plugin) [![Release](https://img.shields.io/github/release/vaulttec/sonar-auth-oidc.svg)](https://github.com/vaulttec/sonar-auth-oidc/releases/latest) [![Marketplace](https://img.shields.io/badge/Marketplace-authoidc-orange?logo=SonarQube)](https://www.sonarplugins.com/authoidc)

## ⚠️ Experimental Status

This plugin is currently in an experimental state. While it has been tested with various configurations, it should be used with caution in production environments. We recommend thorough testing in a staging environment before deployment.

## Description

This plugin enables users to automatically be sign up and authenticated on a SonarQube server via an [OpenID Connect](http://openid.net/connect/) identity provider like [Keycloak](http://www.keycloak.org).
![SonarQube Login](docs/images/login.png)

Optionally the groups a user is associated in SonarQube can be synchronized with the provider (via a custom userinfo claim retrieved from the ID token).

For communicating with the OpenID Connect provider this plugin uses the [Nimbus OAuth 2.0 SDK with OpenID Connect extensions](https://connect2id.com/products/nimbus-oauth-openid-connect-sdk).

## Prerequisites

### Server Base URL

SonarQube's `Server base URL` property must be set either by setting the
URL from SonarQube administration page (General > Server base URL) or the property `sonar.core.serverBaseURL` in the `sonar.properties`.

**In this URL no trailing slash is allowed!** Otherwise the redirects from the identity provider back to the SonarQube server are not created correctly.

### Force user authentication

If the plugin's Auto-Login feature is enabled then SonarQube's `Force user authentication` property must be enabled either from SonarQube administration page (Security > Force user authentication) or the property `sonar.forceAuthentication` in the `sonar.properties`.

**Otherwise the plugin won't be able to automatically redirect to the IdP's login page.**

### Network Proxy

If a [network proxy](https://docs.oracle.com/javase/8/docs/api/java/net/doc-files/net-properties.html#Proxies) is used with SonarQube (via `http[s].proxy[Host|Port]` properties in the `sonar.properties`) and the host name of the identity provider is not resolvable by this proxy then the IdP's host name must be excluded from being resolved by the proxy. This is done by defining the property `http.nonProxyHosts` in the `sonar.properties`.

**Otherwise the plugin won't be able to send the token request to the IdP.**

## Step-by-Step Implementation Guide

### 1. Installation

1. Download the latest plugin JAR from [GitHub Releases](https://github.com/vaulttec/sonar-auth-oidc/releases)
2. Place the JAR file in the `SONARQUBE_HOME/extensions/plugins/` directory
3. Restart the SonarQube server

### 2. Identity Provider Configuration

#### For Keycloak:
1. Log in to your Keycloak admin console
2. Create a new client:
   - Client ID: `sonarqube`
   - Client Protocol: `openid-connect`
   - Access Type: `confidential` (recommended for production)
   - Valid Redirect URIs: `https://your-sonarqube-domain/oauth2/callback/oidc`
   - Web Origins: `+` (or your specific SonarQube domain)
3. Save the client and note the Client Secret
4. Create a mapper for groups (if needed):
   - Name: `groups`
   - Mapper Type: `Group Membership`
   - Token Claim Name: `groups`
   - Full group path: `false`
   - Add to ID token: `true`
   - Add to access token: `true`
   - Add to userinfo: `true`

#### For Other OIDC Providers:
1. Create a new OIDC client application
2. Configure the following:
   - Client ID: `sonarqube`
   - Client Secret: Generate and save
   - Redirect URI: `https://your-sonarqube-domain/oauth2/callback/oidc`
   - Scopes: `openid`, `email`, `profile`, `groups` (if needed)
3. Note the Issuer URI (without the `/.well-known/openid-configuration` path)

### 3. SonarQube Configuration

1. Log in to SonarQube as an administrator
2. Navigate to Administration > Security > OpenID Connect
3. Configure the following settings:
   - Enabled: `true`
   - Issuer URI: Your OIDC provider's base URL
   - Client ID: `sonarqube`
   - Client Secret: The secret from your OIDC provider
   - Login Strategy: `Preferred Username` (recommended)
   - Allow Users to Sign Up: `true` (if you want automatic user creation)
   - Auto-Login: `false` (recommended for initial setup)
   - Groups Sync: `true` (if you want to sync groups)
   - Groups Sync Claim Name: `groups` (or your custom claim name)

### 4. Testing the Configuration

1. Clear your browser cookies for the SonarQube domain
2. Access SonarQube
3. Click the "OpenID Connect" login button
4. You should be redirected to your OIDC provider's login page
5. After successful login, you should be redirected back to SonarQube

### 5. Troubleshooting

If you encounter issues:

1. Enable debug logging in SonarQube:
   - Go to Administration > System > Logs level
   - Set the level to DEBUG
   - Download the Web Server log

2. Common issues and solutions:
   - **Redirect URI mismatch**: Ensure the redirect URI in SonarQube matches exactly with what's configured in your OIDC provider
   - **Invalid client secret**: Verify the client secret is correctly copied
   - **Network issues**: Check if your SonarQube server can reach the OIDC provider
   - **Group sync issues**: Verify the group claim name matches between SonarQube and your OIDC provider


## Contributing

We welcome contributions to this project. Please feel free to:
- Report issues
- Submit pull requests
- Suggest improvements
- Share your implementation experiences

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
