# GitHub Infinite Scroller

An Android app that infinitely scrolls trending GitHub repositories filtered by topics you care about.

## Features

- **Infinite scroll** — powered by Paging 3, loads 20 repos at a time up to GitHub's 1 000-result cap
- **Topic filtering** — pick tags (android, kotlin, compose, …) and the feed updates instantly
- **Persistent preferences** — selected tags survive app restarts via DataStore
- **Pull-to-refresh** — swipe down to reload the feed
- **Tap to open** — opens the repo's GitHub page in the browser
- **No auth required** — uses GitHub's public search API (unauthenticated, 10 req/min)

## Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Paging | Paging 3 (`PagingSource`, `Pager`, `cachedIn`) |
| Networking | Retrofit + OkHttp + Moshi |
| Image loading | Coil |
| Persistence | DataStore Preferences |
| Architecture | MVVM + Repository |
| Async | Kotlin Coroutines + Flow |

## Getting started

1. Clone the repo
2. Open in Android Studio Hedgehog or later
3. Run on a device or emulator (minSdk 26)

No API key or configuration needed.

## Project structure

```
app/src/main/java/com/github/infinitescroller/
├── data/
│   ├── api/          # Retrofit service + client
│   ├── model/        # GithubRepo, Owner, SearchResponse
│   ├── paging/       # GithubPagingSource
│   ├── preferences/  # TagStore interface + DataStore implementation
│   └── repository/   # GithubRepository (query building + Pager)
└── ui/
    ├── feed/         # FeedViewModel, FeedScreen, RepoCard
    ├── navigation/   # AppNavigation (NavHost)
    ├── tags/         # TagViewModel, TagScreen
    └── ViewModelFactory.kt
```

## Running tests

```bash
./gradlew :app:testDebugUnitTest
```

Tests cover `TagViewModel`, `GithubRepository.buildQuery`, and `GithubPagingSource` (page keys, error handling, 1 000-result cap).

## CI

GitHub Actions runs unit tests on every push and pull request (see `.github/workflows/ci.yml`).
