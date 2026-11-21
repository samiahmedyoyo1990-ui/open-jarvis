package com.example.github

import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class GitHubApiClient(
    private val tokenProvider: () -> String? = { null }
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        const val BASE_URL = "https://api.github.com"
        const val RAW_BASE_URL = "https://raw.githubusercontent.com"

        private val OWNER_REGEX = Regex("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$")
        private val REPO_REGEX = Regex("^[A-Za-z0-9._-]{1,100}$")
        private val BRANCH_REGEX = Regex("^[A-Za-z0-9._/-]{1,120}$")

        /**
         * Parses & strictly validates GitHub repository URLs or owner/repo shorthand.
         */
        fun parseUrl(input: String): ParsedRepoInfo? {
            val trimmed = input.trim()
            if (trimmed.isBlank() || trimmed.contains('@') || trimmed.any { it.isWhitespace() }) return null
            if (trimmed.contains("..") || trimmed.contains('\\')) return null

            val pathPart: String = if (trimmed.contains("://")) {
                val uri = try {
                    URI(trimmed)
                } catch (_: Exception) {
                    return null
                }
                val scheme = uri.scheme?.lowercase() ?: return null
                if (scheme != "https") return null
                if (!uri.userInfo.isNullOrBlank()) return null
                val host = uri.host?.lowercase() ?: return null
                if (host != "github.com" && host != "www.github.com") return null
                uri.path.orEmpty()
            } else {
                val strippedHost = trimmed
                    .removePrefix("www.github.com/")
                    .removePrefix("github.com/")
                // Reject arbitrary third-party domains such as evil.com/owner/repo
                if (strippedHost == trimmed && trimmed.substringBefore('/').contains('.')) {
                    return null
                }
                strippedHost.substringBefore('?').substringBefore('#')
            }

            val parts = pathPart.split("/").filter { it.isNotBlank() }
            if (parts.size < 2) return null

            val owner = parts[0]
            val repo = parts[1].removeSuffix(".git")
            if (!OWNER_REGEX.matches(owner)) return null
            if (repo == "." || repo == ".." || !REPO_REGEX.matches(repo)) return null

            var branch: String? = null
            var subPath: String? = null

            if (parts.size >= 4 && parts[2] == "tree") {
                val rawBranch = parts[3]
                if (!BRANCH_REGEX.matches(rawBranch) || rawBranch.contains("..")) return null
                branch = rawBranch
                if (parts.size > 4) {
                    val rawSubParts = parts.subList(4, parts.size)
                    if (rawSubParts.any { it == "." || it == ".." || it.contains("..") }) return null
                    subPath = rawSubParts.joinToString("/")
                }
            } else if (parts.size > 2) {
                return null
            }

            return ParsedRepoInfo(owner, repo, branch, subPath)
        }
    }

    data class ParsedRepoInfo(
        val owner: String,
        val repo: String,
        val branch: String?,
        val subPath: String?
    )

    private fun newRequestBuilder(url: String): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/vnd.github.v3+json")
            .addHeader("User-Agent", "JARVIS-Android-SkillInstaller")

        val token = tokenProvider()
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", if (token.startsWith("Bearer ") || token.startsWith("token ")) token else "token $token")
        }
        return builder
    }

    suspend fun searchRepositories(query: String, sort: String = "stars"): List<GitHubRepoItem> =
        withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "$BASE_URL/search/repositories?q=$encodedQuery&sort=$sort&per_page=20"
                val request = newRequestBuilder(url).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext emptyList()
                    }
                    val body = response.body?.string().orEmpty()
                    val root = JSONObject(body)
                    val items = root.optJSONArray("items") ?: return@withContext emptyList()

                    val result = mutableListOf<GitHubRepoItem>()
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val ownerObj = item.optJSONObject("owner")
                        val licenseObj = item.optJSONObject("license")
                        val topicsArr = item.optJSONArray("topics")

                        val topics = mutableListOf<String>()
                        if (topicsArr != null) {
                            for (t in 0 until topicsArr.length()) {
                                topics.add(topicsArr.getString(t))
                            }
                        }

                        result.add(
                            GitHubRepoItem(
                                id = item.optLong("id", 0L),
                                name = item.optString("name", ""),
                                fullName = item.optString("full_name", ""),
                                owner = ownerObj?.optString("login", "") ?: "",
                                description = item.optString("description", "No description provided"),
                                stars = item.optInt("stargazers_count", 0),
                                forks = item.optInt("forks_count", 0),
                                language = item.optString("language", "Generic"),
                                license = licenseObj?.optString("spdx_id", licenseObj.optString("name", "Unknown")) ?: "Unknown",
                                defaultBranch = item.optString("default_branch", "main"),
                                htmlUrl = item.optString("html_url", ""),
                                topics = topics,
                                updatedAt = item.optString("updated_at", "")
                            )
                        )
                    }
                    result
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun getRepository(owner: String, repo: String): GitHubRepoItem? =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/repos/$owner/$repo"
                val request = newRequestBuilder(url).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string().orEmpty()
                    val item = JSONObject(body)
                    val ownerObj = item.optJSONObject("owner")
                    val licenseObj = item.optJSONObject("license")
                    val topicsArr = item.optJSONArray("topics")

                    val topics = mutableListOf<String>()
                    if (topicsArr != null) {
                        for (t in 0 until topicsArr.length()) {
                            topics.add(topicsArr.getString(t))
                        }
                    }

                    GitHubRepoItem(
                        id = item.optLong("id", 0L),
                        name = item.optString("name", repo),
                        fullName = item.optString("full_name", "$owner/$repo"),
                        owner = ownerObj?.optString("login", owner) ?: owner,
                        description = item.optString("description", ""),
                        stars = item.optInt("stargazers_count", 0),
                        forks = item.optInt("forks_count", 0),
                        language = item.optString("language", "Various"),
                        license = licenseObj?.optString("spdx_id", "Unknown") ?: "Unknown",
                        defaultBranch = item.optString("default_branch", "main"),
                        htmlUrl = item.optString("html_url", "https://github.com/$owner/$repo"),
                        topics = topics,
                        updatedAt = item.optString("updated_at", "")
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }

    suspend fun getReadme(owner: String, repo: String, branch: String? = null): String? =
        withContext(Dispatchers.IO) {
            // First attempt raw fetch for high speed and minimal rate-limit impact
            val rawBranch = branch ?: "main"
            val rawUrl = "$RAW_BASE_URL/$owner/$repo/$rawBranch/README.md"
            try {
                val request = Request.Builder().url(rawUrl).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return@withContext response.body?.string()
                    }
                }
            } catch (_: Exception) {}

            // Try "master" branch fallback
            if (branch == null) {
                try {
                    val rawMasterUrl = "$RAW_BASE_URL/$owner/$repo/master/README.md"
                    val request = Request.Builder().url(rawMasterUrl).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            return@withContext response.body?.string()
                        }
                    }
                } catch (_: Exception) {}
            }

            // Fallback to GitHub API readme endpoint
            try {
                val apiUrl = "$BASE_URL/repos/$owner/$repo/readme" + (if (branch != null) "?ref=$branch" else "")
                val request = newRequestBuilder(apiUrl).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string().orEmpty()
                    val obj = JSONObject(body)
                    val contentBase64 = obj.optString("content", "")
                    if (contentBase64.isNotBlank()) {
                        val decodedBytes = Base64.decode(contentBase64.replace("\n", ""), Base64.DEFAULT)
                        return@withContext String(decodedBytes, Charsets.UTF_8)
                    }
                }
            } catch (_: Exception) {}

            null
        }

    suspend fun getRepositoryContents(owner: String, repo: String, path: String = "", ref: String? = null): List<GitHubContentItem> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/repos/$owner/$repo/contents/$path" + (if (ref != null) "?ref=$ref" else "")
                val request = newRequestBuilder(url).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string().orEmpty()

                    if (body.startsWith("[")) {
                        val array = JSONArray(body)
                        val result = mutableListOf<GitHubContentItem>()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            result.add(
                                GitHubContentItem(
                                    name = obj.optString("name", ""),
                                    path = obj.optString("path", ""),
                                    sha = obj.optString("sha", ""),
                                    size = obj.optLong("size", 0L),
                                    type = obj.optString("type", "file"),
                                    downloadUrl = obj.optString("download_url").takeIf { it.isNotBlank() && it != "null" }
                                )
                            )
                        }
                        return@withContext result
                    } else if (body.startsWith("{")) {
                        val obj = JSONObject(body)
                        return@withContext listOf(
                            GitHubContentItem(
                                name = obj.optString("name", ""),
                                path = obj.optString("path", ""),
                                sha = obj.optString("sha", ""),
                                size = obj.optLong("size", 0L),
                                type = obj.optString("type", "file"),
                                downloadUrl = obj.optString("download_url").takeIf { it.isNotBlank() && it != "null" }
                            )
                        )
                    }
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun getFileContent(owner: String, repo: String, path: String, ref: String? = null): String? =
        withContext(Dispatchers.IO) {
            // Raw attempt first
            val branch = ref ?: "main"
            try {
                val rawUrl = "$RAW_BASE_URL/$owner/$repo/$branch/$path"
                val request = Request.Builder().url(rawUrl).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        return@withContext response.body?.string()
                    }
                }
            } catch (_: Exception) {}

            // GitHub REST endpoint fallback
            try {
                val url = "$BASE_URL/repos/$owner/$repo/contents/$path" + (if (ref != null) "?ref=$ref" else "")
                val request = newRequestBuilder(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string().orEmpty()
                    val obj = JSONObject(body)
                    val contentBase64 = obj.optString("content", "")
                    if (contentBase64.isNotBlank()) {
                        val decodedBytes = Base64.decode(contentBase64.replace("\n", ""), Base64.DEFAULT)
                        return@withContext String(decodedBytes, Charsets.UTF_8)
                    }
                }
            } catch (_: Exception) {}

            null
        }

    suspend fun getBranches(owner: String, repo: String): List<GitHubBranchItem> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/repos/$owner/$repo/branches"
                val request = newRequestBuilder(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string().orEmpty()
                    val array = JSONArray(body)
                    val result = mutableListOf<GitHubBranchItem>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val commitObj = obj.optJSONObject("commit")
                        result.add(
                            GitHubBranchItem(
                                name = obj.optString("name", "main"),
                                commitSha = commitObj?.optString("sha", "").orEmpty()
                            )
                        )
                    }
                    result
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun getReleases(owner: String, repo: String): List<GitHubReleaseItem> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/repos/$owner/$repo/releases"
                val request = newRequestBuilder(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val body = response.body?.string().orEmpty()
                    val array = JSONArray(body)
                    val result = mutableListOf<GitHubReleaseItem>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        result.add(
                            GitHubReleaseItem(
                                tagName = obj.optString("tag_name", ""),
                                name = obj.optString("name", obj.optString("tag_name", "")),
                                body = obj.optString("body", ""),
                                publishedAt = obj.optString("published_at", ""),
                                htmlUrl = obj.optString("html_url", "")
                            )
                        )
                    }
                    result
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
}
