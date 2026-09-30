package com.reddy.vittify.data.pdf

import android.content.Context
import android.graphics.Color
import com.reddy.vittify.data.database.entity.AccountBalanceEntity
import com.reddy.vittify.data.database.entity.TransactionEntity
import com.reddy.vittify.data.database.entity.TransactionType
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Singleton
class TransactionPdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    init {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
    }

    companion object {
        private val A4 = PDRectangle.A4
        private val BOLD get() = PDType1Font.HELVETICA_BOLD
        private val REGULAR get() = PDType1Font.HELVETICA

        // Brand colours (RGB 0-1 floats)
        private val BRAND_PRIMARY = floatArrayOf(0.294f, 0.431f, 0.961f)   // #4B6EF5
        private val BRAND_SECONDARY = floatArrayOf(0.533f, 0.282f, 0.906f) // #8848E7
        private val INCOME_COLOR = floatArrayOf(0.133f, 0.545f, 0.133f)
        private val EXPENSE_COLOR = floatArrayOf(0.824f, 0.118f, 0.118f)
        private val BG_LIGHT = floatArrayOf(0.973f, 0.969f, 1.0f)
        private val BG_ROW_ALT = floatArrayOf(0.95f, 0.95f, 1.0f)
        private val HEADER_BG = floatArrayOf(0.2f, 0.267f, 0.486f)
        private val TEXT_DARK = floatArrayOf(0.1f, 0.1f, 0.1f)
        private val TEXT_MUTED = floatArrayOf(0.5f, 0.5f, 0.5f)

        private val CATEGORY_COLORS = listOf(
            floatArrayOf(0.294f, 0.431f, 0.961f),
            floatArrayOf(0.918f, 0.357f, 0.349f),
            floatArrayOf(0.196f, 0.682f, 0.494f),
            floatArrayOf(0.992f, 0.706f, 0.227f),
            floatArrayOf(0.533f, 0.282f, 0.906f),
            floatArrayOf(0.0f, 0.706f, 0.871f),
            floatArrayOf(0.984f, 0.502f, 0.255f),
            floatArrayOf(0.482f, 0.749f, 0.337f),
        )

        private val DT_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy")
        private val SHORT_DATE = DateTimeFormatter.ofPattern("dd MMM yy")

        private const val MARGIN = 36f
        private const val PAGE_W = 595f  // A4 portrait width
        private const val PAGE_H = 841f  // A4 portrait height
        private val PAGE_SIZE = PDRectangle(PAGE_W, PAGE_H)
    }

    // ─── Public entry point ──────────────────────────────────────────────────

    fun generate(
        outputFile: File,
        accounts: List<AccountBalanceEntity>,
        transactions: List<TransactionEntity>,
        startDate: LocalDate,
        endDate: LocalDate,
        reportTitle: String = "Transaction Report"
    ) {
        com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
        val doc = PDDocument()
        try {
            val sorted = transactions
                .filter { !it.isDeleted }
                .sortedByDescending { it.dateTime }

            // Compute per-account summaries
            val summaries = buildAccountSummaries(accounts, sorted, startDate, endDate)

            // Page 1: Cover + Account Cards
            addCoverPage(doc, summaries, startDate, endDate, reportTitle)

            // Page 2: Category pie chart
            val catBreakdown = buildCategoryBreakdown(sorted)
            if (catBreakdown.isNotEmpty()) {
                addCategoryPage(doc, catBreakdown)
            }

            // Page 3+: Transaction ledger
            addTransactionPages(doc, sorted, summaries)

            doc.save(outputFile)
        } finally {
            doc.close()
        }
    }

    // ─── Account Summary ─────────────────────────────────────────────────────

    data class AccountSummary(
        val entity: AccountBalanceEntity,
        val openingBalance: BigDecimal,
        val closingBalance: BigDecimal,
        val totalIncome: BigDecimal,
        val totalExpense: BigDecimal,
        val txCount: Int
    )

    private fun buildAccountSummaries(
        accounts: List<AccountBalanceEntity>,
        transactions: List<TransactionEntity>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<AccountSummary> = accounts.map { acc ->
        val accTx = transactions.filter {
            it.accountId == acc.id.toString()
        }
        val income = accTx.filter { it.transactionType == TransactionType.INCOME }
            .fold(BigDecimal.ZERO) { s, t -> s + t.amount }
        val expense = accTx.filter { it.transactionType == TransactionType.EXPENSE }
            .fold(BigDecimal.ZERO) { s, t -> s + t.amount }
        AccountSummary(
            entity = acc,
            openingBalance = acc.balance - income + expense,
            closingBalance = acc.balance,
            totalIncome = income,
            totalExpense = expense,
            txCount = accTx.size
        )
    }

    private fun buildCategoryBreakdown(transactions: List<TransactionEntity>): Map<String, BigDecimal> =
        transactions.filter { it.transactionType == TransactionType.EXPENSE }
            .groupBy { it.category.ifBlank { "Others" } }
            .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { s, t -> s + t.amount } }
            .entries.sortedByDescending { it.value }
            .take(8)
            .associate { it.key to it.value }

    // ─── Page 1: Cover ───────────────────────────────────────────────────────

    private fun addCoverPage(
        doc: PDDocument,
        summaries: List<AccountSummary>,
        startDate: LocalDate,
        endDate: LocalDate,
        title: String
    ) {
        val page = PDPage(PAGE_SIZE)
        doc.addPage(page)
        PDPageContentStream(doc, page).use { cs ->
            // Background
            fillRect(cs, 0f, 0f, PAGE_W, PAGE_H, BG_LIGHT)

            // Left sidebar gradient-like block
            fillRect(cs, 0f, 0f, 6f, PAGE_H, BRAND_PRIMARY)
            fillRect(cs, 6f, 0f, 3f, PAGE_H, BRAND_SECONDARY)

            // Header bar
            fillRect(cs, 9f, PAGE_H - 60f, PAGE_W - 9f, 60f, HEADER_BG)

            // Brand name
            drawText(cs, "VITTIFY", BOLD, 22f, 18f, PAGE_H - 30f, floatArrayOf(1f, 1f, 1f))
            drawText(cs, "Financial Intelligence", REGULAR, 9f, 18f, PAGE_H - 44f, floatArrayOf(0.8f, 0.8f, 1f))

            // Report title
            drawText(cs, title, BOLD, 18f, PAGE_W / 2 - 80f, PAGE_H - 32f, floatArrayOf(1f, 1f, 1f))
            val rangeStr = "${startDate.format(DATE_FORMATTER)}  –  ${endDate.format(DATE_FORMATTER)}"
            drawText(cs, rangeStr, REGULAR, 9f, PAGE_W / 2 - 80f, PAGE_H - 48f, floatArrayOf(0.8f, 0.8f, 1f))

            // Generated at (right)
            val genStr = "Generated: ${LocalDateTime.now().format(DT_FORMATTER)}"
            drawText(cs, genStr, REGULAR, 8f, PAGE_W - MARGIN - 180f, PAGE_H - 32f, floatArrayOf(0.8f, 0.8f, 1f))

            // Section header
            drawText(cs, "ACCOUNT SUMMARIES", BOLD, 9f, MARGIN, PAGE_H - 100f, BRAND_PRIMARY)
            drawLine(cs, MARGIN, PAGE_H - 104f, PAGE_W - MARGIN, PAGE_H - 104f, TEXT_MUTED, 0.5f)

            // Account cards
            val cardW = 160f
            val cardH = 150f
            val spacingX = 15f
            val spacingY = 15f
            val maxCols = 3
            
            val colsCount = minOf(summaries.size, maxCols)
            val startX = (PAGE_W - (colsCount * cardW + (colsCount - 1) * spacingX)) / 2

            summaries.forEachIndexed { i, summary ->
                val col = i % maxCols
                val row = i / maxCols
                val x = startX + col * (cardW + spacingX)
                val y = PAGE_H - 120f - cardH - row * (cardH + spacingY)
                drawAccountCard(cs, summary, x, y, cardW, cardH)
            }

            // Totals row
            val totalIncome = summaries.fold(BigDecimal.ZERO) { s, a -> s + a.totalIncome }
            val totalExpense = summaries.fold(BigDecimal.ZERO) { s, a -> s + a.totalExpense }
            val net = totalIncome - totalExpense
            val netColor = if (net >= BigDecimal.ZERO) INCOME_COLOR else EXPENSE_COLOR

            val totalRows = (summaries.size + maxCols - 1) / maxCols
            val yTotals = PAGE_H - 120f - totalRows * (cardH + spacingY) - 10f
            
            fillRect(cs, MARGIN, yTotals - 14f, PAGE_W - MARGIN * 2, 26f, HEADER_BG)
            drawText(cs, "OVERALL  •  Income: ${formatAmt(totalIncome)}   Expense: ${formatAmt(totalExpense)}   Net: ${formatAmt(net)}", BOLD, 8.5f, MARGIN + 8f, yTotals, floatArrayOf(1f, 1f, 1f))

            // Footer
            drawFooter(cs, 1, -1)
        }
    }

    private fun drawAccountCard(
        cs: PDPageContentStream,
        summary: AccountSummary,
        x: Float, y: Float, w: Float, h: Float
    ) {
        // Card shadow
        fillRect(cs, x + 2f, y - 2f, w, h, floatArrayOf(0.85f, 0.85f, 0.9f))
        // Card background
        fillRect(cs, x, y, w, h, floatArrayOf(1f, 1f, 1f))
        // Top accent
        fillRect(cs, x, y + h - 6f, w, 6f, BRAND_PRIMARY)

        val acc = summary.entity
        val name = acc.bankName.let { if (it.length > 18) it.take(15) + "…" else it }
        val acctNo = "••• ${acc.accountLast4}"
        val labelX = x + 8f
        var ty = y + h - 20f

        drawText(cs, name, BOLD, 9f, labelX, ty, TEXT_DARK); ty -= 12f
        drawText(cs, acctNo, REGULAR, 7.5f, labelX, ty, TEXT_MUTED); ty -= 16f

        // Divider
        drawLine(cs, x + 4f, ty + 4f, x + w - 4f, ty + 4f, TEXT_MUTED, 0.4f); ty -= 10f

        // Balance rows
        val rows = listOf(
            Triple("Opening", summary.openingBalance, TEXT_DARK),
            Triple("Closing", summary.closingBalance, TEXT_DARK),
            Triple("Income", summary.totalIncome, INCOME_COLOR),
            Triple("Expense", summary.totalExpense, EXPENSE_COLOR),
        )
        rows.forEach { (lbl, amt, color) ->
            drawText(cs, lbl, REGULAR, 7f, labelX, ty, TEXT_MUTED)
            drawText(cs, formatAmt(amt), BOLD, 7.5f, x + w - 8f - amtWidth(amt), ty, color)
            ty -= 13f
        }
        // Tx count
        drawText(cs, "${summary.txCount} transactions", REGULAR, 6.5f, labelX, ty, TEXT_MUTED)
    }

    // ─── Page 2: Category Breakdown ─────────────────────────────────────────

    private fun addCategoryPage(doc: PDDocument, breakdown: Map<String, BigDecimal>) {
        val page = PDPage(PAGE_SIZE)
        doc.addPage(page)
        PDPageContentStream(doc, page).use { cs ->
            fillRect(cs, 0f, 0f, PAGE_W, PAGE_H, BG_LIGHT)
            fillRect(cs, 0f, 0f, 6f, PAGE_H, BRAND_PRIMARY)

            // Header bar
            fillRect(cs, 9f, PAGE_H - 50f, PAGE_W - 9f, 50f, HEADER_BG)
            drawText(cs, "VITTIFY  •  Spending by Category", BOLD, 14f, 18f, PAGE_H - 32f, floatArrayOf(1f, 1f, 1f))

            val total = breakdown.values.fold(BigDecimal.ZERO) { s, v -> s + v }
            val cx = PAGE_W / 2f
            val cy = PAGE_H - 220f
            val r = 130f

            drawPieChart(cs, breakdown, total, cx, cy, r)
            
            val legendX = (PAGE_W - 240f) / 2f
            val legendY = cy - r - 40f
            drawCategoryLegend(cs, breakdown, total, legendX, legendY)

            drawFooter(cs, 2, -1)
        }
    }

    private fun drawPieChart(
        cs: PDPageContentStream,
        breakdown: Map<String, BigDecimal>,
        total: BigDecimal,
        cx: Float, cy: Float, r: Float
    ) {
        if (total == BigDecimal.ZERO) return
        var startAngle = Math.PI / 2
        breakdown.entries.forEachIndexed { idx, (_, value) ->
            val sweep = (value.toDouble() / total.toDouble()) * 2 * Math.PI
            val color = CATEGORY_COLORS[idx % CATEGORY_COLORS.size]
            drawPieSlice(cs, cx, cy, r, startAngle, sweep, color)
            startAngle += sweep
        }
        // Inner donut hole
        fillCircle(cs, cx, cy, r * 0.48f, BG_LIGHT)
        // Center text
        drawText(cs, "Total", REGULAR, 8f, cx - 13f, cy + 6f, TEXT_MUTED)
        drawText(cs, formatAmt(total), BOLD, 9f, cx - 22f, cy - 8f, TEXT_DARK)
    }

    private fun drawPieSlice(
        cs: PDPageContentStream,
        cx: Float, cy: Float, r: Float,
        startAngle: Double, sweep: Double,
        color: FloatArray
    ) {
        cs.setNonStrokingColor(color[0], color[1], color[2])
        val steps = (sweep * 20 / Math.PI).toInt().coerceAtLeast(4)
        cs.moveTo(cx, cy)
        for (i in 0..steps) {
            val angle = startAngle + sweep * i / steps
            cs.lineTo((cx + r * cos(angle)).toFloat(), (cy + r * sin(angle)).toFloat())
        }
        cs.closePath()
        cs.fill()
        cs.setStrokingColor(1f, 1f, 1f)
        cs.setLineWidth(1f)
        cs.moveTo(cx, cy)
        cs.lineTo((cx + r * cos(startAngle)).toFloat(), (cy + r * sin(startAngle)).toFloat())
        cs.stroke()
    }

    private fun fillCircle(cs: PDPageContentStream, cx: Float, cy: Float, r: Float, color: FloatArray) {
        cs.setNonStrokingColor(color[0], color[1], color[2])
        val k = 0.5523f
        cs.moveTo(cx, cy + r)
        cs.curveTo(cx + k * r, cy + r, cx + r, cy + k * r, cx + r, cy)
        cs.curveTo(cx + r, cy - k * r, cx + k * r, cy - r, cx, cy - r)
        cs.curveTo(cx - k * r, cy - r, cx - r, cy - k * r, cx - r, cy)
        cs.curveTo(cx - r, cy + k * r, cx - k * r, cy + r, cx, cy + r)
        cs.fill()
    }

    private fun drawCategoryLegend(
        cs: PDPageContentStream,
        breakdown: Map<String, BigDecimal>,
        total: BigDecimal,
        x: Float, y: Float
    ) {
        drawText(cs, "Category Breakdown", BOLD, 11f, x, y, TEXT_DARK)
        var ty = y - 18f
        breakdown.entries.forEachIndexed { idx, (cat, amt) ->
            val color = CATEGORY_COLORS[idx % CATEGORY_COLORS.size]
            fillRect(cs, x, ty, 10f, 10f, color)
            val pct = if (total > BigDecimal.ZERO) amt.multiply(BigDecimal(100)).divide(total, 1, RoundingMode.HALF_UP) else BigDecimal.ZERO
            val catLabel = if (cat.length > 24) cat.take(21) + "…" else cat
            drawText(cs, catLabel, REGULAR, 8.5f, x + 14f, ty + 1f, TEXT_DARK)
            drawText(cs, "${formatAmt(amt)}  ($pct%)", BOLD, 8f, x + 220f, ty + 1f, TEXT_DARK)
            ty -= 18f
        }
    }

    // ─── Page 3+: Transaction Ledger ────────────────────────────────────────

    private fun addTransactionPages(
        doc: PDDocument,
        transactions: List<TransactionEntity>,
        summaries: List<AccountSummary>
    ) {
        val accountsMap = summaries.associate { it.entity.id.toString() to it.entity }
        val cols = listOf(
            Col("#", 25f), Col("Date & Time", 70f), Col("Transaction", 110f),
            Col("Category", 90f), Col("Income", 55f), Col("Expense", 55f),
            Col("Account", 118f)
        )
        val tableW = cols.sumOf { it.w.toDouble() }.toFloat()
        val startX = (PAGE_W - tableW) / 2
        val headerH = 45f
        val rowH = 16f
        val pageRows = ((PAGE_H - headerH - MARGIN * 2 - 20f) / rowH).toInt()

        val totalPages = (transactions.size + pageRows - 1) / pageRows.coerceAtLeast(1) + 2

        transactions.chunked(pageRows.coerceAtLeast(1)).forEachIndexed { pageIdx, chunk ->
            val page = PDPage(PAGE_SIZE)
            doc.addPage(page)
            PDPageContentStream(doc, page).use { cs ->
                fillRect(cs, 0f, 0f, PAGE_W, PAGE_H, BG_LIGHT)
                fillRect(cs, 0f, 0f, 6f, PAGE_H, BRAND_PRIMARY)

                // Header
                fillRect(cs, 9f, PAGE_H - headerH, PAGE_W - 9f, headerH, HEADER_BG)
                drawText(cs, "VITTIFY  •  Transaction Ledger", BOLD, 13f, 18f, PAGE_H - 28f, floatArrayOf(1f, 1f, 1f))
                val subTxt = "Page ${pageIdx + 3} of $totalPages"
                drawText(cs, subTxt, REGULAR, 8f, PAGE_W - MARGIN - 70f, PAGE_H - 28f, floatArrayOf(0.8f, 0.8f, 1f))

                // Column headers
                val colHeaderY = PAGE_H - headerH - 18f
                fillRect(cs, startX, colHeaderY - 4f, tableW, 20f, BRAND_PRIMARY)
                var colX = startX
                cols.forEach { col ->
                    drawText(cs, col.label, BOLD, 7f, colX + 3f, colHeaderY, floatArrayOf(1f, 1f, 1f))
                    colX += col.w
                }

                // Rows
                var rowY = colHeaderY - rowH
                chunk.forEachIndexed { rowIdx, tx ->
                    val globalIdx = pageIdx * pageRows + rowIdx
                    val isAlt = rowIdx % 2 == 1
                    if (isAlt) fillRect(cs, startX, rowY - 4f, tableW, rowH, BG_ROW_ALT)

                    colX = startX
                    val isIncome = tx.transactionType == TransactionType.INCOME
                    val isExpense = tx.transactionType == TransactionType.EXPENSE

                    // #
                    drawText(cs, "${globalIdx + 1}", REGULAR, 7f, colX + 3f, rowY, TEXT_MUTED); colX += cols[0].w
                    // Date
                    drawText(cs, tx.dateTime.format(SHORT_DATE) + "\n" + tx.dateTime.format(DateTimeFormatter.ofPattern("HH:mm")), REGULAR, 7f, colX + 2f, rowY, TEXT_DARK); colX += cols[1].w
                    // Merchant
                    val merchant = tx.merchantName.let { if (it.length > 20) it.take(18) + "…" else it }
                    drawText(cs, merchant, BOLD, 7f, colX + 2f, rowY, TEXT_DARK); colX += cols[2].w
                    // Category
                    val cat = buildString {
                        append(tx.category.let { if (it.length > 12) it.take(10) + "…" else it })
                        if (!tx.subcategory.isNullOrBlank()) append("\n${tx.subcategory.let { if (it.length > 12) it.take(10)+"…" else it }}")
                    }
                    drawText(cs, cat, REGULAR, 6.5f, colX + 2f, rowY, TEXT_MUTED); colX += cols[3].w
                    // Income
                    if (isIncome) drawText(cs, formatAmt(tx.amount), BOLD, 7f, colX + 2f, rowY, INCOME_COLOR)
                    colX += cols[4].w
                    // Expense
                    if (isExpense) drawText(cs, formatAmt(tx.amount), BOLD, 7f, colX + 2f, rowY, EXPENSE_COLOR)
                    colX += cols[5].w
                    // Account
                    val account = tx.accountId?.let { accountsMap[it] }
                    val acct = "${account?.bankName?.take(12) ?: "—"}\n••${account?.accountLast4 ?: tx.fromAccount?.takeLast(4) ?: "—"}"
                    drawText(cs, acct, REGULAR, 6.5f, colX + 2f, rowY, TEXT_MUTED); colX += cols[6].w

                    // Row bottom border
                    drawLine(cs, startX, rowY - 5f, startX + tableW, rowY - 5f, floatArrayOf(0.88f, 0.88f, 0.95f), 0.3f)
                    rowY -= rowH
                }

                drawFooter(cs, pageIdx + 3, totalPages)
            }
        }
    }

    // ─── Drawing helpers ─────────────────────────────────────────────────────

    private data class Col(val label: String, val w: Float)

    private fun fillRect(cs: PDPageContentStream, x: Float, y: Float, w: Float, h: Float, color: FloatArray) {
        cs.setNonStrokingColor(color[0], color[1], color[2])
        cs.addRect(x, y, w, h)
        cs.fill()
    }

    private fun drawText(cs: PDPageContentStream, text: String, font: PDType1Font, size: Float, x: Float, y: Float, color: FloatArray) {
        runCatching {
            cs.beginText()
            cs.setNonStrokingColor(color[0], color[1], color[2])
            cs.setFont(font, size)
            cs.newLineAtOffset(x, y)
            val safe = text.lines().first().filter { it.code < 256 }
            cs.showText(safe)
            cs.endText()
        }
    }

    private fun drawLine(cs: PDPageContentStream, x1: Float, y1: Float, x2: Float, y2: Float, color: FloatArray, width: Float) {
        cs.setStrokingColor(color[0], color[1], color[2])
        cs.setLineWidth(width)
        cs.moveTo(x1, y1)
        cs.lineTo(x2, y2)
        cs.stroke()
    }

    private fun drawFooter(cs: PDPageContentStream, pageNum: Int, totalPages: Int) {
        fillRect(cs, 0f, 0f, PAGE_W, 20f, HEADER_BG)
        drawText(cs, "Vittify · Confidential Financial Report", REGULAR, 6.5f, MARGIN, 7f, floatArrayOf(0.7f, 0.7f, 0.9f))
        val pageStr = if (totalPages > 0) "Page $pageNum of $totalPages" else "Page $pageNum"
        drawText(cs, pageStr, REGULAR, 6.5f, PAGE_W - MARGIN - 60f, 7f, floatArrayOf(0.7f, 0.7f, 0.9f))
    }

    private fun formatAmt(amount: BigDecimal): String {
        val abs = amount.abs().setScale(2, RoundingMode.HALF_UP)
        return if (amount < BigDecimal.ZERO) "-₹${abs.toPlainString()}" else "₹${abs.toPlainString()}"
    }

    private fun amtWidth(amount: BigDecimal): Float = formatAmt(amount).length * 4.5f
}
