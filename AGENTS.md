# SignalTrace AI Collaboration Rules

Before changing this repository, read `README.md`, `.ai-collab/MEMORY.md`, `.ai-collab/TASKS.md`, and `.ai-collab/HANDOFF.md`.

- This repository is a company-facing public portfolio. Keep every statement evidence-bounded.
- Never add raw internship logs, credentials, names, exact plant coordinates, internal tags, DCS screenshots, unpublished parameters, network/control details, or unapproved media.
- Do not describe this project as a production system, live digital twin, or real-time plant integration.
- `content/portfolio.json` is the single public content source. Run `npm run sync:content` after changing it.
- Web and Android network content must remain compatible with the same schema.
- Network updates must use HTTPS, validate structure, cap response size, and preserve last-known-good offline content on failure.
- Generated test artifacts belong under `D:\CodexTests\signaltrace-portfolio`.
- Update `.ai-collab/HANDOFF.md` after meaningful work and `.ai-collab/TASKS.md` when priorities change.
