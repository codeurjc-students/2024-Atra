# Security guide
## Introduction
There are many classes involved in the security of the app. Some are implementations of spring interfaces, some are classes provided by Spring, and some are custom classes unrelated to Spring. This doc will gather these classes, along a brief explanation of their function and source.
## Classes for which I provide implementations
### Custom
- AuthResponse: POJO holding String attributes for status, message and error. Used as the body of ResponseEntity during authentication.
- JwtCookieManager: POJO responsible for creating cookies to hold tokens.
- JwtTokenProvider: POJO providing token functionality. Allows creation, validation, and extracting data from the tokens.
- LoginRequest: Simple holder class. Holds a username and a password, and provides access
- SecurityCipher: POJO providing encrypting and decrypting functionalities.
- Token: data class holding token data
- UserLoginService: handles login/logout, and token refreshing. Used by the LoginController.
- RestSecurityConfig: configures the AuthenticationManager and general security settings (permissions for each endpoint, disabling session management, etc)
### Spring
- JwtRequestFilter: a Filter which intercepts requests. It checks their access tokens, and, if valid, logs the user in
- CSRFHandlerConfiguration: does CSRF things, probably unnecessary, as CSRF is disabled. 
- CSRFHandlerInterceptor: does CSRF things, probably unnecessary, as CSRF is disabled.
- RepositoryUserDetailsService: Implements UserDetailsService. This means it implements a method to load a user from database by its username.

## Classes defined by Spring
- UserDetails: holds the username, password, authorities of a user, as well as some other methods to check the data is valid (not expired)
- AuthenticationManager: used to authenticate users. Created and configured in RestSecurityConfig, uses RepositoryUserDetailsService to access user data.
- Authentication: holds information about a user and their authentication state (a Principal, their Authorities, Credentials, Details, and whether they are authenticated).
