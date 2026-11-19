package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.security.MessageDigest
import java.security.SecureRandom

@JsonClass(generateAdapter = true)
data class YouTubeChannelsResponse(
    val items: List<YouTubeChannelItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelItem(
    val id: String = "",
    val snippet: YouTubeChannelSnippet? = null,
    val statistics: YouTubeChannelStatistics? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelSnippet(
    val title: String = "",
    val description: String = "",
    val customUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeChannelStatistics(
    val subscriberCount: String? = null,
    val videoCount: String? = null,
    val viewCount: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YouTubeSearchItem(
    val id: YouTubeVideoId? = null,
    val snippet: YouTubeVideoSnippet? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoId(
    val videoId: String? = null
)

@JsonClass(generateAdapter = true)
data class YouTubeVideoSnippet(
    val title: String = "",
    val description: String = "",
    val publishedAt: String = ""
)

interface YouTubeDataApi {
    @GET("youtube/v3/channels")
    suspend fun getMyChannels(
        @Header("Authorization") bearerToken: String,
        @Query("part") part: String = "snippet,statistics",
        @Query("mine") mine: Boolean = true
    ): YouTubeChannelsResponse

    @GET("youtube/v3/search")
    suspend fun getMyVideos(
        @Header("Authorization") bearerToken: String,
        @Query("part") part: String = "snippet",
        @Query("forMine") forMine: Boolean = true,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 10
    ): YouTubeSearchResponse
}

object YouTubeOAuthHelper {
    const val YOUTUBE_READONLY_SCOPE = "https://www.googleapis.com/auth/youtube.readonly"
    const val YOUTUBE_UPLOAD_SCOPE = "https://www.googleapis.com/auth/youtube.upload"
    const val REDIRECT_URI = "com.aistudio.myvideo.vkmxqz:/oauth2redirect"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val api: YouTubeDataApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YouTubeDataApi::class.java)
    }

    fun generatePkceVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun buildGoogleOAuthUrl(
        clientId: String,
        scopes: List<String>,
        codeChallenge: String
    ): Uri {
        return Uri.parse("https://accounts.google.com/o/oauth2/v2/auth").buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", scopes.joinToString(" "))
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "consent")
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }

    fun launchBrowserOAuth(context: Context, clientId: String, scopes: List<String>): Result<String> {
        return try {
            val verifier = generatePkceVerifier()
            val challenge = generateCodeChallenge(verifier)
            val authUri = buildGoogleOAuthUrl(clientId, scopes, challenge)
            val intent = Intent(Intent.ACTION_VIEW, authUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(verifier)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openInYouTube(context: Context, youtubeVideoId: String?, channelId: String? = null) {
        val url = when {
            !youtubeVideoId.isNullOrBlank() -> "https://www.youtube.com/watch?v=$youtubeVideoId"
            !channelId.isNullOrBlank() -> "https://www.youtube.com/channel/$channelId"
            else -> "https://www.youtube.com"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
