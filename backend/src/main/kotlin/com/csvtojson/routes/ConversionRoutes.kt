package com.csvtojson.routes

import com.csvtojson.models.CsvError
import com.csvtojson.models.ErrorResponse
import com.csvtojson.services.CsvProcessor
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.withPermit
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
private const val MAX_FILE_SIZE = 100 * 1024 * 1024 // 100MB

fun Route.conversionRoutes() {
    
    route("/api") {
        
        post("/convert") {
            var uploadedBytes: ByteArray? = null
            var fileName: String? = null
            var hasHeader = true
            var isFileSizeExceeded = false
            var isFileEmpty = false
            
            logger.info { "Received CSV upload request" }
            
            try {
                val multipart = call.receiveMultipart()
                
                multipart.forEachPart { part ->
                    try {
                        when (part) {
                            is PartData.FileItem -> {
                                if (uploadedBytes == null && !isFileSizeExceeded && !isFileEmpty) {
                                    fileName = part.originalFileName ?: "unknown"
                                    val inputStream = part.streamProvider()
                                    val bytes = inputStream.readBytes()
                                    
                                    when {
                                        bytes.size > MAX_FILE_SIZE -> {
                                            isFileSizeExceeded = true
                                            logger.warn { "File size exceeds limit: ${bytes.size} bytes (max: $MAX_FILE_SIZE)" }
                                        }
                                        bytes.isEmpty() -> {
                                            isFileEmpty = true
                                            logger.warn { "Received empty file: $fileName" }
                                        }
                                        else -> uploadedBytes = bytes
                                    }
                                }
                            }
                            is PartData.FormItem -> {
                                if (part.name == "hasHeader") {
                                    hasHeader = part.value.toBooleanStrictOrNull() ?: true
                                }
                            }
                            else -> {
                                logger.warn { "Unexpected part type: ${part::class.simpleName}" }
                            }
                        }
                    } finally {
                        part.dispose()
                    }
                }
                
                when {
                    isFileSizeExceeded -> {
                        call.respond(
                            HttpStatusCode.PayloadTooLarge,
                            ErrorResponse(
                                error = "File exceeds 100MB limit",
                                code = "FILE_TOO_LARGE"
                            )
                        )
                        return@post
                    }
                    isFileEmpty -> {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = "File is empty",
                                code = "EMPTY_FILE"
                            )
                        )
                        return@post
                    }
                    uploadedBytes == null -> {
                        logger.warn { "No file provided in request" }
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = "No file provided or file part named 'file' not found",
                                code = "MISSING_FILE"
                            )
                        )
                        return@post
                    }
                }
                
                val bytes = uploadedBytes!!
                logger.info { "Processing CSV file: $fileName (${bytes.size} bytes, hasHeader=$hasHeader)" }
                
                CsvProcessor.processingLimiter.withPermit {
                    val startTime = System.currentTimeMillis()
                    val processor = CsvProcessor(bytes, hasHeader)
                    
                    call.response.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    call.response.header(HttpHeaders.CacheControl, "no-cache")
                    
                    call.respondOutputStream(ContentType.Application.Json) {
                        processor.streamJsonOutput(this)
                    }
                    
                    val processingTime = System.currentTimeMillis() - startTime
                    logger.info { "CSV processed successfully: file=$fileName, size=${bytes.size} bytes, time=${processingTime}ms" }
                }
                
            } catch (e: CsvError.InvalidFormat) {
                logger.error { "Invalid CSV format: ${e.message}" }
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        error = e.message ?: "Invalid CSV format",
                        code = "INVALID_CSV_FORMAT",
                        details = "Check that your file has proper delimiters and structure"
                    )
                )
            } catch (e: CsvError.ColumnMismatch) {
                logger.error { "Column mismatch: ${e.message}" }
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        error = e.message ?: "Column count mismatch",
                        code = "COLUMN_MISMATCH",
                        details = "All rows must have the same number of columns"
                    )
                )
            } catch (e: CsvError.EmptyFile) {
                logger.error { "Empty CSV file: ${e.message}" }
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        error = e.message ?: "CSV file is empty",
                        code = "EMPTY_CSV",
                        details = "The CSV file contains no data"
                    )
                )
            } catch (e: CsvError.InvalidEncoding) {
                logger.error { "Invalid encoding: ${e.message}" }
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        error = e.message ?: "Invalid file encoding",
                        code = "INVALID_ENCODING",
                        details = "File must be UTF-8 encoded"
                    )
                )
            } catch (e: IllegalArgumentException) {
                logger.error(e) { "Invalid CSV: ${e.message}" }
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(
                        error = "Invalid CSV format: ${e.message}",
                        code = "INVALID_CSV"
                    )
                )
            } catch (e: Exception) {
                logger.error(e) { "CSV processing failed: ${e.message}" }
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ErrorResponse(
                        error = "Processing failed: ${e.message}",
                        code = "PROCESSING_ERROR"
                    )
                )
            }
        }
        
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
        }
    }
}
