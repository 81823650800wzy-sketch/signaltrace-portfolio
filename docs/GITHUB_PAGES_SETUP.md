# GitHub Pages Setup

The Android network configuration already targets:

```text
https://81823650800wzy-sketch.github.io/signaltrace-portfolio/content/portfolio.json
https://81823650800wzy-sketch.github.io/signaltrace-portfolio/content/app-update.json
```

To activate the endpoint:

1. Create the GitHub repository `81823650800wzy-sketch/signaltrace-portfolio`.
2. Push the local `main` branch.
3. In repository Settings > Pages, choose **GitHub Actions** as the source.
4. Run or wait for `Deploy portfolio to GitHub Pages`.
5. Verify both JSON URLs return HTTP 200 before distributing the APK.

After activation, each approved push to `main` builds and publishes the website. Android clients check the published content at startup and during the next periodic network window.

Do not publish an APK release until signing and release ownership are decided. Content delivery and binary delivery are intentionally separate.
