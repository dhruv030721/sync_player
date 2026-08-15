# Meetflix Coding Standards

1. **Priority on Reusable Composables**: Reuse existing `com.meetflix.core.ui.components` (`MFXButton`, `MFXHeader`, `MFXTabBar`) and widgets before creating new UI elements.
2. **Reusable Component Design**: New UI components must be modular, stateless, and reusable.
3. **Preserve Clean Architecture**: Maintain separation between `data`, `domain`, and `presentation` layers.
4. **Inline Design Pattern Documentation**: Write a short 1-2 line comment above code where a design pattern is used (e.g. Observer, Repository, Strategy, Manager, State).
5. **Consistent Git Commit Messages**: Follow Conventional Commits format (`type(scope): summary`).
