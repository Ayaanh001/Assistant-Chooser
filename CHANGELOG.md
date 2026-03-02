# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3] - 28-02-2026

### Added
- Overlay app source setting — choose whether the overlay shows assistant apps or your custom list
- Show app names toggle for the overlay grid
- Skeleton loading screen — shimmer placeholder UI matching the real list and overlay grid while apps load, replacing the spinner


### Changed
- Full project restructure
- R8 minification and resource shrinking, which reduced app size.
- `ArrowBack` icon replaced with `AutoMirrored.Filled.ArrowBack` for correct RTL support

### Fixed
- Square ripple on app name tap area in main list — now clips to rounded shape with haptic feedback
- Square ripple on dropdown menu items — corners now match the menu's rounded shape

---

## [1.2] - 11-12-2025

### Added
- **Filter button** — switch between Assistant Apps and Custom Apps; users can select any installed app to appear in the list
- Show / hide package name option in Settings
- Check for updates button in Settings — fetches the latest GitHub release directly from the app
- Predictive back gesture support

### Fixed
- Radio button was not syncing with the currently set default digital assistant

---

## [1.1] - 07-12-2025

### Added
- **Add Tile** — add the Quick Settings tile directly from the app in one tap
- **Open App** setting — when enabled, tapping an app opens the full app instead of its voice assistant
- **Auto-close after launch** setting — automatically closes Assistant Chooser after an assistant or activity is launched
- About section in Settings with developer info and source code link

### Fixed
- Gemini Assistant was not launching; the app now falls back to Google Voice Search as a workaround until a proper fix is available

---

## [1.0] - 16-10-2024

### Added
- Initial public release
- View and launch all installed assistant and voice search apps from one place
- Radio button to navigate to system default assistant settings for faster switching
- Assistant overlay triggered by the home button long press, bottom corner swipe gesture, or Quick Settings tile
- Material You (Material 3) UI with dynamic colour support
