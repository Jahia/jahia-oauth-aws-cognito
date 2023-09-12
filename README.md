# jahia-oauth-aws-cognito

This module is a community module to show how to implement an authentication based on OAuth protocol.

`jahia-oauth-aws-cognito` module depends on `jahia-authentication` and `jahia-oauth` modules and relies on Amazon Web Services Cognito API.

## What the module does ?

- The module provides a user and group provider set up by the Jahia Administration in server settings.
- Once activated, the module provides a custom login url provider when a guest user wants to access to an authenticated resource.
  - `AwsCognitoConnectAction`: Jahia Action to redirect the user to the AWS Cognito authentication URL
  - `AwsCognitoCallbackAction`: Jahia Action callback called by AWS Cognito to identify the user
- You can also set up in site settings a custom login url with a secret key to bypass OpenID connect implementation.
  - The module exposes over HTTP GET a custom JAX-RS endpoint to identify the user.

## How to configure the module ?

- Download and deploy the module [jahia-oauth-aws-cognito](https://store.jahia.com/contents/modules-repository/org/jahia/community/jahia-oauth-aws-cognito.html)
- Activate the module in a site (systemsite for instance)
- Go to Server settings: set up  user and group hot provisioning
  - `accessKeyId`: AWS access key
  - `secretAccessKey`: AWS secret access key
  - `region`: AWS region
  - `userPoolId`: AWS user pool ID
- Go to Site settings: set up the fields and toggle the slide `Activate` otherwise the login url provider will not work (only Jahia Actions still work)
  - OpenID Connect implementation:
    - `endpoint`: AWS endpoint
    - `region`: AWS region
    - `clientId`: AWS client ID
    - `clientSecret`: AWS client secret
  - External login URL: toggle the slider `With custom login`
    - `secretKey`: Custom secretKey to identifity the user
    - `loginUrl`: Custom login URL
