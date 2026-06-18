package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "ROOM" or "TRIP"
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val currency: String = "INR", // INR, USD, EUR, AED, GBP
    val targetBudget: Double = 0.0,
    val rentDueDate: Long = 0L, // timestamp or day of month (1-31)
    val isSynced: Boolean = false
) : Serializable

@Entity(tableName = "group_members")
data class GroupMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val upiId: String = "", // for easy settlements inside India
    val phone: String = ""
) : Serializable

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val title: String,
    val amount: Double,
    val paidByMemberId: Long, // ID of member who paid
    val category: String, // "Rent", "Electricity", "Groceries", "Food", "Hotel", "Shopping", Taxi, etc.
    val timestamp: Long = System.currentTimeMillis(),
    val splitType: String = "EQUAL", // EQUAL, CUSTOM, PERCENTAGE, SHARES
    val rawSplitDetails: String = "", // custom layout details
    val baseAmountInUpi: Double = 0.0, // for multi-currency tracking if trip isUSD, etc.
    val receiptImagePath: String? = null // for scanner OCR receipt attachment
) : Serializable

@Entity(tableName = "expense_shares")
data class ExpenseShare(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val memberId: Long,
    val shareAmount: Double
) : Serializable
