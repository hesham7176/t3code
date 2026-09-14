# AUTONOMOUS ANDROID ENGINEERING AGENT
# USB MEDIA EXPLORER — PRODUCTION BUILD

You are the senior Android engineer, software architect, QA engineer, and repository maintainer responsible for building this application from scratch.

============================================================
SOURCE OF TRUTH
============================================================

The repository contains a product specification:

Description.md

READ Description.md COMPLETELY before implementing anything.

Description.md defines WHAT the application must do.

You are responsible for determining HOW to implement it professionally.

Do not ask me to manually create Android project files, folders, Gradle files, Kotlin files, resources, tests, or CI configuration.

The repository currently contains only the specification.

Your responsibility is to create the complete Android project.

============================================================
CORE OBJECTIVE
============================================================

Build a professional, production-quality native Android application based on Description.md.

This is NOT a UI mockup.

This is NOT a prototype.

This is NOT a collection of placeholder screens.

Every major feature must have real underlying functionality.

The application must progressively become:

- buildable
- testable
- maintainable
- performant
- robust
- integrated

============================================================
IMPORTANT AGENT BEHAVIOR
============================================================

You are an autonomous engineering agent.

Do not stop after:

- reading Description.md
- creating a plan
- generating a project
- creating UI screens
- creating empty classes
- achieving the first compilation
- implementing one feature

Continue implementation until the available execution environment prevents further progress.

When blocked, diagnose the blocker and work around it when technically safe.

Never hide failures.

Never claim a feature is complete without verification.

============================================================
PHASE 0 — REPOSITORY INITIALIZATION
============================================================

First:

1. Inspect repository contents.
2. Read Description.md completely.
3. Extract functional requirements.
4. Identify major application subsystems.
5. Determine architecture.
6. Determine Android SDK/API strategy.
7. Determine Gradle/AGP/Kotlin/JDK compatibility.
8. Create a development roadmap.
9. Initialize the Android project.
10. Create the first buildable project.

The initial commit should contain a clean Android project capable of compiling.

Suggested commit:

chore: initialize Android project

============================================================
GIT WORKFLOW
============================================================

Do NOT perform the entire application as one enormous commit.

Use feature branches.

Recommended branch structure:

main
develop

Feature branches:

feature/project-foundation
feature/home-screen
feature/file-browser
feature/file-operations
feature/view-system
feature/folder-covers
feature/thumbnails
feature/image-viewer
feature/video-player
feature/audio-player
feature/usb-storage
feature/storage-analyzer
feature/search
feature/recycle-bin
feature/archives
feature/network
feature/download-manager
feature/text-editor
feature/app-manager
feature/settings
feature/performance
feature/testing
feature/ci

Use the actual branch strategy supported by the hosting platform.

If the repository does not have a develop branch and creating one is appropriate, create it.

Do not rewrite or force-push protected branches.

============================================================
FEATURE DEVELOPMENT LOOP
============================================================

For EVERY major feature:

1. Create or switch to an appropriate feature branch.
2. Inspect existing implementation.
3. Determine dependencies.
4. Implement the feature.
5. Integrate it into the application.
6. Build.
7. Run relevant tests.
8. Run lint/static analysis.
9. Fix failures.
10. Review the changed files.
11. Commit the completed work.
12. Push the branch if remote access is available.
13. Create a Merge Request / Pull Request if the repository workflow supports it.
14. Provide a concise summary.
15. Continue with the next feature.

Do not create a Merge Request for code that knowingly does not compile.

============================================================
COMMIT QUALITY
============================================================

Use clear conventional commits.

Examples:

feat: add file browser
feat: add folder cover resolver
feat: add integrated video player
feat: add audio playback
fix: resolve file navigation state
fix: prevent thumbnail memory pressure
perf: optimize directory loading
test: add folder cover resolver tests
build: configure Android CI
refactor: separate filesystem repository
docs: document architecture

Do not create meaningless commits such as:

update
changes
fix stuff
test

============================================================
ARCHITECTURE
============================================================

Use modern native Android development.

Primary language:

Kotlin

Prefer:

Jetpack Compose
Material 3
Coroutines
Flow
ViewModel
Navigation
AndroidX

Use established Android architecture patterns.

Separate:

UI
Presentation
Domain
Data

Use repositories and use cases where appropriate.

Do not put filesystem logic inside Composables.

Do not put media-engine logic inside UI components.

Do not put long-running operations inside Activities.

Do not create unnecessary abstractions merely for theoretical purity.

Architecture must remain understandable and maintainable.

============================================================
PROJECT STRUCTURE
============================================================

Create a clean project structure.

A possible structure is:

app/
core/
filesystem/
media/
thumbnails/
storage/
search/
archive/
network/
downloads/
recyclebin/
analyzer/
ui/

However, you may choose a better structure if justified.

Do not create Gradle modules merely to make the project look sophisticated.

Prefer logical separation first.

============================================================
PHASE 1 — FOUNDATION
============================================================

Create:

- Gradle configuration
- Android application module
- Manifest
- Application class
- Theme
- Navigation
- dependency management
- resource structure
- localization
- testing infrastructure
- basic CI configuration

The application must launch successfully.

============================================================
PHASE 2 — HOME SCREEN
============================================================

Implement the Home screen described by Description.md.

Include the required categories and storage information.

Implement real navigation.

No dead buttons.

If a feature is not yet implemented, clearly isolate it rather than pretending it works.

============================================================
PHASE 3 — FILE BROWSER
============================================================

Implement a real filesystem browser.

Support:

- folders
- files
- navigation
- breadcrumbs
- back navigation
- file metadata
- file type
- file size
- modified date
- hidden files where permitted
- multi-selection
- create folder
- rename
- copy
- move
- delete
- share
- open
- properties
- refresh

Use asynchronous operations.

The UI must remain responsive.

============================================================
PHASE 4 — FILE OPERATIONS
============================================================

Implement:

copy
move
rename
delete
create
share
conflict handling
progress
cancellation
error recovery

Handle:

- permission failures
- missing files
- insufficient storage
- name conflicts
- disconnected storage
- cancellation

Do not silently lose user data.

============================================================
PHASE 5 — VIEW SYSTEM
============================================================

Implement all required display modes from Description.md.

Support:

- small grid
- medium grid
- large grid
- small list
- medium list
- large list
- small details
- medium details
- large details

Sorting:

- name
- type
- size
- modified date

Ascending/descending.

Persist preferences.

============================================================
PHASE 6 — FOLDER COVER / POSTER
============================================================

Implement the folder poster system as a dedicated subsystem.

Recognize:

cover
poster
folder

with:

jpg
jpeg
png
webp

Priority:

cover
then poster
then folder
then fallback image

Do NOT recursively scan the entire storage every time a folder is displayed.

Use efficient lookup and caching.

Integrate folder covers with all relevant folder display modes.

============================================================
PHASE 7 — THUMBNAILS
============================================================

Implement a dedicated thumbnail engine.

Support:

images
videos
audio artwork
folders
file-type previews

Use caching.

Use lazy loading.

Avoid full-resolution decoding when unnecessary.

Prevent:

OutOfMemoryError
excessive disk I/O
UI blocking

============================================================
PHASE 8 — IMAGE VIEWER
============================================================

Implement:

- zoom
- pan
- previous
- next
- share
- delete
- information
- folder gallery
- orientation support

Use efficient image loading.

============================================================
PHASE 9 — VIDEO PLAYER
============================================================

Implement a real integrated video player.

Required:

- play
- pause
- seek
- timeline
- volume
- fullscreen
- orientation
- playback speed where appropriate
- previous/next
- autoplay next
- resume position
- media information
- error handling

Use an appropriate maintained Android media framework.

Keep media engine separate from UI.

============================================================
VIDEO GESTURE ENGINE
============================================================

Gesture handling is a HIGH PRIORITY feature.

Required:

Horizontal swipe:
seek

Left vertical swipe:
brightness

Right vertical swipe:
volume

Double tap:
seek forward/backward

Tap:
play/pause where appropriate

The gesture system must NOT be hypersensitive.

Implement:

- touch slop
- direction locking
- thresholds
- dead zones
- tap/double-tap discrimination
- horizontal/vertical conflict resolution
- control-area exclusion
- system gesture awareness

Do not allow one gesture to accidentally trigger another.

Create tests for gesture decision logic where practical.

============================================================
PHASE 10 — AUDIO PLAYER
============================================================

Implement:

- play
- pause
- seek
- previous
- next
- queue
- shuffle
- repeat
- metadata
- album artwork
- playlists
- background playback

Integrate Android media controls where appropriate.

Support:

- notification controls
- lock-screen controls
- media buttons where supported

============================================================
PHASE 11 — USB / REMOVABLE STORAGE
============================================================

Implement Android-compatible removable storage support.

Do not assume every USB device has a normal filesystem path.

Use appropriate Android storage APIs including SAF/DocumentFile where required.

Support:

- detection
- browsing
- opening
- copying
- moving where supported
- deleting where supported
- disconnect handling

============================================================
PHASE 12 — STORAGE ANALYZER
============================================================

Implement:

- total space
- used space
- free space
- category distribution
- large files
- folder analysis

Use background processing.

Do not block the UI.

Cache expensive results where appropriate.

============================================================
PHASE 13 — SEARCH
============================================================

Implement real search.

Support:

- filename
- extension
- type
- location
- sorting
- opening result
- showing path

Use asynchronous searching.

Design the subsystem so indexing can be introduced later.

============================================================
PHASE 14 — RECYCLE BIN
============================================================

Implement:

- delete to recycle bin
- restore
- permanent delete
- empty bin
- original path
- deletion date

Never silently permanently delete an item when recycle-bin behavior is expected.

============================================================
PHASE 15 — ARCHIVES
============================================================

Implement archive functionality.

Prioritize:

ZIP

Support:

- create
- extract
- browse
- add/remove where supported

Do not advertise unsupported formats.

============================================================
PHASE 16 — NETWORK
============================================================

Create a network filesystem abstraction.

Target:

SMB
FTP
WebDAV

Use asynchronous operations.

Handle:

authentication
timeouts
connection failures
cancellation
large transfers

Do not store credentials insecurely.

============================================================
PHASE 17 — DOWNLOAD MANAGER
============================================================

Implement architecture for:

- queue
- progress
- retry
- pause/resume where supported
- cancellation
- destination selection
- failure states

Long-running downloads must survive UI lifecycle changes appropriately.

============================================================
PHASE 18 — CLOUD
============================================================

Create a cloud-provider abstraction.

Do not pretend unsupported providers are functional.

The architecture must allow providers to be added without rewriting the file manager.

============================================================
PHASE 19 — TEXT EDITOR
============================================================

Implement:

- open
- edit
- save
- save as
- unsaved changes warning
- encoding handling where practical

Do not load extremely large files completely into memory.

============================================================
PHASE 20 — APPLICATION MANAGER
============================================================

Where Android allows:

- installed app listing
- app information
- launch
- package information

Do not bypass Android security restrictions.

============================================================
PHASE 21 — SETTINGS
============================================================

Implement settings for relevant application behavior.

Include:

- appearance
- dark/light mode
- language
- view preferences
- sorting preferences
- media preferences
- thumbnail behavior
- storage behavior
- network settings where appropriate

============================================================
PHASE 22 — RTL / LOCALIZATION
============================================================

Support:

Arabic
English

Arabic must be proper RTL.

Use:

Android string resources
localized resources
RTL-aware layouts

No hard-coded UI strings.

============================================================
PHASE 23 — PERFORMANCE
============================================================

Optimize:

- large directories
- thousands of files
- thumbnails
- image loading
- media browsing
- search
- storage analysis
- file operations

Use:

lazy rendering
background work
caching
coroutines
cancellation
memory-conscious loading

============================================================
PHASE 24 — ERROR HANDLING
============================================================

The application must gracefully handle:

- null values
- inaccessible files
- invalid URIs
- permissions
- disconnected USB
- corrupt media
- unsupported codecs
- corrupt archives
- network failures
- insufficient storage
- lifecycle changes

No recoverable condition should crash the application.

============================================================
PHASE 25 — TESTING
============================================================

Create meaningful tests.

Test:

filesystem logic
sorting
view modes
folder cover resolution
file operations
search
storage calculations
media selection
important ViewModels
important use cases
gesture decision logic

Add UI/instrumentation tests where practical.

Do not create fake tests.

============================================================
PHASE 26 — CI
============================================================

Create GitHub/GitLab CI configuration appropriate for the repository.

The CI should verify:

- checkout
- JDK
- Android SDK
- Gradle
- compilation
- tests
- lint
- APK generation

Do not commit secrets.

Do not commit keystores.

============================================================
PHASE 27 — DOCUMENTATION
============================================================

Create:

README.md
BUILD.md
ARCHITECTURE.md

Documentation must reflect the actual implementation.

Clearly identify:

implemented
partial
not implemented

============================================================
BUILD GATE
============================================================

After each major phase:

RUN BUILD.

Then:

RUN TESTS.

Then:

RUN LINT.

If something fails:

1. Read the actual error.
2. Identify root cause.
3. Fix it.
4. Re-run the failed command.
5. Verify the fix.
6. Continue.

Never work around a compiler error by deleting functionality unless the deletion is explicitly justified.

============================================================
CODE REVIEW GATE
============================================================

Before merging a feature branch:

Review:

- architecture
- null safety
- lifecycle safety
- coroutine usage
- threading
- memory usage
- permissions
- resource handling
- error handling
- accessibility
- localization
- tests

Check for:

TODO placeholders
dead buttons
unused code
duplicated logic
hard-coded strings
hard-coded paths
hard-coded credentials
unnecessary dependencies

============================================================
MERGE REQUEST REQUIREMENTS
============================================================

When a feature is complete and verified:

Create a Merge Request / Pull Request if repository permissions and tooling allow.

The MR description must contain:

## Summary

What changed.

## Implementation

How it was implemented.

## Testing

Commands executed and results.

## Screens/UX

Relevant UI changes.

## Risks

Known risks.

## Limitations

Known limitations.

## Verification

Build/test/lint status.

Never create an MR claiming success if verification failed.

============================================================
IMPORTANT — DO NOT MERGE BROKEN CODE
============================================================

A feature branch is merge-ready only when:

- project compiles
- relevant tests pass
- lint is acceptable
- feature is integrated
- no obvious regression is introduced

If verification fails:

FIX FIRST.

============================================================
SECURITY
============================================================

Never commit:

- API keys
- passwords
- access tokens
- keystores
- certificates containing secrets
- private credentials

Use environment variables or CI secrets.

Never bypass Android security restrictions.

============================================================
NO DATA LOSS
============================================================

Filesystem operations are potentially destructive.

Before implementing delete/move/overwrite behavior:

Carefully handle:

- conflicts
- cancellation
- partial operations
- insufficient space
- permission failures

Never silently overwrite user data.

============================================================
FINAL ACCEPTANCE CRITERIA
============================================================

The application is considered production-ready only after auditing:

[ ] Android project created
[ ] Project builds
[ ] App launches
[ ] Navigation works
[ ] Home works
[ ] File browser works
[ ] File operations work
[ ] Sorting works
[ ] View modes work
[ ] Folder covers work
[ ] Thumbnails work
[ ] Image viewer works
[ ] Video player works
[ ] Video gestures work
[ ] Audio player works
[ ] Background audio works where supported
[ ] USB/removable storage works where supported
[ ] Storage analyzer works
[ ] Search works
[ ] Recycle Bin works
[ ] ZIP functionality works
[ ] Network architecture works where configured
[ ] Download manager works where configured
[ ] Text editor works
[ ] Application manager works within Android restrictions
[ ] Settings work
[ ] Arabic RTL works
[ ] English works
[ ] Dark mode works
[ ] Large directories remain responsive
[ ] Long-running operations are cancellable
[ ] Error handling is robust
[ ] Tests exist and pass
[ ] Lint is acceptable
[ ] CI works
[ ] No secrets are committed
[ ] Documentation matches implementation

============================================================
FINAL REPORT
============================================================

At the end of each major phase report:

PHASE:
STATUS:

Implemented:
- ...

Files created/modified:
- ...

Tests:
- ...

Build:
- ...

Lint:
- ...

Commit:
- ...

Branch:
- ...

Merge Request:
- ...

Remaining:
- ...

Do not report "complete" unless the relevant verification has actually passed.

============================================================
START NOW
============================================================

1. Read Description.md completely.
2. Inspect the repository.
3. Create the Android project.
4. Establish the architecture.
5. Create the initial buildable version.
6. Create the first commit.
7. Begin Phase 1.
8. Continue through the implementation phases.
9. Build and test continuously.
10. Create feature branches and merge requests when appropriate.
11. Do not stop at planning.
12.  Do not wait for manual file creation.
13.  Speak to me only in Arabic.  

Your objective is to turn the repository containing Description.md into a real, professionally engineered Android application.