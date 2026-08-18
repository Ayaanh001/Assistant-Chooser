# Implementation Plan - Enhanced Shortcut UI, Grouping & Launch Fix

This plan details the improvements to the shortcut experience, including visual badging, grouping by parent app, and fixing the launch mechanism for manifest-defined shortcuts.

## User Review Required

> [!IMPORTANT]
> **Grouping Logic**: In the "Shortcuts" tab, I will group shortcuts under their parent app header. In the "Apps" tab, I will show the app and, if it has shortcuts, provide a way to expand them or show them immediately below. I'll stick to a clean "Grouped List" approach where the parent app is the primary entry.

## Proposed Changes

### Core Data Model

#### [MODIFY] [AssistantApp.kt](file:///C:/Users/Ah/StudioProjects/Assistant-Chooser/app/src/main/java/com/hussain/assistantchooser/core/AssistantApp.kt)
- Add `intent: Intent? = null` to support direct launching of static shortcuts.
- Add `parentIcon: Drawable? = null` to facilitate icon badging.

### Data Loading & Shortcut Parsing

#### [MODIFY] [AppLoader.kt](file:///C:/Users/Ah/StudioProjects/Assistant-Chooser/app/src/main/java/com/hussain/assistantchooser/data/AppLoader.kt)
- Update `parseStaticShortcuts` to extract `<intent>` attributes: `action`, `targetPackage`, `targetClass`, and `data`.
- Construct a proper `Intent` for these shortcuts.
- Store the parent app's icon in each `AssistantApp` object.
- Ensure `LauncherApps` path also populates `parentIcon`.

### UI Enhancements

#### [MODIFY] [AppIconItem.kt](file:///C:/Users/Ah/StudioProjects/Assistant-Chooser/app/src/main/java/com/hussain/assistantchooser/ui/components/AppIconItem.kt) and [AssistantAppRadioCard.kt](file:///C:/Users/Ah/StudioProjects/Assistant-Chooser/app/src/main/java/com/hussain/assistantchooser/main/AssistantAppRadioCard.kt)
- Implement a badge UI: If the item is a shortcut, draw the `parentIcon` as a small circle in the bottom-right corner of the main icon.

#### [MODIFY] [CustomAppPickerBottomSheet.kt](file:///C:/Users/Ah/StudioProjects/Assistant-Chooser/app/src/main/java/com/hussain/assistantchooser/main/CustomAppPickerBottomSheet.kt)
- Refactor the "Shortcuts" tab to group items by `packageName` using `StickyHeader` or grouped items.
- Grouping will show the App Name once, followed by all its shortcuts.

### Launch Fix

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Ah/StudioProjects/Assistant-Chooser/app/src/main/java/com/hussain/assistantchooser/main/MainActivity.kt) and [AssistantOverlayActivity.kt](file:///C:/Users/Ah/StudioProjects/Assistant-Chooser/app/src/main/java/com/hussain/assistantchooser/overlay/AssistantOverlayActivity.kt)
- Update `onAppClick` to prioritize `app.intent` if present. This ensures static shortcuts launch correctly without needing special launcher permissions.

## Verification Plan

### Manual Verification
1.  **Icon Badging**: Verify that all shortcuts in the picker and main list show a small parent app icon badge.
2.  **Shortcuts Grouping**: Open the "Shortcuts" tab in the picker and verify items are grouped by app.
3.  **Shortcut Launch**: Verify that "Incognito" (Chrome) and "Subscriptions" (YouTube) shortcuts open their respective screens directly.
4.  **Overlay Consistency**: Ensure the overlay grid also shows the badges and launches correctly.
