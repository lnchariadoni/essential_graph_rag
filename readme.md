[![Continuous Integration & Code Analysis](https://github.com/lnchariadoni/essential_graph_rag/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/lnchariadoni/essential_graph_rag/actions/workflows/build.yml)
[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=lnchariadoni_essential_graph_rag&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=lnchariadoni_essential_graph_rag)

## 🛠️ Quality & Engineering Discipline

This repository serves as a reference architecture for strict code quality, utilizing a three-layered guard system:

1. **Local Guard (Pre-commit Hook):** Uses a native Git hook to run `mvn clean verify` locally. It blocks commits immediately if code violates cyclomatic complexity limits.
2. **Gatekeeper (GitHub Actions):** Automates server-side verification on every `push` and `pull_request` to ensure the main branch remains clean.
3. **Auditor (SonarCloud):** Performs deep static code analysis, tracking long-term maintainability, cognitive complexity, and quality gates.

### Complexity Limits Enforced:
* **Max Cyclomatic Complexity per Method:** 6 (Managed via PMD)
