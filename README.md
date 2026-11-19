# GitHub Infinite Scroller

An Android app for endlessly browsing GitHub repositories by the topics you care about.

## Features

- **Topic tabs.** Pick topics during onboarding (or type any GitHub topic). Each one becomes a chip in the feed, next to a **Popular** feed.
- **Infinite scroll.** Paging 3 loads 30 repos at a time, up to GitHub's 1,000-result cap per search. Duplicate results across pages are dropped.
- **Sort and search.** Most stars, Rising this month, Recently updated, Most forks, plus free-text search within the current topic.
- **Repo details.** A bottom sheet with stats, topics, license, dates, and buttons to open on GitHub, visit the website, save or share.
- **Saved.** Bookmark repos to a Saved tab that works offline, with undo.
- **Clear errors.** Distinct messages for offline, rate limited (with a countdown and automatic retry), bad token, and invalid query.
- **Optional token.** Add a GitHub personal access token in Settings to raise the search limit from 10 to 30 requests a minute.
- **Theming.** Light, dark or system theme, an indigo brand palette, and optional Material You dynamic color.

## Why one topic per request

GitHub's search API rejects boolean operators between qualifiers. A query like `topic:android OR topic:kotlin` returns
HTTP 422 ("Logical operators only apply to text, not to qualifiers"). v1.0 built exactly that query for multiple tags,
so the first request always failed. The feed now sends a single `topic:` qualifier per request and lets you switch topics
with the chip row. `QueryBuilderTest` guards against this coming back.

## Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Paging | Paging 3 |
| Networking | Retrofit + OkHttp (with HTTP cache) + Moshi |
| Images | Coil |
| Persistence | DataStore Preferences (settings, bookmarks as JSON) |
| Architecture | MVVM, manual DI via `AppContainer` |

## Project structure

```
app/src/main/java/com/github/infinitescroller/
├── AppContainer.kt         # dependency container + Application class
├── MainActivity.kt         # theme + system bars, hosts navigation
├── data/
│   ├── api/                # Retrofit API, OkHttp client, typed GitHub errors
│   ├── model/              # Repo, Owner, License, SearchResponse
│   ├── paging/             # SearchPagingSource
│   ├── preferences/        # settings, bookmarks, curated topics
│   └── repository/         # QueryBuilder, RepoRepository
└── ui/
    ├── components/         # RepoCard, detail sheet, empty/error/skeleton states
    ├── explore/  saved/  settings/  topics/
    ├── navigation/         # NavHost + bottom navigation
    ├── theme/
    └── util/               # number/date formatting, error messages, language colors
```

## Building

The repo has no Gradle wrapper script. Use Gradle 8.9+ and JDK 21 with an Android SDK (platform 35, build-tools 35.0.0):

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

## CI and releases

GitHub Actions runs unit tests on every push and pull request. Pushing a `v*` tag also builds the debug APK and publishes
it to a GitHub release (see `.github/workflows/ci.yml`).
