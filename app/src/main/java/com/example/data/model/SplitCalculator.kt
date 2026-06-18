package com.example.data.model

import java.io.Serializable
import kotlin.math.abs

data class MemberBalance(
    val member: GroupMember,
    val totalPaid: Double,
    val totalOwed: Double,
    val netBalance: Double // totalPaid - totalOwed
) : Serializable

data class SettlementSuggestion(
    val debtor: GroupMember,
    val creditor: GroupMember,
    val amount: Double
) : Serializable

data class GroupSummary(
    val totalExpenses: Double,
    val perPersonExpense: Double,
    val memberBalances: List<MemberBalance>,
    val settlements: List<SettlementSuggestion>
) : Serializable

object SplitCalculator {

    fun calculateGroupSummary(
        members: List<GroupMember>,
        expenses: List<Expense>,
        allShares: List<ExpenseShare>
    ): GroupSummary {
        val totalExpenses = expenses.sumOf { it.amount }
        val memberCount = members.size
        val perPersonExpense = if (memberCount > 0) totalExpenses / memberCount else 0.0

        // Create initial maps
        val totalPaidMap = mutableMapOf<Long, Double>()
        val totalOwedMap = mutableMapOf<Long, Double>()

        // Initialize maps for all members
        members.forEach {
            totalPaidMap[it.id] = 0.0
            totalOwedMap[it.id] = 0.0
        }

        // Sum up total expansions paid by each member
        expenses.forEach { expense ->
            val paidId = expense.paidByMemberId
            val currentPaid = totalPaidMap[paidId] ?: 0.0
            totalPaidMap[paidId] = currentPaid + expense.amount
        }

        // Sum up total shares owed by each member
        allShares.forEach { share ->
            val oweId = share.memberId
            if (totalOwedMap.containsKey(oweId)) {
                val currentOwed = totalOwedMap[oweId] ?: 0.0
                totalOwedMap[oweId] = currentOwed + share.shareAmount
            }
        }

        // build MemberBalance list
        val memberBalances = members.map { member ->
            val paid = totalPaidMap[member.id] ?: 0.0
            val owed = totalOwedMap[member.id] ?: 0.0
            MemberBalance(
                member = member,
                totalPaid = paid,
                totalOwed = owed,
                netBalance = paid - owed
            )
        }

        // Calculate minimum transfers using a greedy algorithm
        val settlements = mutableListOf<SettlementSuggestion>()

        // Lists of (Member, Balance)
        val debtors = mutableListOf<Pair<GroupMember, Double>>()
        val creditors = mutableListOf<Pair<GroupMember, Double>>()

        memberBalances.forEach { mb ->
            // Allow small epsilon tolerance for float rounding
            if (mb.netBalance < -0.01) {
                debtors.add(Pair(mb.member, mb.netBalance))
            } else if (mb.netBalance > 0.01) {
                creditors.add(Pair(mb.member, mb.netBalance))
            }
        }

        // Sort debtors ascending (most negative first) and creditors descending (most positive first)
        debtors.sortBy { it.second }
        creditors.sortByDescending { it.second }

        var dIdx = 0
        var cIdx = 0

        val debtorsMutable = debtors.map { it.first to it.second }.toMutableList()
        val creditorsMutable = creditors.map { it.first to it.second }.toMutableList()

        while (dIdx < debtorsMutable.size && cIdx < creditorsMutable.size) {
            val debtorPair = debtorsMutable[dIdx]
            val creditorPair = creditorsMutable[cIdx]

            val dMember = debtorPair.first
            val dAmount = debtorPair.second // Negative value!

            val cMember = creditorPair.first
            val cAmount = creditorPair.second // Positive value!

            val amountToTransfer = minOf(abs(dAmount), cAmount)

            if (amountToTransfer > 0.01) {
                settlements.add(
                    SettlementSuggestion(
                        debtor = dMember,
                        creditor = cMember,
                        amount = amountToTransfer
                    )
                )
            }

            // Update remaining balances
            val newDAmount = dAmount + amountToTransfer
            val newCAmount = cAmount - amountToTransfer

            debtorsMutable[dIdx] = dMember to newDAmount
            creditorsMutable[cIdx] = cMember to newCAmount

            if (abs(newDAmount) < 0.01) {
                dIdx++
            }
            if (abs(newCAmount) < 0.01) {
                cIdx++
            }
        }

        return GroupSummary(
            totalExpenses = totalExpenses,
            perPersonExpense = perPersonExpense,
            memberBalances = memberBalances,
            settlements = settlements
        )
    }
}
