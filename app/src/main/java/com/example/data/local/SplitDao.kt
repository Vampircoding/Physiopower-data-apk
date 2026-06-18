package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SplitDao {
    // Groups
    @Query("SELECT * FROM groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: Long): Group?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group): Long

    @Update
    suspend fun updateGroup(group: Group)

    @Delete
    suspend fun deleteGroup(group: Group)

    // Members
    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun getMembersForGroup(groupId: Long): Flow<List<GroupMember>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    suspend fun getMembersForGroupSync(groupId: Long): List<GroupMember>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GroupMember>)

    @Query("DELETE FROM group_members WHERE id = :memberId")
    suspend fun deleteMember(memberId: Long)

    @Query("SELECT * FROM group_members WHERE id = :memberId")
    suspend fun getMemberById(memberId: Long): GroupMember?

    // Expenses
    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getExpensesForGroup(groupId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId")
    suspend fun getExpensesForGroupSync(groupId: Long): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Long)

    @Query("DELETE FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun deleteSharesForExpense(expenseId: Long)

    // Shares
    @Query("SELECT * FROM expense_shares WHERE expenseId = :expenseId")
    suspend fun getSharesForExpense(expenseId: Long): List<ExpenseShare>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShares(shares: List<ExpenseShare>)

    @Query("SELECT * FROM expense_shares WHERE expenseId IN (SELECT id FROM expenses WHERE groupId = :groupId)")
    fun getSharesForGroup(groupId: Long): Flow<List<ExpenseShare>>

    @Transaction
    suspend fun deleteExpenseWithShares(expenseId: Long) {
        deleteSharesForExpense(expenseId)
        deleteExpenseById(expenseId)
    }
}
