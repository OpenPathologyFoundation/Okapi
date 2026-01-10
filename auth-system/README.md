# Okapi Auth System

A robust, enterprise-grade authentication and authorization backend for Okapi, built with **Spring Boot 3.5.6** and **Java 25**.

## 🚀 Features

-   **Java 25 & Spring Boot 3.5.6**: Leveraging the latest Java features with a stable, compatible Spring Boot release.
-   **OIDC Authentication**: Fully integrated with Keycloak (or any OIDC provider like Okta, Auth0) for secure login.
-   **Identity Normalization**: Automatic mapping of external identities (from IdP) to internal `Identity` objects.
-   **Role-Based Access Control (RBAC)**: Maps IdP groups (e.g., `Okapi_Pathologists`) to internal roles (`PATHOLOGIST`, `ADMIN`).
-   **Comprehensive Testing**: Includes unit tests and Docker-based integration tests using a local Keycloak instance.

---

## 🛠 Project Structure

-   `src/main/java/com/okapi/auth`
    -   `config`: Security configuration (`SecurityFilterChain`).
    -   `model`: `Identity` and `Role` definitions.
    -   `service`: `CustomOidcUserService` (Auto-User Creation) and `UserRoleMapper`.
    -   `controller`: Endpoints like `/me` (User Profile) and `/login`.
-   `src/test/java`: Extensive test suite.
    -   `integration/KeycloakIntegrationTest`: Verifies OIDC flow against a real (Dockerized) Keycloak.

---

## 🧪 Running Tests

This project includes a Docker Compose environment for testing against a real Keycloak instance.

### Prerequisites

-   Docker & Docker Compose running.
-   Java 25 installed.

### Steps

1.  **Start Keycloak**:
    ```bash
    docker compose up -d
    ```
    *(This starts Keycloak on port 8180 and automatically imports the `okapi` realm)*

2.  **Run Tests**:
    ```bash
    ./gradlew test
    ```
    *(You will see detailed DEBUG logs showing the connection to Keycloak:8180)*

---

## 🐘 Connecting to the Database

The project runs a Dockerized **PostgreSQL 16** instance.

### Connection Details
-   **Host**: `localhost`
-   **Port**: `5432` (Default, see troubleshooting below)
-   **Database**: `okapi_auth`
-   **Username**: `okapi_service`
-   **Password**: `postgres_dev_password` (defined in `.env`)

### ⚠️ Troubleshooting: Port Conflicts
If you have a **local PostgreSQL** installation running on port `5432`, Docker may fail to bind or you might accidentally connect to your local DB instead of the container.

**Solution**: Change the Docker mapping in your `.env` file to use a different host port (e.g., `5433`):

```bash
# .env
POSTGRES_PORT=5433
```

Then restart Docker:
```bash
docker compose down
docker compose up -d
```

You can now connect via **`localhost:5433`** while the container internally uses 5432.

---

## 🔐 Enabling Enterprise SAML Integration

The system is pre-wired for SAML 2.0 but it is currently disabled to allow for OIDC-first development. Follow these strict steps to enable SAML for enterprise accounts.

### Step 1: Add Dependency
Open `build.gradle` and **uncomment** line 22:

```groovy
// FROM:
// implementation 'org.springframework.boot:spring-boot-starter-saml2-service-provider'

// TO:
implementation 'org.springframework.boot:spring-boot-starter-saml2-service-provider'
```

### Step 2: Enable Security Chain
Open `src/main/java/com/okapi/auth/config/SecurityConfig.java` and **uncomment** line 29, adding the import `org.springframework.security.config.Customizer`:

```java
// FROM:
// .saml2Login(Customizer.withDefaults()); // Enable when metadata is ready

// TO:
.saml2Login(org.springframework.security.config.Customizer.withDefaults());
```

### Step 3: Configure Metadata
Open `src/main/resources/application.yml` and **uncomment** lines 15-20. Ensure you provide the valid Metadata URL from your Identity Provider (IdP).

```yaml
# FROM:
#      saml2:
#        relying-party:
#          registration:
#            okapi-saml:
#              asserting-party:
#                metadata-uri: ${SAML_IDP_METADATA_URL}

# TO:
      saml2:
        relying-party:
          registration:
            okapi-saml:
              asserting-party:
                metadata-uri: ${SAML_IDP_METADATA_URL}
```

### Step 4: Environment Variables
Add the metadata URL to your `.env` file or environment variables:

```bash
SAML_IDP_METADATA_URL=https://your-idp.com/metadata.xml
```

### Step 5: Verification
Run the application. Spring Security will automatically generate the Service Provider (SP) metadata at:
`http://localhost:8080/saml2/service-provider-metadata/okapi-saml`

Share this URL (or the XML content) with your Enterprise Identity Provider to complete the trust establishment.
