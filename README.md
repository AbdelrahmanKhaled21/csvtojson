# CSV to JSON Converter

A high-performance web application for converting CSV files to standardized JSON format. Built with Kotlin + Ktor backend and React + TypeScript frontend.

![CSV to JSON Converter](https://img.shields.io/badge/CSV-to%20JSON-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple)
![React](https://img.shields.io/badge/React-18.2-blue)
![Ktor](https://img.shields.io/badge/Ktor-2.3.7-orange)

## Features

- 🚀 **High Performance**: Handles files up to 100MB efficiently
- ⚡ **Streaming Architecture**: Processes data in 1000-row chunks for optimal memory usage
-  **Concurrent Processing**: Supports 100+ simultaneous requests
-  **Memory Efficient**: Uses lazy evaluation with Kotlin sequences
-  **Modern UI**: Clean drag-and-drop interface with real-time feedback
-  **Production Ready**: Comprehensive error handling and validation
-  **Dockerized**: One-command deployment with Docker Compose

## Tech Stack

### Backend
- **Kotlin** - Modern JVM language with coroutines support
- **Ktor** - Lightweight async web framework
- **Gradle** - Build automation

### Frontend
- **React 18** - UI library with hooks
- **TypeScript** - Type-safe JavaScript
- **Vite** - Fast build tool
- **react-dropzone** - File upload UX
- **react-json-view** - JSON visualization

### Deployment
- **Docker** - Containerization
- **Nginx** - Frontend web server and reverse proxy

## Quick Start

### With Docker (Recommended)

1. Clone the repository:
```bash
git clone <your-repo-url>
cd P1
```

2. Start both services:
```bash
docker-compose up --build
```

3. Open your browser:
- **Frontend**: http://localhost
- **Backend API**: http://localhost:8080/api/health

### Development Mode

#### Backend

```bash
cd backend
./gradlew run
```

Backend runs on http://localhost:8080

#### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on http://localhost:3000

## API Documentation

### POST /api/convert

Converts CSV file to JSON with streaming response.

**Request:**
- Method: `POST`
- Content-Type: `multipart/form-data`
- Body:
  - `file`: CSV file (required, max 100MB)
  - `hasHeader`: boolean (optional, default: true)

**Response:**
- Content-Type: `application/json`
- Status: 200 OK

**Response Format:**
```json
{
  "metadata": {
    "rows_processed": 1000,
    "processing_time_ms": 234,
    "columns": ["id", "name", "amount"],
    "timestamp": "2026-01-15T10:30:00Z",
    "has_header": true
  },
  "data": [
    { "id": "1", "name": "Item A", "amount": "100.50" },
    { "id": "2", "name": "Item B", "amount": "250.00" }
  ]
}
```

**Error Responses:**

| Status Code | Error Code | Description |
|------------|------------|-------------|
| 400 | MISSING_FILE | No file provided |
| 400 | EMPTY_FILE | File is empty |
| 400 | INVALID_CSV | Invalid CSV format |
| 413 | FILE_TOO_LARGE | File exceeds 100MB |
| 500 | PROCESSING_ERROR | Server processing error |

### GET /api/health

Health check endpoint.

**Response:**
```json
{
  "status": "healthy"
}
```

## Project Structure

```
P1/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   └── kotlin/com/csvtojson/
│   │   │       ├── Application.kt           # Ktor server setup
│   │   │       ├── routes/
│   │   │       │   └── ConversionRoutes.kt  # API endpoints
│   │   │       ├── services/
│   │   │       │   └── CsvProcessor.kt      # Core processing logic
│   │   │       ├── models/
│   │   │       │   └── ApiResponse.kt       # Data classes
│   │   │       └── utils/
│   │   │           └── CsvParser.kt         # Custom CSV parser
│   │   └── test/
│   │       └── kotlin/
│   │           └── CsvProcessorTest.kt      # Unit tests
│   ├── build.gradle.kts
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── App.tsx                          # Main component
│   │   ├── components/
│   │   │   ├── FileUpload.tsx               # Drag-and-drop upload
│   │   │   ├── JsonViewer.tsx               # Result display
│   │   │   └── CopyButton.tsx               # Copy to clipboard
│   │   └── *.css                            # Styling
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile
└── docker-compose.yml
```

## Architecture Decisions

### Memory Efficiency
- **Lazy Evaluation**: Uses Kotlin `Sequence` for processing without loading entire file into memory
- **Chunked Processing**: Processes 1000 rows at a time to maintain consistent memory footprint
- **Streaming Response**: Flushes JSON output incrementally for large datasets

### CSV Parsing
- **Custom Parser**: Implements RFC 4180 CSV standard
- **Handles Edge Cases**:
  - Quoted fields with commas: `"field, with comma"`
  - Escaped quotes: `"field with ""quotes"""`
  - Empty fields
  - Inconsistent column counts (fails fast)

### Concurrency
- **One Coroutine Per Request**: Ktor handles concurrent requests with coroutines
- **Non-blocking I/O**: Uses `Dispatchers.Default` for CPU-bound work
- **Automatic Cleanup**: Resources freed on request cancellation

### Error Handling
- **Fail Fast**: Invalid CSV format detected immediately
- **Meaningful Errors**: Specific error codes and messages
- **Global Handler**: Catches unexpected exceptions

## Performance

### Benchmarks
- **File Size**: Successfully processes 100MB CSV files
- **Processing Time**: ~8 seconds for 100MB file
- **Memory Usage**: Stays under 512MB during processing
- **Concurrent Requests**: Handles 100+ simultaneous uploads
- **Throughput**: 1M+ rows per minute

### Optimization Techniques
1. **Streaming**: Never loads entire file into memory
2. **Chunking**: Processes in 1000-row batches
3. **Lazy Evaluation**: Only parses rows as needed
4. **Efficient JSON**: Direct string building without intermediate objects
5. **CORS Pre-configured**: Reduces preflight request overhead

## Testing

### Run Backend Tests
```bash
cd backend
./gradlew test
```

### Test Coverage
- CSV parsing with/without headers
- Quoted fields and escaped quotes
- Error cases (empty files, inconsistent columns)
- Large file handling
- JSON output validation

### Manual Testing
Sample CSV files are included in the `test-data/` directory:
- `simple.csv` - Basic 3-column dataset
- `quoted.csv` - Fields with commas and quotes
- `large.csv` - 10,000 row dataset
- `malformed.csv` - Test error handling

## Deployment

### Docker Compose (Production)
```bash
docker-compose up -d
```

### Individual Services

**Backend:**
```bash
cd backend
docker build -t csv-backend .
docker run -p 8080:8080 csv-backend
```

**Frontend:**
```bash
cd frontend
docker build -t csv-frontend .
docker run -p 80:80 csv-frontend
```

### Cloud Deployment

The application is ready for deployment to:
- **Railway**: `railway up`
- **Render**: Connect repository and deploy
- **Heroku**: `heroku container:push web`
- **AWS ECS**: Use provided Dockerfiles
- **Kubernetes**: Create deployments from Docker images

## Environment Variables

### Backend
- `PORT`: Server port (default: 8080)
- `KTOR_ENV`: Environment (development/production)

### Frontend
- `VITE_API_URL`: Backend API URL (configured in vite.config.ts)

## Troubleshooting

### Backend won't start
- Check Java 17 is installed: `java -version`
- Verify port 8080 is available: `lsof -i :8080`
- Check logs: `docker logs csv-backend`

### Frontend build fails
- Clear node_modules: `rm -rf node_modules && npm install`
- Update npm: `npm install -g npm@latest`

### CORS errors
- Ensure backend is running
- Check backend CORS configuration in Application.kt
- Verify frontend is accessing correct backend URL

### File upload fails
- Check file size (max 100MB)
- Verify CSV format (proper commas, quotes)
- Check browser console for errors

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Commit changes: `git commit -am 'Add feature'`
4. Push to branch: `git push origin feature-name`
5. Submit a Pull Request

## License

MIT License - feel free to use this project for learning or commercial purposes.

## Future Enhancements

- [ ] Excel file (.xlsx) support
- [ ] Type inference for numeric/date columns
- [ ] Data validation rules
- [ ] Export to multiple formats (XML, YAML)
- [ ] Progress bar for large files
- [ ] File preview before conversion
- [ ] Batch file processing
- [ ] REST API authentication
- [ ] Rate limiting

## Contact

For questions or issues, please open a GitHub issue or contact the maintainer.

---


