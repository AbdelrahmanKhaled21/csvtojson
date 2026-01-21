# CSV to JSON Converter - Test Data

This directory contains sample CSV files for testing the application.

## Files

- `simple.csv` - Basic CSV with 5 rows, 4 columns
- `quoted.csv` - CSV with quoted fields containing commas and escaped quotes

## Testing Instructions

1. Start the application (see main README.md)
2. Upload each test file through the web interface
3. Verify the JSON output matches expected structure

## Expected Behavior

### simple.csv
- Should process 5 rows
- Should extract 4 columns: name, age, city, occupation
- Should complete in < 100ms

### quoted.csv
- Should handle commas within quoted fields correctly
- Should convert double quotes (`""`) to single quotes (`"`)
- Should preserve all text within quotes
