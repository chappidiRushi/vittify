# Vittify

Vittify is a minimalist, AI-powered Android expense tracker. It reads transaction SMS on-device, automatically categorizes them, and presents spending as a simple, delightful financial timeline. It now features optional Gemini AI integration for natural language queries.

The product should feel like:

> **A tiny, smart financial companion that makes tracking money surprisingly delightful.**

The design philosophy is **Material 3 Expressive** — tactile, tonal, playful, calm, highly scannable, accessible, and intentionally restrained.

---

# 1. Product Principles

Vittify follows these principles in priority order:

1. **Clear first. Delightful second.**
2. **Fast and lightweight.**
3. **Financial information must be immediately understandable.**
4. **Expressiveness should communicate hierarchy, interaction, or feedback — never decorative noise.**
5. **Consistency over novelty.**
6. **Accessibility over aesthetics.**
7. **Meaning over color.**
8. **Motion should explain causality.**
9. **Tonal surfaces over borders.**
10. **Prefer simple interactions over complicated workflows.**

A useful rule:

> If everything is expressive, nothing is expressive.

Expressive components should therefore be surrounded by calm, quiet UI.

---

# 2. Architecture & Engineering Standards

## Architecture

Use:

* MVVM
* Clean Architecture
* UDF / Unidirectional Data Flow
* Jetpack Compose
* Material 3 Expressive
* Hilt
* Room
* WorkManager
* StateFlow

Architecture:

```text
UI
 ↓
ViewModel
 ↓
Use Case
 ↓
Repository
 ↓
Data Source
```

### UI

Responsible for:

* Rendering immutable UI state
* Collecting StateFlow
* Emitting user intents
* Navigation
* Animations
* Accessibility semantics

UI must NOT contain:

* Business logic
* Database operations
* SMS parsing
* Financial calculations
* Repository calls
* Complex state transformations

### Domain

Contains:

* Business rules
* Use cases
* Financial calculations
* Categorization logic
* Transaction classification
* Validation rules

Domain code must remain independent of Android UI.

### Data

Contains:

* Room
* DAOs
* Entities
* Repositories
* SMS data sources
* Preferences
* Parser integration

---

# 3. Parser Architecture

All bank parsing belongs in:

```text
parser-core/src/main/kotlin/com/vittify/parser/core/bank/
```

Bank parsers:

* Extend `BankParser`
* Implement bank identification
* Parse transaction SMS
* Return `ParsedTransaction`
* Register in `BankParserFactory`

The main application maps parser models into Room entities.

Follow:

```text
docs/parser-test-standards.md
```

Do not move bank parsing logic into UI or application modules.

---

# 4. Coding Standards

Use idiomatic Kotlin.

Prefer:

* Small composables
* Immutable UI state
* `StateFlow`
* Sealed classes/interfaces for UI states where appropriate
* Explicit state ownership
* Reusable components
* Named design tokens
* Composition over duplication

Avoid:

* God composables
* Deeply nested composables
* Magic numbers
* Hardcoded colors
* Hardcoded typography
* Hardcoded spacing
* Business logic inside composables
* Duplicate UI implementations
* Unnecessary abstractions

## Important

Do not create arbitrary design values.

Avoid:

```kotlin
padding(13.dp)
RoundedCornerShape(21.dp)
```

Prefer:

```kotlin
VittifySpacing.standard
VittifyShapes.hero
```

Every repeated design value should eventually become a token.

---

# 6. Material 3 Expressive Design System

## 6.1 General Philosophy

Vittify uses Material 3 Expressive with:

* Generous squircle geometry
* Tonal surfaces
* Spring physics
* Tactile interactions
* Expressive typography
* Strong monetary hierarchy
* Restrained semantic colors
* Rich but purposeful haptics
* Accessible touch targets

The interface should feel physical without becoming cartoonish.

---

# 7. Design Token Architecture

The design system must have a centralized token layer.

```text
Foundation
├── Color tokens
├── Typography tokens
├── Shape tokens
├── Spacing tokens
├── Elevation tokens
├── Motion tokens
├── Icon tokens
└── Component tokens
```

Screens should consume these tokens rather than inventing their own values.

---

# 8. Spacing System

Use a consistent spacing scale.

```text
4dp   Micro
8dp   Tight
12dp  Compact
16dp  Standard
20dp  Comfortable
24dp  Section
28dp  Expressive
32dp  Hero
```

Do not introduce one-off spacing values unless there is a documented design reason.

---

# 9. Color Architecture

Never use arbitrary hardcoded colors.

Use Material 3 semantic color roles.

## Tonal Surface Hierarchy

### `surfaceContainerLowest`

Use for:

* Recessed wells
* Deep canvas
* AMOLED base
* Very low emphasis backgrounds

### `surfaceContainerLow`

Use for:

* Default screen background
* Primary canvas

### `surfaceContainer`

Use for:

* Cards
* Modular widgets
* Grouped platters
* Main content islands

### `surfaceContainerHigh`

Use for:

* Inputs
* Search bars
* Interactive surfaces
* Highlighted states
* Switch tracks

### `surfaceContainerHighest`

Use for:

* Dialogs
* Bottom sheets
* Elevated pills
* Strongly elevated interactive surfaces

---

# 10. Dynamic & Curated Themes

Support:

* Dynamic Monet theming on Android 12+
* Curated expressive themes

Example curated themes:

* Latte
* Macchiato
* Rosé Pine

Theme customization must preserve semantic meaning and accessibility.

Never allow customization to reduce important financial information below accessibility contrast requirements.

---

# 11. Financial Semantic Colors

Use semantic colors consistently.

### Expenses / Outflow

Soft coral / terracotta.

### Income / Inflow

Fresh emerald / mint.

### Transfers

Playful lavender / wisteria.

### Investments / Savings

Deep teal / cyan.

### Warnings / Thresholds

Warm amber / tangerine.

### Important Rule

Never communicate financial meaning through color alone.

Always combine color with at least one of:

* Sign
* Icon
* Direction
* Text
* Label

Example:

```text
↓ ₹2,450
```

must remain understandable even without color.

---

# 12. Typography

Primary font:

**SN Pro**

Use:

* Medium
* Bold
* ExtraBold

Use system fallback where required.

Typography must have a clear hierarchy.

Avoid using too many font sizes on one screen.

---

# 13. Financial Number Formatting

Financial numbers are the most important visual information in Vittify.

## Hero Balance

Use:

* Currency symbol: smaller and lighter
* Major amount: large and bold
* Decimal/paise: smaller and quieter

Example:

```text
₹ 48,250.75
```

with:

* `₹` → title-level
* `48,250` → headline/display
* `.75` → body-level

## Transaction Amount

Use directional indicators:

```text
↓ ₹200
↑ ₹5,000
```

Debit:

* Soft coral

Credit:

* Fresh mint

Transfers:

* Lavender

---

# 14. Financial UX Rules

Financial interfaces require extra clarity.

Always:

* Show the amount clearly.
* Show the transaction direction.
* Use localized currency formatting.
* Preserve meaningful decimal precision.
* Make the final amount unmistakable.
* Keep account/source information visible where relevant.

Never:

* Hide important financial information behind gestures.
* Use animation that makes a value difficult to read.
* Use color as the only indicator of transaction type.
* Use playful motion for serious security or financial warnings.
* Obscure the account involved in an important action.

For important confirmations, prioritize:

```text
Amount
↓
Account
↓
Action
↓
Confirmation
```

---

# 15. Shape System

Vittify uses squircle geometry.

## Tokens

### Hero / Modular Widgets

```text
28dp
```

### Grouped Platters / Islands

```text
24dp–28dp
```

### Inputs / Nested Pickers

```text
16dp–18dp
```

### Buttons / Primary CTAs

```text
18dp
```

### Pills / Chips / Tags

```text
50dp / capsule
```

### Bottom Sheets

```text
32dp top corners
```

### Dialogs

```text
28dp
```

---

# 16. Grouped Squircle Platter Rule

Related content MUST live inside a single seamless surface.

Example:

```text
┌─────────────────────────────┐
│ Today                       │
│                             │
│ Amazon             ↓ ₹2,450 │
│ ─────────────────────────── │
│ Swiggy               ↓ ₹540 │
│ ─────────────────────────── │
│ Salary           ↑ ₹85,000 │
└─────────────────────────────┘
```

Do not create:

```text
Card
Card
Card
Card
```

inside another card.

Use:

* One surface
* Internal spacing
* Subtle dividers
* Typography hierarchy

Individual rows should not have separate borders.

Dividers:

```kotlin
outlineVariant.copy(alpha = 0.2f)
```

---

# 17. Surface Philosophy

Follow:

> One surface, one purpose.

Avoid:

```text
Card
 └── Card
      └── Card
           └── Card
```

Prefer:

```text
Screen
 ↓
Section platter
 ↓
Interactive content
 ↓
Content hierarchy
```

Use tonal differences and spacing instead of borders.

---

# 18. Visual Density

Every screen should have one dominant visual focus.

Rules:

* Maximum three major hierarchy levels within a section.
* Avoid excessive cards.
* Avoid excessive pills.
* Do not make every element colorful.
* Secondary metadata should visually recede.
* Important financial numbers dominate.
* Decorative elements must never compete with content.

---

# 19. Buttons

## Primary Button

Characteristics:

* 18dp squircle
* 52–56dp height
* `primary`
* `onPrimary`
* Strong tactile feedback

Press:

```text
scale = 0.96f
```

with immediate light haptic.

Release:

* Spring back to normal scale.

## Tonal Button

Use:

* `surfaceContainerHigh`
* `secondaryContainer`

## Destructive Button

Use:

* `errorContainer`
* `onErrorContainer`

Do not use destructive colors for ordinary actions.

---

# 20. Interaction Hierarchy

Every interaction belongs to one of three levels.

## Primary

Examples:

* Add transaction
* Save
* Confirm
* Main navigation

These receive the strongest:

* Visual emphasis
* Motion
* Haptics

## Secondary

Examples:

* Edit
* Filter
* Sort
* Account selection

## Tertiary

Examples:

* More
* Advanced settings
* Less important actions

Only primary interactions should receive the strongest expressive treatment.

---

# 21. Component State System

Every reusable component should account for:

```text
Default
Pressed
Focused
Selected
Disabled
Loading
Success
Warning
Error
Empty
Partial
```

Do not implement these states differently on every screen.

Create reusable component state APIs.

Example:

```kotlin
VittifyButton(
    state = ButtonState.Loading
)
```

---

# 22. Touch Targets

Minimum interactive target:

```text
48dp × 48dp
```

Prefer:

```text
52dp–56dp
```

for important actions.

Visual size and touch target size may differ.

Small icons should still have an accessible touch area.

---

# 23. Motion System

Motion must feel physical but remain fast.

## Snappy Spring

Use for:

* Buttons
* Toggles
* Tabs
* Indicators

```text
Spring.DampingRatioMediumBouncy
Spring.StiffnessMedium
```

## Bouncy Spring

Use for:

* Card expansion
* Dialog presentation
* Major expressive interactions

```text
Spring.DampingRatioLowBouncy
Spring.StiffnessLow
```

## Smooth Spring

Use for:

* Sheet movement
* Scroll settling
* Layout transitions

```text
Spring.DampingRatioNoBouncy
Spring.StiffnessLow
```

---

# 24. Motion Principles

Follow:

> Motion should explain causality.

Motion should answer:

* What changed?
* Why did it change?
* Where did it come from?
* Where did it go?

Rules:

* Motion must be interruptible.
* Motion must not block interaction.
* Avoid unnecessary animation.
* Do not animate unrelated components together.
* Related elements should share a motion origin.
* Prefer short perceived transitions.
* Use spring physics for tactile interaction.
* Use calmer transitions for informational changes.

---

# 25. Motion Choreography

Use this conceptual sequence:

```text
User action
    ↓
Immediate tactile feedback
    ↓
State change
    ↓
Visual transition
    ↓
Settling
```

Haptic feedback should happen at the moment of interaction, not after an animation finishes.

---

# 26. Reduced Motion

Respect system reduced-motion preferences.

When reduced motion is enabled:

* Remove bounce.
* Reduce scale transformations.
* Reduce morphing.
* Reduce large translations.
* Prefer subtle fades/crossfades.
* Preserve state changes.
* Never remove necessary feedback entirely.

---

# 27. Haptics

### Light

Use for:

* Button taps
* Tab changes
* Selection

### Double tick

Use for:

* Successful save
* Completed important action

### Warning feedback

Use for:

* Delete
* Threshold breach
* Important warning

Do not overuse haptics.

Haptics should reinforce meaning, not become constant noise.

---

# 28. Swipe Actions

Transaction rows support:

### Swipe Left

Reveal:

```text
Delete
```

Use:

* Soft coral
* Tactile threshold haptic

### Swipe Right

Reveal:

```text
Duplicate / Edit
```

Use:

* Emerald / primary
* Tactile feedback

Swipe actions must never be the only way to perform important actions.

---

# 29. Loading States

Prefer structural skeletons when the content layout is known.

Example:

```text
Transaction platter
 ├── skeleton icon
 ├── skeleton merchant
 └── skeleton amount
```

Avoid generic:

```text
Loading...
◌
```

for content that has a predictable structure.

Use the expressive morphing loader for:

* Whole-screen loading
* Longer operations
* Major transitions

Do not use elaborate loaders for tiny operations.

---

# 30. Success / Feedback

Success should feel satisfying but brief.

Example:

```text
Transaction saved
```

Use:

* Small expressive motion
* Haptic confirmation
* Clear visual acknowledgment

Do not use full-screen celebration for ordinary actions.

---

# 31. Empty States

Empty states should be useful and human.

Avoid:

```text
No records found.
```

Prefer:

```text
No transactions yet

Your spending story starts here.

[ + Add transaction ]
```

Vittify's writing style should feel:

* Calm
* Friendly
* Concise
* Helpful
* Human

Avoid technical/database terminology.

---

# 32. Product Voice

Vittify speaks like:

> A calm, helpful financial companion.

Not:

> A database.

Prefer:

```text
Your spending story starts here.
```

over:

```text
No records found.
```

Prefer:

```text
You're all caught up.
```

over:

```text
No pending transactions.
```

Avoid:

* Excessive exclamation marks
* Fear-based language
* Financial jargon where unnecessary
* Technical error messages
* Overly childish copy

---

# 33. Accessibility

Vittify targets:

**WCAG AA principles and strong Android accessibility practices.**

Requirements:

* Minimum 48dp touch targets
* Accessible labels
* Correct TalkBack semantics
* Dynamic font scaling
* Strong contrast
* No color-only communication
* Logical focus order
* Meaningful content descriptions
* Decorative icons excluded from accessibility tree

TalkBack order should follow the user's visual reading order.

---

# 34. Navigation & Screen Scaffolding

Support two navigation bar modes.

### Normal

Edge-to-edge navigation.

### Floating Footer Nav

Floating navigation footer.

Floating elements must never collide with:

* Navigation
* FAB
* Content
* Bottom sheets
* System gesture areas

---

# 35. Top App Bar

The Couple / View Mode toggle:

```text
Both
Me
Partner
```

belongs in the top app bar beside the avatar.

Do not place it in the bottom navigation.

---

# 36. Refresh

Manual synchronization uses:

**Pull-to-refresh**

Do not add a permanent refresh button unless there is a strong contextual reason.

---

# 37. Primary Action

Use a prominent expressive FAB:

```text
+
```

Characteristics:

* Squircle geometry
* Strong semantic emphasis
* Spring compression
* Haptic feedback

The FAB must not compete with the main content's hero element.

---

# 38. Home Screen Architecture

Home is driven by:

```kotlin
HomeWidget
```

Available widgets:

```text
QUICK_ADD
NETWORTH_SUMMARY
ACCOUNT_CAROUSEL
UPCOMING_SUBSCRIPTIONS
RECENT_TRANSACTIONS
BUDGET_CAROUSEL
TRANSACTION_HEATMAP
SHORTCUTS
```

Every widget is a self-contained:

```text
28dp squircle platter
```

---

# 39. Quick Add

Quick Add is intentionally simple.

Collapsed:

```text
✨ Type a transaction in plain English...
```

When focused:

* Expand with spring animation
* Reveal additional controls progressively
* Maintain strong monetary hierarchy
* Keep the save action accessible

Do not overwhelm the user with a large form immediately.

---

# 40. Add Transaction Screen

Hero amount:

* Large
* Dominant
* Rolling number animation
* 24dp squircle container

Inputs:

* Filled containers
* `surfaceContainerHigh`
* `surfaceContainerLow`
* Leading icons where useful
* Clear actions

Save CTA:

```text
56dp
```

anchored appropriately near the bottom.

---

# 41. Bottom Sheets

Use:

```text
32dp
```

top corners.

Include:

```text
32dp × 4dp
```

drag handle.

When blur is supported/enabled:

* Use `hazeEffect`
* Keep content readable
* Preserve contrast

Use:

* Sticky title
* Expressive close pill
* Clear primary action

---

# 42. Dialogs

Use:

```text
28dp squircle
```

with:

* Semantic badge icon
* Clear title
* Concise explanation
* Primary action
* Secondary/cancel action

Avoid unnecessary confirmation dialogs.

Do not ask users to confirm routine, reversible actions.

---

# 43. Iconography

Use an expressive hybrid model.

## Categories

Use:

* Warm 3D illustrations
* Friendly colorful artwork
* 44dp squircle badges
* Soft tonal backgrounds

Provide vector fallbacks for custom categories.

## System / Navigation

Use:

* Material Symbols
* Iconax vectors

Icons should be optically consistent.

Do not mix unrelated icon families inside the same component.

---

# 44. Icon Rules

* Same row → same optical icon size
* Same component → same icon family
* Do not use icons just to fill empty space
* Prefer familiar system symbols
* Icon + text spacing must be consistent
* Decorative icons should not clutter accessibility output

---

# 45. Screen Composition

Every screen should communicate one visual sentence.

## Home

```text
How am I doing?
    ↓
Net worth
    ↓
Accounts
    ↓
Recent activity
    ↓
What needs attention?
```

## Transactions

```text
What happened?
    ↓
Filters
    ↓
Timeline
    ↓
Transaction details
```

## Settings

```text
What can I control?
    ↓
Grouped preferences
    ↓
Appearance / account / privacy
```

A screen should not feel like a collection of unrelated components.

---

# 46. Responsive / Adaptive Design

Support:

* Compact phones
* Large phones
* Foldables
* Tablets
* Expanded window sizes

Conceptual layout:

```text
Compact
    1 column

Medium
    1 column
    wider content

Expanded
    Navigation rail
    Multi-column dashboard
```

Components should expand their content area rather than simply increasing visual density.

Do not stretch cards unnecessarily.

---

# 47. UX Consistency Rules

Every screen must answer:

1. What is the user looking at?
2. What is most important?
3. What can the user do?
4. What happened after they acted?

A user should never need to guess where an important action is located.

Use predictable placement for:

* Back
* Save
* Close
* Search
* Filter
* Add
* Delete
* Confirm

---

# 48. One Primary CTA Rule

A screen should normally have:

```text
ONE dominant primary action
```

Secondary actions should be visually subordinate.

Do not create multiple competing filled buttons.

---

# 49. Progressive Disclosure

Do not show every option immediately.

Prefer:

```text
Simple
 ↓
Relevant detail
 ↓
Advanced options
```

The default experience should remain lightweight.

Advanced controls can appear:

* In bottom sheets
* Expandable sections
* Secondary menus
* Contextual actions

---

# 50. Gestures

Gestures should enhance the UI, not hide it.

Never make an important operation gesture-only.

Every important gesture action should have an accessible alternative.

Examples:

* Swipe → visible menu alternative
* Pull-to-refresh → understandable loading state
* Long press → contextual action menu

---

# 51. Error UX

Errors should be:

* Clear
* Calm
* Actionable
* Human

Prefer:

```text
We couldn't read this message yet.

Try scanning again.
```

over:

```text
ParserException: SMS_PARSE_FAILED
```

Do not expose implementation details to users.

Developer diagnostics belong in logs.

---

# 52. Security & Sensitive Information

Financial and security-related states should use calmer motion.

Do not use playful celebration for:

* Suspicious activity
* Security warnings
* Failed authentication
* Sensitive financial errors

Important warnings should prioritize clarity over delight.

---

# 53. Design Anti-Patterns

DO NOT:

* Nest squircle cards unnecessarily.
* Use borders to separate ordinary content.
* Use more than one dominant CTA.
* Use semantic colors decoratively.
* Animate every state change.
* Use bounce for serious/security/error states.
* Put controls in unexpected locations.
* Hide important financial information behind gestures.
* Require users to remember information between screens.
* Use icon-only controls when the meaning is unclear.
* Create one-off spacing values.
* Create one-off typography values.
* Duplicate components that should be shared.
* Put business logic inside composables.
* Use generic spinners when structural skeletons are possible.
* Use destructive colors for normal actions.
* Overuse pills and chips.
* Put cards inside cards without a strong hierarchy reason.
* Make every screen colorful.
* Make every interaction bouncy.
* Add decoration that competes with financial information.
* Make users confirm routine reversible actions unnecessarily.
* Sacrifice accessibility for visual styling.

---

# 54. Expressiveness Rules

Expressiveness should be concentrated in:

* Hero balances
* Quick Add
* Primary CTA
* Important state transitions
* Empty states
* Success feedback
* Category illustrations
* Navigation transitions

Expressiveness should be restrained in:

* Transaction rows
* Metadata
* Settings
* Secondary controls
* Technical information

---

# 55. Vittify UI Law

This is the highest-level design rule.

> ## Clear first. Delightful second.
>
> Every interaction should be immediately understandable before it becomes expressive.
>
> **Hierarchy over decoration.**
> **Meaning over color.**
> **Motion over static ornament.**
> **Tonal surfaces over borders.**
> **Consistency over novelty.**
> **Accessibility over aesthetics.**

When the design specification does not explicitly define something, use this principle to make the decision.

---

# 56. Definition of Done

A feature is not complete until:

### Architecture

* Business logic is outside composables.
* State flows through ViewModel → UI.
* Domain logic remains testable.
* Data access remains in the data layer.

### UI

* Existing design tokens are used.
* No arbitrary colors.
* No arbitrary spacing.
* No unnecessary borders.
* Correct squircle geometry.
* Correct surface hierarchy.
* Correct typography hierarchy.
* Correct component states.

### UX

* Primary action is obvious.
* Loading state is intentional.
* Empty state is useful.
* Errors are actionable.
* Important information is immediately visible.
* Gestures have accessible alternatives.

### Motion

* Motion explains the state change.
* Animation does not block interaction.
* Haptics are meaningful.
* Reduced-motion behavior is supported.

### Accessibility

* Touch targets are at least 48dp.
* TalkBack order is logical.
* Content descriptions are meaningful.
* Decorative elements are excluded where appropriate.
* Color is never the sole communication mechanism.
* Dynamic text scaling works correctly.
* Contrast remains accessible.

### Financial UX

* Amount is unmistakable.
* Transaction direction is clear.
* Currency formatting is correct.
* Semantic colors are used consistently.
* Serious financial/security states are calm and clear.

### Engineering

* No duplicated components.
* No unnecessary abstractions.
* No business logic in UI.
* automatic debug Gradle builds.
* automatic ADB installation.

---

# 57. Developer Workflow Rules

When implementing UI:

1. Inspect existing components first.
2. Reuse existing design tokens.
3. Reuse existing components.
4. Only create a new component when there is a real reusable concept.
5. Keep composables focused.
6. Keep business logic outside composables.
7. Preserve existing architecture.
8. Make the smallest reasonable change.
9. Check accessibility.
10. Check loading, empty, error, and disabled states.
11. Check reduced motion.
13. build debug apk once changes are done.
14.  after building  install the debug apk in all the devices.
---

# 58. Final Design Standard

Every new Vittify screen or component should feel like it belongs to the same product without requiring the developer to invent new styling.

Before implementing a new component, ask:

```text
Does an existing component already solve this?
        ↓
Can existing tokens express it?
        ↓
Can the interaction be simpler?
        ↓
Is the hierarchy obvious?
        ↓
Is the financial meaning clear?
        ↓
Is the interaction accessible?
        ↓
Does motion explain what happened?
        ↓
Is the expressiveness purposeful?
```

If the answer is yes to all of the above, the component is ready to implement.

**Vittify should feel calm at rest, expressive in interaction, and exceptionally clear when money is involved.**

before implementing any UI images, generate the UI image get feedback and then do the changes
