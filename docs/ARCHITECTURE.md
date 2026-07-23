# Architecture

```text
content/portfolio.json
        |
        +-- npm run sync:content --> public/content/portfolio.json --> static website/CDN
        |
        +-- npm run sync:content --> Android assets/portfolio.json --> first-launch fallback

website/CDN -- HTTPS --> Android startup sync + periodic JobScheduler
                                  |
                                  +-- validate shape and size
                                  +-- save last-known-good content
                                  +-- render offline after network failure
```

The website and Android app consume the same content contract. Process graph and incident data remain separate because they have richer visualization schemas.

Binary updates use `content/app-update.json`. Android compares `versionCode` and opens an HTTPS release page when a newer version exists. The app does not silently install packages.
