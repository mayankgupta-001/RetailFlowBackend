# Contributing to RetailFlow Backend

Thank you for your interest in contributing to RetailFlow Backend. Contributions are welcome through issues and pull requests.

## Before you start

For significant changes, open an issue first so the proposed solution can be discussed before implementation. Small documentation fixes and clearly scoped bug fixes can usually be submitted directly as pull requests.

Please search existing issues and pull requests before creating a new one.

## Development setup

1. Fork the repository on GitHub.
2. Clone your fork and create a feature branch:

   ```bash
   git clone https://github.com/<your-username>/RetailFlowBackend.git
   cd RetailFlowBackend
   git checkout -b feature/short-description
   ```

3. Copy `.env.example` to `.env` and provide local development values.
4. Start PostgreSQL with Docker Compose or use a local PostgreSQL instance.
5. Run the application and tests with the Maven Wrapper.

```bash
./mvnw test
./mvnw spring-boot:run
```

The project uses Flyway for database migrations. Do not use Hibernate schema auto-update for shared databases. Add schema changes as a new versioned migration under:

```text
src/main/resources/db/migration/
```

Use the naming format:

```text
V7__description_of_change.sql
```

Never rewrite a migration that has already been applied. Add a new migration instead.

## Making changes

- Keep pull requests focused on one problem or feature.
- Follow the existing package and service structure.
- Put business rules in the service layer rather than controllers.
- Validate input at the API boundary and enforce important rules again on the backend.
- Avoid committing credentials, tokens, local database files, or generated build output.
- Update documentation when behavior or setup changes.
- Add or update tests for bug fixes and new behavior.

## Testing requirements

Before opening a pull request, run:

```bash
./mvnw test
```

Your pull request should include tests for the changed behavior where practical. Database-related changes should verify that Flyway migrations run successfully and that Hibernate schema validation passes.

## Commit and pull request guidelines

Use a clear commit message that explains the change. Keep the pull request description concise but include:

- What changed.
- Why it changed.
- How it was tested.
- Any migration, deployment, or compatibility considerations.

Pull requests should pass the available checks and receive review before merging. The repository maintainer makes the final merge decision.

## Reporting security issues

Please do not disclose security vulnerabilities in a public issue. Report them privately to the repository maintainer or through GitHub's private vulnerability reporting feature when enabled.

## License

By contributing to RetailFlow Backend, you agree that your contribution may be distributed under the repository's [MIT License](LICENSE).
