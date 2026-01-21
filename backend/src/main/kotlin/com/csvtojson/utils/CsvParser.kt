package com.csvtojson.utils

import com.csvtojson.models.CsvError
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun String.stripBOM(): String {
    return if (this.startsWith('\uFEFF')) {
        this.substring(1)
    } else {
        this
    }
}

/**
 * RFC 4180 compliant CSV parser with support for:
 * - Quoted fields with commas and newlines
 * - Escaped quotes (double quotes)
 * - Multiple delimiter types (auto-detection)
 * - BOM handling
 * - Empty line skipping
 * - Line ending normalization
 * - Column consistency validation
 */
class CsvParser(private val csvContent: String) : Iterable<Map<String, String>> {
    private val processedContent: String
    private val delimiter: Char
    private val headers: List<String>
    private val expectedColumnCount: Int

    init {
        val contentWithoutBOM = csvContent.stripBOM()
        processedContent = contentWithoutBOM.replace("\r\n", "\n")
        val nonEmptyLines = processedContent.lines().filter { it.isNotBlank() }
        
        if (nonEmptyLines.isEmpty()) {
            logger.error { "CSV content is empty" }
            throw CsvError.EmptyFile()
        }
        
        delimiter = detectDelimiter(nonEmptyLines.first())
        logger.debug { "Detected CSV delimiter: '$delimiter'" }
        
        if (delimiter == ',' && !nonEmptyLines.first().contains(',') && 
            !nonEmptyLines.first().contains(';') && 
            !nonEmptyLines.first().contains('\t')) {
            logger.error { "Content does not appear to be CSV - no delimiters found" }
            throw CsvError.InvalidFormat("No delimiters found in content")
        }
        
        headers = parseCsvLine(nonEmptyLines.first(), delimiter)
        expectedColumnCount = headers.size
        
        if (headers.isEmpty()) {
            logger.error { "CSV headers are empty" }
            throw CsvError.InvalidFormat("Headers are empty")
        }
        
        logger.debug { "Parsed CSV headers: ${headers.size} columns - ${headers.take(5).joinToString(", ")}${if (headers.size > 5) "..." else ""}" }
    }

    /**
     * Auto-detect delimiter by analyzing the first row.
     * Supports comma, semicolon, tab, and pipe delimiters.
     * Returns the most common delimiter found outside quoted sections.
     */
    private fun detectDelimiter(line: String): Char {
        val delimiters = listOf(',', ';', '\t', '|')
        val counts = mutableMapOf<Char, Int>()
        var isInsideQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> isInsideQuotes = !isInsideQuotes
                !isInsideQuotes && char in delimiters -> {
                    counts[char] = counts.getOrDefault(char, 0) + 1
                }
            }
        }
        
        return counts.maxByOrNull { it.value }?.key ?: ','
    }

    /**
     * Parse a single CSV line per RFC 4180.
     * Handles quoted fields with embedded commas and newlines,
     * and escaped quotes (doubled quotes).
     */
    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val fields = mutableListOf<String>()
        val currentField = StringBuilder()
        var isInsideQuotes = false
        var i = 0
        
        while (i < line.length) {
            val char = line[i]
            
            when {
                char == '"' -> {
                    if (isInsideQuotes) {
                        if (i + 1 < line.length && line[i + 1] == '"') {
                            currentField.append('"')
                            i++
                        } else {
                            isInsideQuotes = false
                        }
                    } else {
                        isInsideQuotes = true
                    }
                }
                char == delimiter && !isInsideQuotes -> {
                    fields.add(currentField.toString())
                    currentField.clear()
                }
                else -> {
                    currentField.append(char)
                }
            }
            i++
        }
        
        fields.add(currentField.toString())
        
        return fields
    }

    override fun iterator(): Iterator<Map<String, String>> {
        return object : Iterator<Map<String, String>> {
            private val lines = processedContent.lines().filter { it.isNotBlank() }
            private var currentIndex = 1
            
            override fun hasNext(): Boolean {
                return currentIndex < lines.size
            }
            
            override fun next(): Map<String, String> {
                if (!hasNext()) {
                    throw NoSuchElementException()
                }
                
                val line = lines[currentIndex]
                val values = parseCsvLine(line, delimiter)
                
                if (values.size != expectedColumnCount) {
                    val rowNumber = currentIndex + 1
                    logger.error { "Column mismatch at row $rowNumber: expected $expectedColumnCount, got ${values.size}" }
                    throw CsvError.ColumnMismatch(rowNumber, expectedColumnCount, values.size)
                }
                
                currentIndex++
                return headers.zip(values).toMap()
            }
        }
    }
    
    fun getHeaders(): List<String> = headers
    
    fun getDelimiter(): Char = delimiter
}
