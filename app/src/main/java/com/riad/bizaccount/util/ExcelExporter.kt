package com.riad.bizaccount.util

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a genuine, spec-valid .xlsx file using only java.util.zip -- no Apache POI.
 *
 * POI's "poi-ooxml" jar pulls in javax.xml/xmlbeans classes that are notoriously unreliable
 * on Android (missing classes at runtime, huge method count, R8 conflicts). Since the report
 * export here is a single flat sheet, writing the OOXML parts directly is far more robust
 * for a phone/tablet target and keeps the APK small.
 */
object ExcelExporter {

    fun export(outputFile: File, sheetName: String, header: List<String>, rows: List<List<String>>): File {
        outputFile.parentFile?.mkdirs()
        val sharedStrings = LinkedHashMap<String, Int>()
        fun stringIndex(s: String): Int = sharedStrings.getOrPut(s) { sharedStrings.size }

        // Pre-register all strings to build sharedStrings.xml in a single pass.
        header.forEach { stringIndex(it) }
        rows.forEach { r -> r.forEach { stringIndex(it) } }

        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            writeEntry(zip, "[Content_Types].xml", contentTypesXml())
            writeEntry(zip, "_rels/.rels", relsXml())
            writeEntry(zip, "xl/workbook.xml", workbookXml(sheetName))
            writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            writeEntry(zip, "xl/sharedStrings.xml", sharedStringsXml(sharedStrings))
            writeEntry(zip, "xl/worksheets/sheet1.xml", sheetXml(header, rows, sharedStrings))
        }
        return outputFile
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")

    private fun contentTypesXml() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
        </Types>
    """.trimIndent()

    private fun relsXml() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun workbookXml(sheetName: String) = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="${escape(sheetName)}" sheetId="1" r:id="rId1"/>
          </sheets>
        </workbook>
    """.trimIndent()

    private fun workbookRelsXml() = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
        </Relationships>
    """.trimIndent()

    private fun sharedStringsXml(strings: LinkedHashMap<String, Int>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.size}" uniqueCount="${strings.size}">""")
        strings.keys.forEach { s -> sb.append("<si><t xml:space=\"preserve\">${escape(s)}</t></si>") }
        sb.append("</sst>")
        return sb.toString()
    }

    private fun colLetter(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i >= 0) {
            sb.insert(0, ('A' + (i % 26)))
            i = i / 26 - 1
        }
        return sb.toString()
    }

    private fun sheetXml(header: List<String>, rows: List<List<String>>, strings: LinkedHashMap<String, Int>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")

        fun rowXml(rowIndex: Int, values: List<String>) {
            sb.append("<row r=\"$rowIndex\">")
            values.forEachIndexed { c, v ->
                val ref = "${colLetter(c)}$rowIndex"
                val idx = strings[v] ?: 0
                sb.append("<c r=\"$ref\" t=\"s\"><v>$idx</v></c>")
            }
            sb.append("</row>")
        }

        rowXml(1, header)
        rows.forEachIndexed { i, r -> rowXml(i + 2, r) }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }
}
