# Project Guidelines

## Development Environment
- **Operating System**: macOS (Apple Silicon).
- **Architecture**: ARM64 optimized.
- **Shell**: zsh (default for macOS).
- **Tools**: 
    - Use `brew` for dependency management where applicable.
    - Ensure all terminal commands are compatible with macOS (e.g., `sed -i ''` instead of `sed -i`, `bsdtar` behavior).
    - Project initialization should account for `aarch64` if native binaries are used.

## Development Standards
- **Framework**: Java Spring Boot 3 (target version >= 3.4.1).
- **JDK**: Java 17 or higher (LTS), optimized for Apple Silicon (e.g., Azul Zulu or Liberica).
- **Architecture**: 
    - **Back-end**: Pure Back-end API (Spring Boot).
    - **Front-end**: Separated from back-end, communicating via REST/OIDC.
    - **Identity**: Follow the "Connector + Normalization" architecture (DHF-04).
- **Security**: 
    - Separate Authentication (AuthN) from Authorization (AuthZ).
    - AuthN: Delegate to external IdPs (OIDC/SAML).
    - AuthZ: Internal RBAC (Pathologist, Technician, Admin).
    - Session: 12-hour secure, HTTP-only cookies.
    - Credentials: Store in `.env` or environment variables; never commit to Git.
- **Testing**: 
    - Mandatory unit and integration tests for AuthN/AuthZ logic.
    - Traceability to `SRS` and `SDS` must be maintained.

## Quality Management System (QMS)
- All design changes must be reflected in the Design History File (DHF) in `qms/dhf/`.
- Traceability between User Needs (PURS), System Requirements (SRS), and Software Design (SDS) is required.
- Risk management must be updated in `05b-Hazard-Analysis.md` when new features introduce hazards.

## Tooling & Automation
- **Build Tool**: Maven (preferred) or Gradle.
- **Secrets**: Use `.env` file for local development (ensure it is in `.gitignore`).
- **Initialization**: When using `curl` for Spring Initializr, ensure proper escaping for `zsh` and use `-o` to save files before unzipping.
- **ZIP/Unzip**: Use standard macOS `unzip` or `tar`.
