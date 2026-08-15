---
name: meetflix-coding-standards
description: >-
  Coding standards and architectural guidelines for Meetflix: prioritize reusable composables,
  design new components for reusability, enforce Clean Architecture, document applied design patterns inline,
  and follow consistent Conventional Commit messages.
---

# Meetflix Development & Architectural Guidelines

Whenever developing, refactoring, or extending code in this repository, strictly adhere to the following 5 core rules:

## 1. Prioritize Existing Reusable Composables
* Before creating any new UI element, inspect `com.meetflix.core.ui.components` (e.g. `MFXButton`, `MFXHeader`, `MFXTabBar`) and existing widgets under `com.meetflix.<feature>.presentation.widgets`.
* Reuse established theme tokens, brushes, fonts (`Geist_Font`), and styling (`blackRedGradient`, `whiteRedGradientBorder`) rather than redefining ad-hoc styles.

## 2. Design New Components for High Reusability
* When a new UI element is required, extract it as a standalone, modular Composable in `presentation/component` or `presentation/widgets`.
* Ensure parameters are clean, stateless, and use stable Jetpack Compose types (e.g., pass lambdas `() -> Unit` rather than unstable activity result launchers or arrays).

## 3. Maintain Strict Clean Architecture
* **Presentation**: Compose UI Screens & ViewModels (`com.meetflix.<feature>.presentation.*`).
* **Domain**: Immutable State & Domain Models (`com.meetflix.<feature>.domain.models.*`).
* **Data**: Repositories, Services & Native Managers (`com.meetflix.<feature>.data.*`).
* ViewModels must consume data/managers via `StateFlow` and expose unidirectional `uiState`.

## 4. Document Applied Design Patterns Inline
* Whenever implementing a recognized design pattern (e.g., **Repository**, **Observer / MVI**, **Strategy**, **Manager / Wrapper**, **Factory**, **State**, **Adapter**), place a concise 1-2 line Kotlin KDoc comment immediately above the symbol header explaining the pattern and its rationale.
* Example:
  ```kotlin
  /**
   * Design Pattern: Observer / StateFlow (MVI)
   * Exposes immutable UI state to Compose UI components in a unidirectional flow.
   */
  val uiState: StateFlow<NearbyConnectionState> = nearbyManager.uiState
  ```

## 5. Consistent Git Commit Messages
* Follow the **Conventional Commits** specification format:
  ```text
  <type>(<scope>): <short summary in imperative mood>

  - <detailed bullet point 1>
  - <detailed bullet point 2>
  ```
* Standard types: `feat`, `fix`, `refactor`, `style`, `docs`, `test`, `chore`.
* Standard scopes: `connections`, `player`, `home`, `party`, `profile`, `upload`, `core`.
