package com.github.infinitescroller.testing

import com.github.infinitescroller.data.api.GithubApi
import com.github.infinitescroller.data.model.Owner
import com.github.infinitescroller.data.model.Repo
import com.github.infinitescroller.data.model.SearchResponse
import com.github.infinitescroller.data.preferences.BookmarkRepository
import com.github.infinitescroller.data.preferences.SettingsRepository
import com.github.infinitescroller.data.preferences.ThemeMode
import com.github.infinitescroller.data.preferences.UserSettings
import com.github.infinitescroller.data.repository.SortOption
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.Response

val testMoshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

fun repo(id: Long, stars: Int = 100) = Repo(
    id = id,
    name = "repo$id",
    fullName = "owner/repo$id",
    description = "Description $id",
    htmlUrl = "https://github.com/owner/repo$id",
    stars = stars,
    owner = Owner("owner", "https://example.com/a.png"),
)

/** Records every call and answers with whatever [respond] returns. */
class FakeGithubApi(
    var respond: suspend (query: String, page: Int) -> Response<SearchResponse> = { _, _ ->
        Response.success(SearchResponse(0, emptyList()))
    },
) : GithubApi {
    data class Call(val query: String, val sort: String, val perPage: Int, val page: Int)

    val calls = mutableListOf<Call>()

    override suspend fun searchRepositories(
        query: String,
        sort: String,
        order: String,
        perPage: Int,
        page: Int,
    ): Response<SearchResponse> {
        calls += Call(query, sort, perPage, page)
        return respond(query, page)
    }
}

fun errorResponse(code: Int, body: String, headers: Map<String, String> = emptyMap()): Response<SearchResponse> {
    val raw = okhttp3.Response.Builder()
        .code(code)
        .message("error")
        .protocol(Protocol.HTTP_1_1)
        .request(Request.Builder().url("https://api.github.com/search/repositories").build())
        .apply { headers.forEach { (k, v) -> header(k, v) } }
        .build()
    return Response.error(body.toResponseBody(), raw)
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<UserSettings> = state

    override suspend fun setTopics(topics: List<String>) = state.update { it.copy(topics = topics.distinct()) }
    override suspend fun toggleTopic(topic: String) = state.update {
        it.copy(topics = if (topic in it.topics) it.topics - topic else it.topics + topic)
    }
    override suspend fun setSort(sort: SortOption) = state.update { it.copy(sort = sort) }
    override suspend fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override suspend fun setDynamicColor(enabled: Boolean) = state.update { it.copy(dynamicColor = enabled) }
    override suspend fun setToken(token: String) = state.update { it.copy(token = token.trim()) }
    override suspend fun setOnboardingDone() = state.update { it.copy(onboardingDone = true) }
}

class FakeBookmarkRepository : BookmarkRepository {
    val state = MutableStateFlow<List<Repo>>(emptyList())
    override val bookmarks: Flow<List<Repo>> = state
    override suspend fun toggle(repo: Repo) = state.update { list ->
        if (list.any { it.id == repo.id }) list.filterNot { it.id == repo.id } else listOf(repo) + list
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
