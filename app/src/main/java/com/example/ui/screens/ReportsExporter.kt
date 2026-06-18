package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Expense
import com.example.data.model.Group
import com.example.data.model.GroupMember
import com.example.data.model.GroupSummary
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object ReportsExporter {

    fun shareReportViaText(context: Context, group: Group, summary: GroupSummary?, members: List<GroupMember>, expenses: List<Expense>) {
        if (summary == null) {
            Toast.makeText(context, "No splitting data available to export.", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date(group.createdAt))

        // Create a beautiful, polished text report representation
        val report = StringBuilder()
        report.append("============ SAGAR SPLIT PRO ============\n")
        report.append("  Created by Sagar with Financial Precision\n")
        report.append("=========================================\n\n")
        report.append("Group: ${group.name}\n")
        report.append("Type: ${group.type}\n")
        report.append("Since: $dateString\n")
        report.append("Reporting Currency: ${group.currency}\n")
        report.append("Description: ${group.description}\n\n")
        report.append("-----------------------------------------\n")
        report.append("          FINANCIAL SUMMARY              \n")
        report.append("-----------------------------------------\n")
        report.append("Total Expenses: ${group.currency} ${String.format("%.2f", summary.totalExpenses)}\n")
        report.append("Cost Per Person: ${group.currency} ${String.format("%.2f", summary.perPersonExpense)}\n")
        report.append("Active Split Members: ${members.size}\n\n")

        report.append("-----------------------------------------\n")
        report.append("          MEMBER BALANCE STATS           \n")
        report.append("-----------------------------------------\n")
        summary.memberBalances.forEach { mb ->
            val indicator = if (mb.netBalance >= 0) "Receives: +" else "Owes: -"
            report.append("- ${mb.member.name}:\n")
            report.append("  * Paid: ${group.currency} ${String.format("%.2f", mb.totalPaid)}\n")
            report.append("  * Share: ${group.currency} ${String.format("%.2f", mb.totalOwed)}\n")
            report.append("  * $indicator ${group.currency} ${String.format("%.2f", Math.abs(mb.netBalance))}\n")
        }
        report.append("\n")

        report.append("-----------------------------------------\n")
        report.append("          SETTLEMENT REPORT               \n")
        report.append("-----------------------------------------\n")
        if (summary.settlements.isEmpty()) {
            report.append("All split accounts are perfectly settled!\n")
        } else {
            summary.settlements.forEach { st ->
                report.append("- ${st.debtor.name} pays ${group.currency} ${String.format("%.2f", st.amount)} to ${st.creditor.name}\n")
                if (st.creditor.upiId.isNotBlank()) {
                    report.append("  [UPI ID]: ${st.creditor.upiId}\n")
                }
            }
        }
        report.append("\n")

        report.append("-----------------------------------------\n")
        report.append("          DETAILED EXPENSE LEDGER        \n")
        report.append("-----------------------------------------\n")
        if (expenses.isEmpty()) {
            report.append("No expenses logged.\n")
        } else {
            expenses.forEachIndexed { idx, exp ->
                val payer = members.find { it.id == exp.paidByMemberId }?.name ?: "Unknown"
                val expDate = dateFormat.format(Date(exp.timestamp))
                report.append("${idx + 1}. [${exp.category}] ${exp.title}\n")
                report.append("   Amount: ${group.currency} ${String.format("%.2f", exp.amount)}\n")
                report.append("   Paid by: $payer on $expDate\n")
                report.append("   Split type: ${exp.splitType}\n")
            }
        }
        report.append("\n=========================================\n")
        report.append("Sagar Split Pro - Empowering Indian Users\n")
        report.append("=========================================\n")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, "${group.name} - Settlement Report")
            putExtra(Intent.EXTRA_TEXT, report.toString())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Sagar Split Pro Report")
        context.startActivity(shareIntent)
    }

    fun exportExcelCsvReport(context: Context, group: Group, summary: GroupSummary?, members: List<GroupMember>, expenses: List<Expense>) {
        if (summary == null || expenses.isEmpty()) {
            Toast.makeText(context, "No expense ledger available to export CSV.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val cachePath = File(context.cacheDir, "reports")
            cachePath.mkdirs()
            val csvFile = File(cachePath, "${group.name.replace(" ", "_")}_split_report.csv")
            val writer = FileWriter(csvFile)

            // Header blocks
            writer.append("Sagar Split Pro Settlement Ledger,,,,,,\n")
            writer.append("Created by Sagar,,,,,,\n")
            writer.append("Group,${group.name},Type,${group.type},Currency,${group.currency},\n\n")

            // Block 1: Overview
            writer.append("OVERVIEW,,,,,\n")
            writer.append("Total Group Spend,${summary.totalExpenses},Cost Per Person,${summary.perPersonExpense},,,\n\n")

            // Block 2: Members
            writer.append("MEMBER STATS,,,,,\n")
            writer.append("Member Name,Total Paid,Total Owed,Net Balance,UPI ID,,\n")
            summary.memberBalances.forEach { mb ->
                writer.append("${mb.member.name},${mb.totalPaid},${mb.totalOwed},${mb.netBalance},${mb.member.upiId},,\n")
            }
            writer.append("\n")

            // Block 3: Settlements
            writer.append("RECOMMENDED TRANSFERS,,,,,\n")
            writer.append("Debtor (Who pays),Creditor (Who gets),Amount,UPI Link,,,\n")
            summary.settlements.forEach { st ->
                val upiLink = "upi://pay?pa=${st.creditor.upiId}&pn=${st.creditor.name}&am=${st.amount}&cu=${group.currency}"
                writer.append("${st.debtor.name},${st.creditor.name},${st.amount},\"$upiLink\",,,\n")
            }
            writer.append("\n")

            // Block 4: Detailed transactions
            writer.append("EXPENSE REGISTER,,,,,\n")
            writer.append("Date,Title,Category,Amount,Paid By,Split Type,,\n")
            val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
            expenses.forEach { exp ->
                val payer = members.find { it.id == exp.paidByMemberId }?.name ?: "Unknown"
                val expDate = dateFormat.format(Date(exp.timestamp))
                writer.append("\"$expDate\",\"${exp.title.replace("\"", "\"\"")}\",${exp.category},${exp.amount},${payer},${exp.splitType},,\n")
            }

            writer.flush()
            writer.close()

            // Share file intent using authority
            val authority = "${context.packageName}.fileprovider"
            val fileUri: Uri = FileProvider.getUriForFile(context, authority, csvFile)

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "text/csv"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Open Excel Split CSV Report"))

        } catch (e: Exception) {
            Toast.makeText(context, "Excel export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
