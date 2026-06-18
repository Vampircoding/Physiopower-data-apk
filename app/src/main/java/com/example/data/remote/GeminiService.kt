package com.example.data.remote

import android.util.Log
import com.example.data.model.Expense
import com.example.data.model.GroupMember
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

@JsonClass(generateAdapter = true)
data class OcrResult(
    val merchant: String,
    val amount: Double,
    val date: String,
    val category: String,
    val confidence: String
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

object GeminiService {
    private const val TAG = "GeminiService"

    suspend fun scanReceiptOcr(receiptText: String): OcrResult? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured")
            return@withContext null
        }

        val prompt = """
            Parse the following bill/receipt text and extract:
            1. Merchant name (under merchant)
            2. Total amount as a double (under amount)
            3. Date of expense (under date, format: YYYY-MM-DD or DD-MM-YYYY)
            4. Best matched expense category (under category, choose from: Rent, Electricity, Water, Internet, Gas, Maid, Groceries, Hotel, Flight, Train, Taxi, Fuel, Food, Shopping, Activities, Emergency, Maintenance)
            5. Extraction confidence statement (high/medium/low under confidence)

            Receipt Text Content:
            $receiptText

            Format your response strictly as a single JSON object. Do not include markdown code block syntax with backticks or any trailing text. Just the raw JSON object.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.1f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText != null) {
                val adapter = RetrofitClient.moshi.adapter(OcrResult::class.java)
                return@withContext adapter.fromJson(responseText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in OCR scanning: ${e.message}", e)
        }
        return@withContext null
    }

    suspend fun analyzeGroupExpensesText(
        expenses: List<Expense>,
        members: List<GroupMember>,
        groupName: String,
        groupType: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "AI features are in preview. Please configure your GEMINI_API_KEY in the AI Studio Secrets panel to get live, personalized insights!"
        }

        val expensesDetails = expenses.joinToString("\n") { exp ->
            val paidBy = members.find { it.id == exp.paidByMemberId }?.name ?: "Unknown"
            "- ${exp.title}: ₹${exp.amount} (Paid by: $paidBy, Category: ${exp.category})"
        }

        val membersList = members.joinToString(", ") { it.name }

        val systemInstruction = "You are Sagar Split Pro's Senior Expense Auditor and AI Budget Advisor. Speak dynamically with financial wisdom, providing concise, actionable split reports for roommates or trip-makers."

        val prompt = """
            Analyze these finances for the $groupType Group: "$groupName"
            Members: $membersList
            
            Expenses Recorded:
            $expensesDetails
            
            Please provide:
            1. **Budget Health Evaluation**: Total group spending structure and a warning if there are any unusual outlays (like bloated bills or disproportionate categories).
            2. **Smart Cost Saving Recommendations**: Suggest 2-3 specific, budget-friendly ideas for flatmates or trip members to reduce bills (e.g. optimizing AC hours for electricity, ordering group groceries in bulk, sharing taxi rates).
            3. **Forward Budget Prediction**: Forecast next month's Room rent or trip budget based on current trends.
            
            Keep your response highly structured, formatted with markdown bullets, and engaging. Mention 'Created by Sagar' proudly. Keep it under 250 words total.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(
                temperature = 0.7f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (responseText != null) {
                return@withContext responseText
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in AI analysis: ${e.message}", e)
        }
        return@withContext "Failed to fetch AI insights. Check your internet connection and verify that your Gemini API Key is correctly configured in the Secrets panel."
    }
}
