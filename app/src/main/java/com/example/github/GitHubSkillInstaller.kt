package com.example.github

import android.content.Context
import com.example.data.local.entity.DevWorkspaceRepoEntity
import com.example.data.local.entity.InstalledSkillEntity
import com.example.data.local.entity.SkillAuditLogEntity
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class UpdateCheckResult(
    val hasUpdate: Boolean,
    val currentVersion: String,
    val latestVersion: String,
    val releaseNotes: String = "",
    val newPermissionsRequested: List<String> = emptyList()
)

class GitHubSkillInstaller(
    private val context: Context,
    val apiClient: GitHubApiClient,
    private val repository: JarvisRepository
) {

    /**
     * Curated featured repositories compatible with JARVIS.
     */
    fun getFeaturedSkills(): List<GitHubRepoItem> {
        return listOf(
            GitHubRepoItem(
                id = 101,
                name = "open-jarvis",
                fullName = "samiahmedyoyo1990-ui/open-jarvis",
                owner = "samiahmedyoyo1990-ui",
                description = "JARVIS Ultimate Personal AI Assistant ecosystem for Android.",
                stars = 420,
                forks = 85,
                language = "Kotlin",
                license = "Apache-2.0",
                defaultBranch = "main",
                htmlUrl = "https://github.com/samiahmedyoyo1990-ui/open-jarvis",
                topics = listOf("ai-assistant", "android", "bangla-ai", "kotlin", "jarvis")
            ),
            GitHubRepoItem(
                id = 102,
                name = "jarvis-coding-agent",
                fullName = "jarvis-skills/coding-agent",
                owner = "jarvis-skills",
                description = "AI Coding Specialist Skill for Android Architecture, Jetpack Compose & Clean Code.",
                stars = 215,
                forks = 34,
                language = "Markdown",
                license = "MIT",
                defaultBranch = "main",
                htmlUrl = "https://github.com/jarvis-skills/coding-agent",
                topics = listOf("coding-agent", "agent-skill", "prompts")
            ),
            GitHubRepoItem(
                id = 103,
                name = "bangla-linguistics-pack",
                fullName = "jarvis-skills/bangla-linguistics",
                owner = "jarvis-skills",
                description = "Romanized Bangla, Bengali NLP prompt pack and translation intelligence.",
                stars = 180,
                forks = 22,
                language = "JSON",
                license = "MIT",
                defaultBranch = "main",
                htmlUrl = "https://github.com/jarvis-skills/bangla-linguistics",
                topics = listOf("bengali", "banglish", "translation", "nlp")
            ),
            GitHubRepoItem(
                id = 104,
                name = "live-research-tools",
                fullName = "jarvis-skills/live-web-search",
                owner = "jarvis-skills",
                description = "Live web research, fact verification, and real-time knowledge synthesis tool definitions.",
                stars = 310,
                forks = 48,
                language = "JSON",
                license = "Apache-2.0",
                defaultBranch = "main",
                htmlUrl = "https://github.com/jarvis-skills/live-web-search",
                topics = listOf("research", "fact-check", "duckduckgo", "tools")
            ),
            GitHubRepoItem(
                id = 105,
                name = "device-automation-suite",
                fullName = "jarvis-skills/device-automation",
                owner = "jarvis-skills",
                description = "Android app launcher, settings shortcuts, routine triggers, and phone telemetry skills.",
                stars = 145,
                forks = 19,
                language = "JSON",
                license = "MIT",
                defaultBranch = "main",
                htmlUrl = "https://github.com/jarvis-skills/device-automation",
                topics = listOf("automation", "android-intent", "routines")
            )
        )
    }

    /**
     * Inspects a GitHub repository URL or full name:
     * 1. Retrieves repository metadata
     * 2. Retrieves file list
     * 3. Fetches README
     * 4. Parses manifest (jarvis.skill.json, SKILL.md, prompts.json, or dev source)
     * 5. Runs security scanner
     * 6. Produces comprehensive SecurityAuditReport
     */
    suspend fun inspectRepositoryUrl(
        urlOrName: String,
        branchOverride: String? = null
    ): SecurityAuditReport = withContext(Dispatchers.IO) {
        val parsed = GitHubApiClient.parseUrl(urlOrName)
            ?: return@withContext SecurityAuditReport(
                repoUrl = urlOrName,
                repoFullName = urlOrName,
                branch = "main",
                commitSha = "",
                compatibilityType = CompatibilityType.UNSUPPORTED,
                status = AuditStatus.UNSUPPORTED,
                integritySha256 = "",
                detectedManifestName = null,
                requestedPermissions = emptyList(),
                findings = listOf(SecurityFinding("CRITICAL", "অবৈধ গিটহাব লিঙ্ক বা ফরম্যাট। সঠিক URL: https://github.com/owner/repo")),
                skillManifest = null,
                isInstallable = false,
                isDeveloperSource = false
            )

        val owner = parsed.owner
        val repo = parsed.repo
        val fullName = "$owner/$repo"
        val branch = branchOverride ?: parsed.branch ?: "main"

        // 1. Fetch repo metadata
        val repoInfo = apiClient.getRepository(owner, repo)

        // 2. Fetch repository contents
        val contents = apiClient.getRepositoryContents(owner, repo, parsed.subPath.orEmpty(), branch)

        // 3. Fetch README
        val readme = apiClient.getReadme(owner, repo, branch).orEmpty()

        // 4. Parse skill manifest
        val parseResult = SkillManifestParser.parseRepositorySkill(
            repoFullName = fullName,
            defaultBranch = branch,
            files = contents
        ) { fileName ->
            apiClient.getFileContent(owner, repo, fileName, branch)
        }

        // 5. Run Security Inspection
        val auditReport = SkillSecurityInspector.inspectRepository(
            repoFullName = fullName,
            branch = branch,
            files = contents,
            manifest = parseResult.manifest,
            readmeContent = readme,
            compatibilityType = parseResult.compatibilityType
        )

        // Record audit log to local database
        try {
            val findingsArray = JSONArray()
            auditReport.findings.forEach {
                findingsArray.put(JSONObject().apply {
                    put("level", it.level)
                    put("message", it.message)
                    put("affectedItem", it.affectedItem)
                })
            }
            repository.recordAuditLog(
                SkillAuditLogEntity(
                    repoUrl = "https://github.com/$fullName",
                    status = auditReport.status.name,
                    securityScore = if (auditReport.status == AuditStatus.COMPATIBLE) 100 else if (auditReport.status == AuditStatus.WARNING) 70 else 40,
                    findingsJson = findingsArray.toString()
                )
            )
        } catch (_: Exception) {}

        auditReport
    }

    /**
     * One-Click Installation after user approval.
     */
    suspend fun installSkill(
        report: SecurityAuditReport,
        approvedPermissions: List<String>
    ): Long = withContext(Dispatchers.IO) {
        val manifest = report.skillManifest ?: return@withContext -1L

        val permissionsArray = JSONArray()
        approvedPermissions.forEach { permissionsArray.put(it) }

        val entity = InstalledSkillEntity(
            skillId = manifest.id,
            name = manifest.name,
            banglaName = if (manifest.banglaName.isNotBlank()) manifest.banglaName else manifest.name,
            author = manifest.author,
            version = manifest.version,
            description = manifest.description,
            category = manifest.category,
            skillType = manifest.type,
            repoUrl = report.repoUrl,
            repoBranch = report.branch,
            commitSha = report.commitSha,
            license = "Verified",
            isEnabled = true,
            grantedPermissionsJson = permissionsArray.toString(),
            manifestJson = manifest.toJsonString(),
            systemPromptSnippet = manifest.systemPrompt,
            toolsJson = "[]",
            integrityHash = report.integritySha256
        )

        repository.saveInstalledSkill(entity)
    }

    /**
     * Imports a source-code project into Developer Workspace.
     */
    suspend fun importToDeveloperWorkspace(
        report: SecurityAuditReport,
        repoItem: GitHubRepoItem? = null
    ): Long = withContext(Dispatchers.IO) {
        val parsed = GitHubApiClient.parseUrl(report.repoUrl)
        val owner = parsed?.owner ?: report.repoFullName.substringBefore('/')
        val repo = parsed?.repo ?: report.repoFullName.substringAfter('/')

        val files = apiClient.getRepositoryContents(owner, repo, ref = report.branch)
        val fileTreeArray = JSONArray()
        files.forEach { fileTreeArray.put("${if (it.type == "dir") "📁 " else "📄 "}${it.name}") }

        val entity = DevWorkspaceRepoEntity(
            repoName = repo,
            owner = owner,
            url = report.repoUrl,
            defaultBranch = report.branch,
            description = repoItem?.description ?: "Developer source project from GitHub",
            language = repoItem?.language ?: "Various",
            stars = repoItem?.stars ?: 0,
            readmeContent = report.rawReadme,
            fileTreeJson = fileTreeArray.toString(),
            aiAnalysisNotes = "সোর্স কোড প্যাকেজ সফলভাবে ডেভেলপার ওয়ার্কস্পেসে ইমপোর্ট হয়েছে। আর্কিটেকচার ও কোড বিশ্লেষণের জন্য প্রস্তুত।"
        )

        repository.saveDevRepo(entity)
    }

    /**
     * Checks for updates by querying upstream releases or branch commits.
     */
    suspend fun checkForUpdates(skill: InstalledSkillEntity): UpdateCheckResult =
        withContext(Dispatchers.IO) {
            val parsed = GitHubApiClient.parseUrl(skill.repoUrl) ?: return@withContext UpdateCheckResult(false, skill.version, skill.version)

            val releases = apiClient.getReleases(parsed.owner, parsed.repo)
            if (releases.isNotEmpty()) {
                val latest = releases.first()
                val latestTag = latest.tagName.removePrefix("v").trim()
                val isNewer = latestTag != skill.version.removePrefix("v").trim()
                return@withContext UpdateCheckResult(
                    hasUpdate = isNewer,
                    currentVersion = skill.version,
                    latestVersion = latest.tagName,
                    releaseNotes = latest.body
                )
            }

            // Fallback: check default branch commit SHA
            val branches = apiClient.getBranches(parsed.owner, parsed.repo)
            val currentBranch = branches.find { it.name == skill.repoBranch }
            if (currentBranch != null && currentBranch.commitSha.isNotBlank() && skill.commitSha.isNotBlank()) {
                val hasNewCommit = !currentBranch.commitSha.startsWith(skill.commitSha)
                return@withContext UpdateCheckResult(
                    hasUpdate = hasNewCommit,
                    currentVersion = skill.version,
                    latestVersion = "${skill.version} (${currentBranch.commitSha.take(7)})",
                    releaseNotes = "New commits detected on branch ${skill.repoBranch}"
                )
            }

            UpdateCheckResult(false, skill.version, skill.version)
        }

    /**
     * Updates an installed skill, creating a rollback backup snapshot first.
     */
    suspend fun updateSkill(skill: InstalledSkillEntity): Boolean = withContext(Dispatchers.IO) {
        val inspection = inspectRepositoryUrl(skill.repoUrl, skill.repoBranch)
        val manifest = inspection.skillManifest ?: return@withContext false

        // Snapshot current state for 1-click rollback
        val backupSnapshot = JSONObject().apply {
            put("version", skill.version)
            put("systemPrompt", skill.systemPromptSnippet)
            put("integrityHash", skill.integrityHash)
            put("commitSha", skill.commitSha)
            put("updatedAt", skill.updatedAt)
        }.toString()

        val updated = skill.copy(
            version = manifest.version,
            systemPromptSnippet = manifest.systemPrompt,
            commitSha = inspection.commitSha,
            integrityHash = inspection.integritySha256,
            updatedAt = System.currentTimeMillis(),
            previousVersionBackupJson = backupSnapshot
        )

        repository.updateInstalledSkill(updated)
        true
    }

    /**
     * 1-Click Rollback to previous version snapshot.
     */
    suspend fun rollbackSkill(skill: InstalledSkillEntity): Boolean = withContext(Dispatchers.IO) {
        val backup = skill.previousVersionBackupJson ?: return@withContext false
        try {
            val obj = JSONObject(backup)
            val rolledBack = skill.copy(
                version = obj.optString("version", skill.version),
                systemPromptSnippet = obj.optString("systemPrompt", skill.systemPromptSnippet),
                integrityHash = obj.optString("integrityHash", skill.integrityHash),
                commitSha = obj.optString("commitSha", skill.commitSha),
                updatedAt = System.currentTimeMillis(),
                previousVersionBackupJson = null
            )
            repository.updateInstalledSkill(rolledBack)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun uninstallSkill(id: Long) = withContext(Dispatchers.IO) {
        repository.deleteInstalledSkill(id)
    }
}
