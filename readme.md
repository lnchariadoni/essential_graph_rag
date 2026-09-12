# My Java Concepts Project 🚀

[![Java CI with Maven](https://github.com)](https://github.com)
[![Quality Gate Status](https://sonarcloud.io)](https://sonarcloud.io)
[![Complexity](https://sonarcloud.io)](https://sonarcloud.io)

## 🛠️ Quality & Engineering Discipline

This repository serves as a reference architecture for strict code quality, utilizing a three-layered guard system:

1. **Local Guard (Pre-commit Hook):** Uses a native Git hook to run `mvn clean verify` locally. It blocks commits immediately if code violates cyclomatic complexity limits.
2. **Gatekeeper (GitHub Actions):** Automates server-side verification on every `push` and `pull_request` to ensure the main branch remains clean.
3. **Auditor (SonarCloud):** Performs deep static code analysis, tracking long-term maintainability, cognitive complexity, and quality gates.

### Complexity Limits Enforced:
* **Max Cyclomatic Complexity per Method:** 6 (Managed via PMD)
