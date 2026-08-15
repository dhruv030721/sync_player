# Meetflix Development Rules & Coding Standards

Always follow these 5 mandatory rules when writing code in this repository:

1. **Priority on Reusable Composables**: Always check `com.meetflix.core.ui.components` (`MFXButton`, `MFXHeader`, `MFXTabBar`) and existing feature widgets before creating a new UI component.
2. **Make New Components Reusable**: Every new component must be extracted as a reusable, stateless Composable with clean, stable parameters.
3. **Preserve Clean Architecture**: Maintain separation between `presentation`, `domain`, and `data` layers across all features.
4. **Document Applied Design Patterns Inline**: Place a short 1-2 line Kotlin doc comment (`/** Design Pattern: <PatternName> - <Description> */`) directly above any class, function, or property implementing a design pattern (e.g. Repository, Observer, Strategy, Manager, State, Factory).
5. **Consistent Git Commit Messages**: Always format git commits according to Conventional Commits: `<type>(<scope>): <summary>` followed by bulleted details.
