package com.csvtojson.services

import com.csvtojson.models.CsvError
import com.csvtojson.utils.CsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import java.io.OutputStream
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Core CSV processor that orchestrates parsing and JSON streaming.
 * Processes CSV in chunks of 5000 rows for memory efficiency.
 */
class CsvProcessor(private val csvBytes: ByteArray, private val hasHeader: Boolean = true) {
    
    companion object {
        // Prevents memory exhaustion from too many concurrent large file uploads
        val processingLimiter = Semaphore(50)
    }
    
    private val startTime = System.currentTimeMillis()
    
    /**
     * Stream JSON output to the provided OutputStream.
     * Times out after 30 seconds to prevent hanging.
     */
    suspend fun streamJsonOutput(outputStream: OutputStream) = withTimeout(30_000) {
        logger.info { "Starting CSV processing: ${csvBytes.size} bytes" }
        
        val csvContent = try {
            withContext(Dispatchers.IO) {
                csvBytes.decodeToString()
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to decode CSV content - invalid encoding" }
            throw CsvError.InvalidEncoding()
        }
        
        withContext(Dispatchers.Default) {
            val parser = CsvParser(csvContent)
            val headers = parser.getHeaders()
            val writer = outputStream.bufferedWriter()
            
            logger.debug { "Starting JSON streaming with ${headers.size} columns" }
            
            writer.write("{")
            
            var rowCount = 0
            writer.write("\"data\":[")
            
            parser.asSequence()
                .chunked(5000) // Process 5000 rows at a time for better performance
                .forEachIndexed { chunkIndex, chunk ->
                    ensureActive()
                    chunk.forEachIndexed { _, row ->
                        if (rowCount > 0) writer.write(",")
                        val jsonRow = convertRowToJson(row)
                        writer.write(jsonRow)
                        rowCount++
                    }
                    writer.flush()
                    
                    if (chunkIndex > 0 && chunkIndex % 10 == 0) {
                        logger.debug { "Processed ${rowCount} rows so far..." }
                    }
                }
            
            writer.write("],")
            writeMetadataSection(writer, headers, rowCount)
            writer.write("}")
            writer.flush()
            
            val processingTime = System.currentTimeMillis() - startTime
            logger.info { "CSV processing completed: rows=$rowCount, time=${processingTime}ms" }
        }
    }
    
    private fun writeMetadataSection(writer: java.io.Writer, headers: List<String>, rowCount: Int) {
        val processingTime = System.currentTimeMillis() - startTime
        writer.write("\"metadata\":{")
        writer.write("\"rows_processed\":$rowCount,")
        writer.write("\"processing_time_ms\":$processingTime,")
        writer.write("\"columns\":")
        writer.write(columnsToJson(headers))
        writer.write(",")
        writer.write("\"timestamp\":\"${Instant.now()}\",")
        writer.write("\"has_header\":true")
        writer.write("}")
    }
    
    private fun convertRowToJson(row: Map<String, String>): String {
        return buildString {
            append("{")
            row.entries.forEachIndexed { index, (key, value) ->
                if (index > 0) append(",")
                append("\"${key.escapeJson()}\":\"${value.escapeJson()}\"")
            }
            append("}")
        }
    }
    
    private fun columnsToJson(columns: List<String>): String {
        return "[${columns.joinToString(",") { "\"${it.escapeJson()}\"" }}]"
    }
    
    private fun String.escapeJson(): String {
        return this.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
    }
}
