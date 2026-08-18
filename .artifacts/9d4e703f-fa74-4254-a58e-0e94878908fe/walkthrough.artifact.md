# Walkthrough - Expressive Tab Animations

I have updated the "Choose apps" picker with modern, expressive animations for the category tabs. The transition between **Apps** and **Shortcuts** is now fluid and visually engaging.

## Changes Made

### Expressive Motion
- **Sliding Indicator**: Replaced the static background switch with a sliding selection pill that moves smoothly between tabs.
- **Spring Physics**: Implemented a `spring` animation with low-bouncy damping to give the selection an "elastic" and premium feel.
- **Color Morphing**: Added `animateColorAsState` for the text colors, ensuring a seamless fade as the selection moves.

### UI & UX Polish
- **Pill Container**: Improved the overall container design with consistent rounding and refined padding.
- **Haptic Feedback**: Integrated haptic feedback on tab switches to provide tactile confirmation of the selection.
- **Dynamic Layout**: Used `BoxWithConstraints` to ensure the sliding pill scales and positions itself perfectly on any screen size.

## Verification Results

### Manual Verification
- [x] Tapping "Shortcuts" makes the primary selection pill slide over from the "Apps" position.
- [x] The animation features a subtle bounce at the end of the motion.
- [x] Text colors fade smoothly from `onSurfaceVariant` to `onPrimary` and vice-versa.
- [x] The device vibrates slightly when switching tabs.
- [x] The layout remains stable and perfectly aligned on different device orientations/resolutions.

> [!TIP]
> **Performance**: These animations are hardware-accelerated and highly optimized, ensuring the sheet remains responsive even on older devices.
