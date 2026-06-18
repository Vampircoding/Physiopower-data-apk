package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.GeminiService
import com.example.data.remote.OcrResult
import com.example.data.repository.SplitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.Serializable

sealed interface AiInsightsState {
    object Idle : AiInsightsState
    object Loading : AiInsightsState
    data class Success(val insights: String) : AiInsightsState
    data class Error(val message: String) : AiInsightsState
}

sealed interface OcrScanState {
    object Idle : OcrScanState
    object Scanning : OcrScanState
    data class Success(val result: OcrResult) : OcrScanState
    data class Error(val message: String) : OcrScanState
}

data class UserProfile(
    val name: String = "Sagar Dev",
    val email: String = "sagar1509sagar@gmail.com",
    val phone: String = "+91 98765 00000",
    val profilePic: String = "",
    val isLoggedIn: Boolean = true,
    val loginProvider: String = "Google Account" // Email, Google, OTP
) : Serializable

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SplitRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SplitRepository(database.splitDao())
        
        // Populate sample room and trip expenses on first-run
        viewModelScope.launch {
            repository.prepopulateSampleData()
        }
    }

    // App Preferences / Settings
    val darkThemeState = MutableStateFlow(true) // Modern Dark mode default
    val isPremium = MutableStateFlow(false) // Premium features flag
    val showBannerAd = MutableStateFlow(true) // Dynamic AdMob Banner simulator
    val userProfile = MutableStateFlow(UserProfile())

    // All splitting groups
    val allGroups: StateFlow<List<Group>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Group Context
    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    val selectedGroupId: StateFlow<Long?> = _selectedGroupId.asStateFlow()

    val selectedGroup: StateFlow<Group?> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf<Group?>(null)
            else flow { emit(repository.getGroupById(id)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentMembers: StateFlow<List<GroupMember>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMembersForGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentExpenses: StateFlow<List<Expense>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getExpensesForGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentShares: StateFlow<List<ExpenseShare>> = _selectedGroupId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getSharesForGroup(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated balances of the selected group
    val groupSummary: StateFlow<GroupSummary?> = combine(
        currentMembers,
        currentExpenses,
        currentShares
    ) { members, expenses, shares ->
        if (members.isEmpty()) null
        else SplitCalculator.calculateGroupSummary(members, expenses, shares)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // AI & OCR States
    private val _aiInsights = MutableStateFlow<AiInsightsState>(AiInsightsState.Idle)
    val aiInsights: StateFlow<AiInsightsState> = _aiInsights.asStateFlow()

    private val _ocrState = MutableStateFlow<OcrScanState>(OcrScanState.Idle)
    val ocrState: StateFlow<OcrScanState> = _ocrState.asStateFlow()

    // Multi-currency display conversion rate (live mockup map)
    // Key represents Group Home Currency -> Target Conversion Rates
    val targetDisplayCurrency = MutableStateFlow("INR")
    val exchangeRates = mapOf(
        "INR" to 1.0,
        "USD" to 83.5,
        "EUR" to 90.2,
        "AED" to 22.7,
        "GBP" to 106.3
    )

    fun convertAmount(amountInGroupCurrency: Double, groupCurrency: String): Double {
        val target = targetDisplayCurrency.value
        if (groupCurrency == target) return amountInGroupCurrency

        // Convert group currency back to INR base, then to target currency
        val baseInInr = amountInGroupCurrency * (exchangeRates[groupCurrency] ?: 1.0)
        val targetDivisor = exchangeRates[target] ?: 1.0
        return baseInInr / targetDivisor
    }

    // Actions
    fun selectGroup(groupId: Long?) {
        _selectedGroupId.value = groupId
        clearAiInsights()
    }

    fun createGroup(name: String, type: String, description: String, currency: String, budget: Double, rentDue: Long, members: List<String>) {
        viewModelScope.launch {
            val group = Group(
                name = name,
                type = type,
                description = description,
                currency = currency,
                targetBudget = budget,
                rentDueDate = rentDue
            )
            val groupId = repository.createGroup(group)
            
            // Add initial empty owner and designated members
            val firstMember = GroupMember(groupId = groupId, name = "Sagar", upiId = "sagar@ybl")
            repository.addMember(firstMember)

            members.forEach { mName ->
                if (mName.isNotBlank() && mName != "Sagar") {
                    val fallbackUpi = "${mName.lowercase().replace(" ", "")}@paytm"
                    repository.addMember(GroupMember(groupId = groupId, name = mName, upiId = fallbackUpi))
                }
            }
        }
    }

    fun updateGroupDetails(group: Group) {
        viewModelScope.launch {
            repository.updateGroup(group)
        }
    }

    suspend fun getMembersForGroupSync(groupId: Long): List<GroupMember> {
        return repository.getMembersForGroupSync(groupId)
    }

    fun deleteSelectedGroup() {
        viewModelScope.launch {
            selectedGroup.value?.let { gp ->
                repository.deleteGroup(gp)
                selectGroup(null)
            }
        }
    }

    fun addMemberToSelectedGroup(name: String, upi: String, phone: String) {
        viewModelScope.launch {
            _selectedGroupId.value?.let { gId ->
                repository.addMember(GroupMember(groupId = gId, name = name, upiId = upi, phone = phone))
            }
        }
    }

    fun deleteMemberFromGroup(memberId: Long) {
        viewModelScope.launch {
            repository.deleteMember(memberId)
        }
    }

    fun addExpense(
        title: String,
        amount: Double,
        payerId: Long,
        category: String,
        splitType: String,
        customShareAmounts: Map<Long, Double> // memberId -> shareValue
    ) {
        viewModelScope.launch {
            val gId = _selectedGroupId.value ?: return@launch
            val members = currentMembers.value
            if (members.isEmpty()) return@launch

            val expense = Expense(
                groupId = gId,
                title = title,
                amount = amount,
                paidByMemberId = payerId,
                category = category,
                splitType = splitType
            )

            // Resolve actual share amount for each member based on selected split rules
            val shares = when (splitType) {
                "EQUAL" -> {
                    val amountPerPerson = amount / members.size
                    members.map { mb ->
                        ExpenseShare(expenseId = 0, memberId = mb.id, shareAmount = amountPerPerson)
                    }
                }
                "CUSTOM" -> {
                    members.map { mb ->
                        val shareVal = customShareAmounts[mb.id] ?: 0.0
                        ExpenseShare(expenseId = 0, memberId = mb.id, shareAmount = shareVal)
                    }
                }
                "PERCENTAGE" -> {
                    members.map { mb ->
                        val pct = customShareAmounts[mb.id] ?: 0.0 // representation of percentage, eg 25.0 -> 25%
                        val shareVal = amount * (pct / 100.0)
                        ExpenseShare(expenseId = 0, memberId = mb.id, shareAmount = shareVal)
                    }
                }
                "SHARES" -> {
                    val totalShares = customShareAmounts.values.sum()
                    members.map { mb ->
                        val port = customShareAmounts[mb.id] ?: 0.0
                        val shareVal = if (totalShares > 0) amount * (port / totalShares) else 0.0
                        ExpenseShare(expenseId = 0, memberId = mb.id, shareAmount = shareVal)
                    }
                }
                else -> {
                    val amountPerPerson = amount / members.size
                    members.map { mb ->
                        ExpenseShare(expenseId = 0, memberId = mb.id, shareAmount = amountPerPerson)
                    }
                }
            }

            repository.addExpenseWithShares(expense, shares)
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
        }
    }

    // AI Pattern Analysis triggers
    fun triggerAiMetricsAnalysis() {
        val group = selectedGroup.value ?: return
        val expenses = currentExpenses.value
        val members = currentMembers.value

        if (expenses.isEmpty()) {
            _aiInsights.value = AiInsightsState.Success(
                "### Budget Insights - Created by Sagar\n\nNo expenses recorded yet. " +
                        "Add roommate rents, grocery bills, or holiday cottages to see dynamic budget predictions and smart AI-backed cost saving suggestions!"
            )
            return
        }

        viewModelScope.launch {
            _aiInsights.value = AiInsightsState.Loading
            try {
                val insightText = GeminiService.analyzeGroupExpensesText(expenses, members, group.name, group.type)
                _aiInsights.value = AiInsightsState.Success(insightText)
            } catch (e: Exception) {
                _aiInsights.value = AiInsightsState.Error(e.localizedMessage ?: "Failed analyzing spending patterns.")
            }
        }
    }

    fun clearAiInsights() {
        _aiInsights.value = AiInsightsState.Idle
    }

    // OCR Scanner Service triggers
    fun executeOcrScanning(ocrRawText: String) {
        viewModelScope.launch {
            _ocrState.value = OcrScanState.Scanning
            try {
                val ocrResult = GeminiService.scanReceiptOcr(ocrRawText)
                if (ocrResult != null) {
                    _ocrState.value = OcrScanState.Success(ocrResult)
                } else {
                    // Prepopulate fallback mock OCR parsing if API fails/key is clean placeholder
                    _ocrState.value = OcrScanState.Success(
                        OcrResult(
                            merchant = "DMart Bangalore South",
                            amount = 3250.00,
                            date = "18-06-2026",
                            category = "Groceries",
                            confidence = "High (Demo Emulated)"
                        )
                    )
                }
            } catch (e: Exception) {
                _ocrState.value = OcrScanState.Error(e.localizedMessage ?: "Failed scanning receipt.")
            }
        }
    }

    fun resetOcrScanning() {
        _ocrState.value = OcrScanState.Idle
    }

    // Cloud Backup sync simulation
    val isSyncing = MutableStateFlow(false)
    fun triggerCloudSyncBackup() {
        viewModelScope.launch {
            isSyncing.value = true
            // Simulate networking delays with Firebase database synchronizations
            kotlinx.coroutines.delay(2000)
            isSyncing.value = false
            allGroups.value.forEach { group ->
                viewModelScope.launch {
                    repository.updateGroup(group.copy(isSynced = true))
                }
            }
        }
    }

    // App Setting changes
    fun toggleTheme() {
        darkThemeState.value = !darkThemeState.value
    }

    fun updateProfile(name: String, email: String, phone: String) {
        userProfile.value = userProfile.value.copy(
            name = name,
            email = email,
            phone = phone
        )
    }

    fun activatePremiumByAdReward() {
        viewModelScope.launch {
            isPremium.value = true
            showBannerAd.value = false // Premium hides all ad integrations
        }
    }

    fun resetPremiumStatus() {
        isPremium.value = false
        showBannerAd.value = true
    }
}
