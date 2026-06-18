package com.example.data.repository

import com.example.data.local.SplitDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class SplitRepository(private val dao: SplitDao) {

    val allGroups: Flow<List<Group>> = dao.getAllGroups()

    suspend fun getGroupById(groupId: Long): Group? = dao.getGroupById(groupId)

    suspend fun createGroup(group: Group): Long = dao.insertGroup(group)

    suspend fun updateGroup(group: Group) = dao.updateGroup(group)

    suspend fun deleteGroup(group: Group) = dao.deleteGroup(group)

    // Members
    fun getMembersForGroup(groupId: Long): Flow<List<GroupMember>> = dao.getMembersForGroup(groupId)

    suspend fun getMembersForGroupSync(groupId: Long): List<GroupMember> = dao.getMembersForGroupSync(groupId)

    suspend fun addMember(member: GroupMember): Long = dao.insertMember(member)

    suspend fun addMembers(members: List<GroupMember>) = dao.insertMembers(members)

    suspend fun deleteMember(memberId: Long) = dao.deleteMember(memberId)

    suspend fun getMemberById(memberId: Long): GroupMember? = dao.getMemberById(memberId)

    // Expenses
    fun getExpensesForGroup(groupId: Long): Flow<List<Expense>> = dao.getExpensesForGroup(groupId)

    suspend fun getExpensesForGroupSync(groupId: Long): List<Expense> = dao.getExpensesForGroupSync(groupId)

    suspend fun addExpenseWithShares(expense: Expense, shares: List<ExpenseShare>): Long {
        val expenseId = dao.insertExpense(expense)
        val sharesWithId = shares.map { it.copy(expenseId = expenseId) }
        dao.insertShares(sharesWithId)
        return expenseId
    }

    suspend fun deleteExpense(expenseId: Long) = dao.deleteExpenseWithShares(expenseId)

    // Shares
    suspend fun getSharesForExpense(expenseId: Long): List<ExpenseShare> = dao.getSharesForExpense(expenseId)

    fun getSharesForGroup(groupId: Long): Flow<List<ExpenseShare>> = dao.getSharesForGroup(groupId)

    // Prepopulate database with Sagar Split Pro specific sample entries if empty
    suspend fun prepopulateSampleData() {
        val existingGroups = dao.getAllGroups().firstOrNull() ?: emptyList()
        if (existingGroups.isNotEmpty()) return

        // 1. Create Room split group
        val roomGroupId = dao.insertGroup(
            Group(
                name = "Flat 302, Green Glen",
                type = "ROOM",
                description = "Monthly roommates rental & grocery splits",
                currency = "INR",
                targetBudget = 45000.0,
                rentDueDate = 5L // 5th of every month
            )
        )

        // Add members
        val roomMembers = listOf(
            GroupMember(groupId = roomGroupId, name = "Sagar", upiId = "sagar@ybl", phone = "+91 98765 43210"),
            GroupMember(groupId = roomGroupId, name = "Rahul", upiId = "rahul@paytm", phone = "+91 91234 56789"),
            GroupMember(groupId = roomGroupId, name = "Amit", upiId = "amit@okhdfc", phone = "+91 88776 65544"),
            GroupMember(groupId = roomGroupId, name = "Meera", upiId = "meera@okaxis", phone = "+91 76543 21098")
        )
        dao.insertMembers(roomMembers)

        val insertedRoomMembers = dao.getMembersForGroupSync(roomGroupId)
        val sagarObj = insertedRoomMembers.find { it.name == "Sagar" }!!
        val rahulObj = insertedRoomMembers.find { it.name == "Rahul" }!!
        val amitObj = insertedRoomMembers.find { it.name == "Amit" }!!
        val meeraObj = insertedRoomMembers.find { it.name == "Meera" }!!

        // Add Room expenses
        // 1. House Rent paid by Sagar
        val rentExpenseId = dao.insertExpense(
            Expense(
                groupId = roomGroupId,
                title = "Monthly Apartment Rent",
                amount = 28000.0,
                paidByMemberId = sagarObj.id,
                category = "Rent",
                timestamp = System.currentTimeMillis() - 86400000 * 7, // 7 days ago
                splitType = "EQUAL"
            )
        )
        dao.insertShares(
            insertedRoomMembers.map { ExpenseShare(expenseId = rentExpenseId, memberId = it.id, shareAmount = 7000.0) }
        )

        // 2. Internet Bill paid by Meera
        val wifiExpenseId = dao.insertExpense(
            Expense(
                groupId = roomGroupId,
                title = "Act Fiber internet 300Mbps",
                amount = 1200.0,
                paidByMemberId = meeraObj.id,
                category = "Internet Bill",
                timestamp = System.currentTimeMillis() - 86400000 * 5, // 5 days ago
                splitType = "EQUAL"
            )
        )
        dao.insertShares(
            insertedRoomMembers.map { ExpenseShare(expenseId = wifiExpenseId, memberId = it.id, shareAmount = 300.0) }
        )

        // 3. Maid Charges paid by Rahul
        val maidExpenseId = dao.insertExpense(
            Expense(
                groupId = roomGroupId,
                title = "Maid Salary & Cooking",
                amount = 6000.0,
                paidByMemberId = rahulObj.id,
                category = "Maid Charges",
                timestamp = System.currentTimeMillis() - 86400000 * 3, // 3 days ago
                splitType = "EQUAL"
            )
        )
        dao.insertShares(
            insertedRoomMembers.map { ExpenseShare(expenseId = maidExpenseId, memberId = it.id, shareAmount = 1500.0) }
        )

        // 4. Groceries bought by Amit (Custom Split!)
        val groceryExpenseId = dao.insertExpense(
            Expense(
                groupId = roomGroupId,
                title = "Weekly Grocery - DMart",
                amount = 4000.0,
                paidByMemberId = amitObj.id,
                category = "Groceries",
                timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
                splitType = "CUSTOM",
                rawSplitDetails = "Custom shared proportions"
            )
        )
        dao.insertShares(
            listOf(
                ExpenseShare(expenseId = groceryExpenseId, memberId = sagarObj.id, shareAmount = 1200.0),
                ExpenseShare(expenseId = groceryExpenseId, memberId = rahulObj.id, shareAmount = 800.0),
                ExpenseShare(expenseId = groceryExpenseId, memberId = amitObj.id, shareAmount = 1000.0),
                ExpenseShare(expenseId = groceryExpenseId, memberId = meeraObj.id, shareAmount = 1000.0)
            )
        )


        // 2. Create Trip group
        val tripGroupId = dao.insertGroup(
            Group(
                name = "Manali Adventure Tour",
                type = "TRIP",
                description = "Snow biking, cottage rentals and trekking in Manali",
                currency = "INR",
                targetBudget = 80000.0
            )
        )

        val tripMembers = listOf(
            GroupMember(groupId = tripGroupId, name = "Sagar", upiId = "sagar@ybl", phone = "+91 98765 43210"),
            GroupMember(groupId = tripGroupId, name = "Amit", upiId = "amit@okhdfc", phone = "+91 88776 65544"),
            GroupMember(groupId = tripGroupId, name = "Karan", upiId = "karan@paytm", phone = "+91 99911 22233")
        )
        dao.insertMembers(tripMembers)

        val insertedTripMembers = dao.getMembersForGroupSync(tripGroupId)
        val tSagar = insertedTripMembers.find { it.name == "Sagar" }!!
        val tAmit = insertedTripMembers.find { it.name == "Amit" }!!
        val tKaran = insertedTripMembers.find { it.name == "Karan" }!!

        // Add Trip expenses
        // 1. Hotel booking
        val hotelId = dao.insertExpense(
            Expense(
                groupId = tripGroupId,
                title = "Riverside Cottage Stay",
                amount = 18000.0,
                paidByMemberId = tSagar.id,
                category = "Hotel",
                timestamp = System.currentTimeMillis() - 86400000 * 2,
                splitType = "EQUAL"
            )
        )
        dao.insertShares(
            insertedTripMembers.map { ExpenseShare(expenseId = hotelId, memberId = it.id, shareAmount = 6000.0) }
        )

        // 2. Paragliding activity (Only Karan & Sagar did Paragliding!)
        val activityId = dao.insertExpense(
            Expense(
                groupId = tripGroupId,
                title = "Solang Valley Paragliding",
                amount = 7000.0,
                paidByMemberId = tKaran.id,
                category = "Activities",
                timestamp = System.currentTimeMillis() - 86400000,
                splitType = "CUSTOM"
            )
        )
        dao.insertShares(
            listOf(
                ExpenseShare(expenseId = activityId, memberId = tSagar.id, shareAmount = 3500.0),
                ExpenseShare(expenseId = activityId, memberId = tKaran.id, shareAmount = 3500.0),
                ExpenseShare(expenseId = activityId, memberId = tAmit.id, shareAmount = 0.0)
            )
        )
    }
}
