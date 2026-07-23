# Content Release

1. Add only verified, publishable material to `content/portfolio.json`.
2. Increment `meta.version` and update `meta.updatedAt`.
3. Run `npm run build`; this synchronizes and validates web/Android copies.
4. Review `/`, `#lab`, and `#studio` locally.
5. Deploy `dist`, including `dist/content/portfolio.json` and `dist/content/app-update.json`.
6. Android clients receive content at startup or during the next scheduled network window.

For a new APK release, update Android `versionCode`/`versionName`, publish the APK or release page, then update `content/app-update.json`. Set `apkUrl` or `releasePageUrl` to HTTPS before deployment.

Do not publish raw diary data. The private diary remains an evidence source; only reviewed and sanitized conclusions enter this repository.
