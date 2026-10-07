package com.reddy.vittify.data.update.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubRelease(
    @SerialName("id") val id: Long = 0,
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("prerelease") val prerelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("target_commitish") val targetCommitish: String? = null,
    @SerialName("assets") val assets: List<GithubAsset> = emptyList()
)

@Serializable
data class GithubAsset(
    @SerialName("id") val id: Long = 0,
    @SerialName("name") val name: String = "",
    @SerialName("size") val size: Long = 0,
    @SerialName("browser_download_url") val downloadUrl: String = "",
    @SerialName("content_type") val contentType: String? = null
)

data class AppUpdateInfo(
    val release: GithubRelease,
    val versionName: String,
    val isBeta: Boolean,
    val changelog: String,
    val downloadUrl: String,
    val apkName: String,
    val apkSize: Long
)
