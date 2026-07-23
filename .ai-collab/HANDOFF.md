# Handoff Log

## 2026-07-22 Codex

- Goal: create a standalone, company-facing personal portfolio that can grow beyond the original experiment repository.
- Changed: created `D:\Projects\signaltrace-portfolio`, migrated selected web/process/Android assets, renamed the public identity to SignalTrace Portfolio, and established `content/portfolio.json` as the single content source.
- Changed: added web content publishing, validation, Android startup/periodic HTTPS sync, last-known-good offline cache, and APK version-manifest support.
- Safety: network content is HTTPS-only, size-capped, structure-validated, and never replaces cached content after a failed request.
- Open: final public URL, real-device runtime verification, approved personal identity/contact data, and future GLB/media assets.
- Next: finish builds and browser QA, then provide deployment and Android configuration handoff.

## 2026-07-22 Codex

- Goal: finish the first company-reviewable standalone release foundation.
- Verified: `npm run build` passes, including content sync, 32-node/32-edge process validation, portfolio validation, TypeScript, and Vite production output.
- Verified: Android `assembleDebug` passes; APK package is `com.signaltrace.portfolio`, version `0.2.0` / code 2, API 26 minimum. Manifest contains INTERNET, network-state, boot persistence, and `PortfolioSyncJobService`.
- Verified: source, web-public, Android-embedded, and built `portfolio.json` files have the same SHA-256. APK SHA-256 is `6B6715FAB13496153699C4CB6085AA885150A39C779A095E6A5077BC839A919A`.
- Verified: desktop/mobile browser QA passes for company positioning, project cards, core-project navigation, studio navigation, responsive containment, and zero console warnings/errors. Screenshots are under `D:\CodexTests\signaltrace-portfolio`.
- Open: the expected GitHub Pages endpoint currently returns HTTP 404 because the remote repository/Pages deployment has not been created. Android safely falls back to embedded/cached content until it becomes HTTP 200.
- Next: follow `docs/GITHUB_PAGES_SETUP.md`, then verify both update endpoints before distributing the APK.

## 2026-07-22 23:49 Codex

- Goal: correct the product narrative from a recruiter-first portfolio to a personal internship achievement showcase for leaders, enterprise readers, relatives, and friends.
- Changed: rewrote the homepage story, outcomes, learning method, growth evidence, and audience framing; kept SignalTrace as the technical project identity rather than the main headline.
- Changed: updated the unified portfolio content, Android top-bar identity, README, reading guide, and durable collaboration memory.
- Verified: web build and Android `assembleDebug` pass; content version is `2026.07.2`; all three published/embedded content copies share SHA-256 `A2E4A432B9947EBFDC024CFC342D28642775C17E111CE5FC882C314508BD0791`.
- Verified: desktop (1280px) and mobile (375px) browser layouts have no horizontal overflow or overlap; the desktop hero title is a single line, core-project and studio navigation work, and page logs contain no warnings or errors.
- Open: GitHub repository/Pages deployment and real-device Android testing remain pending; approved identity, photos, and later model assets are still user-supplied.
- Next: add the user's first approved internship-learning module to `content/portfolio.json`, then publish when explicitly approved.

## 2026-07-23 00:32 Codex

- Goal: make the portfolio materially reflect the user's actual internship, add a product-management project, and redesign Web/Android with an original industrial tactical visual system.
- Data: read 48 Miaoda records in memory, selected 15 work-learning events, excluded personal diet/fitness and duplicates, and removed exact unit identifiers and sensitive numeric details before publication.
- Changed: content version `2026.07.3` now includes 4 works, 15 structured journal entries, 1 FieldTrace product case, and 3 model slots; raw API data and credentials were not written to the repository.
- Changed: added `#product`, a complete PM case with real evidence, problem definition, users, solution flow, MVP priorities, non-goals, and metrics; added interactive date filtering for all 15 logs.
- Changed: rebuilt homepage, product case, process lab, content studio, and Android presentation with angular solid-color planes, offset depth, high-contrast hierarchy, and faux-3D prototypes. Added old-draft migration and a versioned studio storage key.
- Verified: `npm run build` passes; Android `assembleDebug` passes at version `0.3.0` / code 3. Desktop 1280px and mobile 375px Web QA show no overflow or overlap; route transitions, journal filtering, product navigation, lab, studio, and clean-console checks pass.
- Hashes: portfolio content SHA-256 is `92B729543B8B632247B07446959B64CBEF217E2C5096E31C29159774712402F2`; APK SHA-256 is `B7BC7844A785B47F006C994D1D8A8E0BC17A3B112BBDFAB3AFAB22F44A135394`.
- Open: no Android device or AVD was available, so native runtime visual QA remains pending. GitHub repository/Pages deployment is still not created, so network update endpoints remain unavailable.
- Security: the Miaoda token appears in an earlier pasted conversation attachment; it should be rotated and kept only in private Claude/local configuration.
