## ADDED Requirements

### Requirement: API authenticates requests only from verified tokens

`api/` MUST validate a token's signature before trusting claims for identity or authorization. An accepted token MUST conform to the current issuer contract: JWT HS256 with the required `sub`, `role` and `exp` claims.

#### Scenario: Accept a valid token

- **GIVEN** a protected API operation and an unexpired token with a valid signature and all required claims
- **WHEN** the client submits the token as a Bearer token
- **THEN** the API authenticates the request using the verified claims
- **AND** the operation preserves its existing successful response

#### Scenario: Reject an invalid signature

- **GIVEN** a protected API operation and a token with an invalid signature
- **WHEN** the client submits the token as a Bearer token
- **THEN** the API does not treat the request as authenticated or authorized

#### Scenario: Reject altered token content

- **GIVEN** a protected API operation and a token whose content was changed without a corresponding valid signature
- **WHEN** the client submits the token as a Bearer token
- **THEN** the API does not trust the changed claims or grant access based on them

#### Scenario: Reject expired or incomplete tokens

- **GIVEN** a protected API operation and a token that is expired or lacks `sub`, `role` or `exp`
- **WHEN** the client submits the token as a Bearer token
- **THEN** the API does not treat the request as authenticated or authorized

### Requirement: Token verification fails closed without verification secret

`api/` MUST NOT accept a token or grant access based on its claims when the configured verification secret is unavailable.

#### Scenario: Verification secret is unavailable

- **GIVEN** a protected API operation and no configured verification secret
- **WHEN** the client submits a token as a Bearer token
- **THEN** the API does not treat the request as authenticated or authorized

### Requirement: Token consumer roles remain explicit

The token flow MUST preserve the documented roles: `auth/` issues tokens, `api/` verifies tokens before using their claims, and `web/` transports the token without acting as a signature verifier or authorization authority.

#### Scenario: Repository token flow is inventoried

- **GIVEN** the in-repository token consumers documented by the project
- **WHEN** the H-AUTH-03 verification scope is reviewed
- **THEN** `api/` is identified as the verifier and `web/` as a transport-only client
- **AND** any consumer outside the repository remains explicitly unconfirmed unless separate evidence identifies it