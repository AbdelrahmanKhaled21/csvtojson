package com.csvtojson.models

/**
 * Sealed class hierarchy for CSV processing errors.
 * Provides specific error types for better error handling and user feedback.
 */
sealed class CsvError(message: String) : Exception(message) {
    /**
     * Thrown when CSV format is invalid (e.g., malformed structure, missing delimiters)
     */
    class InvalidFormat(details: String) : CsvError("Invalid CSV format: $details")
    
    /**
     * Thrown when a row has a different number of columns than expected
     */
    class ColumnMismatch(row: Int, expected: Int, actual: Int) : 
        CsvError("Row $row has $actual columns but expected $expected")
    
    /**
     * Thrown when the CSV file is empty or contains no data
     */
    class EmptyFile : CsvError("CSV file is empty")
    
    /**
     * Thrown when file encoding is not UTF-8 or cannot be decoded
     */
    class InvalidEncoding : CsvError("File encoding is not UTF-8")
}
