package com.riad.bizaccount.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

data class ExportRow(
    val date: String,
    val type: String,
    val category: String,
    val description: String,
    val paymentMethod: String,
    val amount: String
)

data class ExportSummary(
    val businessName: String,
    val periodLabel: String,
    val totalIncome: String,
    val totalExpense: String,
    val netResult: String,
    val isProfit: Boolean,
    val transactionCount: Int
)

/**
 * Generates a multi-page A4 PDF report with a header, a summary block and a transaction table.
 * Uses android.graphics.pdf.PdfDocument directly -- no third-party PDF library needed.
 */
object PdfExporter {

    private const val PAGE_WIDTH = 595  // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val ROW_HEIGHT = 22f

    fun export(
        outputFile: File,
        summary: ExportSummary,
        rows: List<ExportRow>
    ): File {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; color = Color.BLACK }
        val subPaint = Paint().apply { textSize = 11f; color = Color.DKGRAY }
        val headerPaint = Paint().apply { textSize = 10f; isFakeBoldText = true; color = Color.WHITE }
        val cellPaint = Paint().apply { textSize = 9.5f; color = Color.BLACK }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }
        val headerBgPaint = Paint().apply { color = Color.rgb(27, 107, 74) }
        val profitPaint = Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.rgb(27, 138, 74) }
        val lossPaint = Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.rgb(198, 40, 40) }

        val colWidths = floatArrayOf(70f, 45f, 80f, 130f, 70f, 80f)
        val headers = listOf("তারিখ", "ধরন", "ক্যাটাগরি", "বিবরণ", "পদ্ধতি", "পরিমাণ")

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = drawHeader(canvas, summary, titlePaint, subPaint, profitPaint, lossPaint)

        // Table header
        y = drawTableHeader(canvas, y, colWidths, headers, headerPaint, headerBgPaint)

        for (row in rows) {
            if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN + 20f
                y = drawTableHeader(canvas, y, colWidths, headers, headerPaint, headerBgPaint)
            }
            y = drawRow(canvas, y, colWidths, row, cellPaint, linePaint)
        }

        document.finishPage(page)

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
        return outputFile
    }

    private fun drawHeader(
        canvas: Canvas,
        summary: ExportSummary,
        titlePaint: Paint,
        subPaint: Paint,
        profitPaint: Paint,
        lossPaint: Paint
    ): Float {
        var y = MARGIN + 10f
        canvas.drawText(summary.businessName.ifBlank { "হিসাব ম্যানেজার" }, MARGIN, y, titlePaint)
        y += 20f
        canvas.drawText(summary.periodLabel, MARGIN, y, subPaint)
        y += 22f
        canvas.drawText("মোট আয়: ${summary.totalIncome}", MARGIN, y, subPaint)
        y += 16f
        canvas.drawText("মোট ব্যয়: ${summary.totalExpense}", MARGIN, y, subPaint)
        y += 18f
        val resultLabel = if (summary.isProfit) "নিট লাভ: ${summary.netResult}" else "নিট ক্ষতি: ${summary.netResult}"
        canvas.drawText(resultLabel, MARGIN, y, if (summary.isProfit) profitPaint else lossPaint)
        y += 16f
        canvas.drawText("মোট লেনদেন: ${summary.transactionCount}", MARGIN, y, subPaint)
        y += 20f
        return y
    }

    private fun drawTableHeader(
        canvas: Canvas,
        startY: Float,
        colWidths: FloatArray,
        headers: List<String>,
        headerPaint: Paint,
        bgPaint: Paint
    ): Float {
        var x = MARGIN
        val rect = RectF(MARGIN, startY, MARGIN + colWidths.sum(), startY + ROW_HEIGHT)
        canvas.drawRect(rect, bgPaint)
        headers.forEachIndexed { i, h ->
            canvas.drawText(h, x + 4f, startY + ROW_HEIGHT - 7f, headerPaint)
            x += colWidths[i]
        }
        return startY + ROW_HEIGHT
    }

    private fun drawRow(
        canvas: Canvas,
        startY: Float,
        colWidths: FloatArray,
        row: ExportRow,
        cellPaint: Paint,
        linePaint: Paint
    ): Float {
        var x = MARGIN
        val values = listOf(row.date, row.type, row.category, row.description, row.paymentMethod, row.amount)
        values.forEachIndexed { i, v ->
            canvas.drawText(v.take(28), x + 4f, startY + ROW_HEIGHT - 7f, cellPaint)
            x += colWidths[i]
        }
        canvas.drawLine(MARGIN, startY + ROW_HEIGHT, MARGIN + colWidths.sum(), startY + ROW_HEIGHT, linePaint)
        return startY + ROW_HEIGHT
    }
}
