# Shared Memory

## Project

- Name: SignalTrace Portfolio.
- Path: `D:\Projects\signaltrace-portfolio`.
- Audience: internship leaders, enterprise readers, relatives and friends, plus future collaborators.
- Positioning: a personal internship achievement showcase first, with professional evidence for enterprise readers; it is not primarily a job-application landing page.
- Core story: observe a field signal, trace it through the system, verify evidence, and turn the result into a reusable product artifact.
- Current evidence set: 15 sanitized work-learning events from 2026-07-15 through 2026-07-22, 4 public outputs, and 1 product-management case.
- Product case: FieldTrace instrumentation patrol cognitive map, derived from actual device-location, trend-comparison, recurring-fault, and differential-diagnosis observations.
- Visual direction: original industrial tactical UI using angular solid-color planes, offset shadows, and faux-3D layers; it may reference genre-level composition but must not copy third-party game assets or branding.

## Product Surfaces

- `/`: personal instrumentation-internship story and achievement overview for leaders, companies, and relatives.
- `#lab`: 32-node process-learning and incident-review prototype.
- `#studio`: local-only content editing and preview.
- `android-app`: offline portfolio app with HTTPS content updates and version notifications.

## Durable Decisions

- `content/portfolio.json` is the single public content source.
- Web publishing exposes `/content/portfolio.json` and `/content/app-update.json`.
- Android keeps embedded content, last-known-good network content, and user-imported content as fallback layers.
- Content updates can apply automatically. APK updates require user/store confirmation and cannot be silently installed.
- Existing private diary data stays outside this repository and is never automatically synchronized.
- Raw Miaoda logs are never stored in the repository; only manually reviewed, sanitized work-learning entries enter `content/portfolio.json`.

## Storage and Privacy

- Active project and tests use `D:\`.
- `E:\` is a USB drive and `F:\` is an external drive; neither is a default working directory.
- Public data is limited to public sources, synthetic data, sanitized observations, and approved media.
