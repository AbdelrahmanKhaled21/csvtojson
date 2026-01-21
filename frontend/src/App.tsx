import { useState } from 'react'
import FileUpload from './components/FileUpload.tsx'
import JsonViewer from './components/JsonViewer.tsx'
import './App.css'

interface ProcessingResult {
  metadata: {
    rows_processed: number
    processing_time_ms: number
    columns: string[]
    timestamp: string
    has_header: boolean
  }
  data: Record<string, string>[]
}

function App() {
  const [conversionResult, setConversionResult] = useState<ProcessingResult | null>(null)
  const [isProcessing, setIsProcessing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleFileUpload = async (file: File) => {
    setIsProcessing(true)
    setError(null)
    setConversionResult(null)
    
    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('hasHeader', 'true')
      
      const response = await fetch('/api/convert', {
        method: 'POST',
        body: formData,
      })
      
      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.error || 'Upload failed')
      }
      
      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('Failed to read response stream')
      }
      
      const decoder = new TextDecoder()
      let jsonText = ''
      
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        
        jsonText += decoder.decode(value, { stream: true })
      }
      
      jsonText += decoder.decode()
      
      const parsedResult = JSON.parse(jsonText)
      setConversionResult(parsedResult)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error occurred')
    } finally {
      setIsProcessing(false)
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>CSV to JSON Converter</h1>
        <p>Upload a CSV file and convert it to JSON format instantly</p>
      </header>
      
      <main className="app-main">
        <FileUpload 
          onFileSelected={handleFileUpload}
          isProcessing={isProcessing}
        />
        
        {error && (
          <div className="error-message">
            <strong>Error:</strong> {error}
          </div>
        )}
        
        {conversionResult && <JsonViewer data={conversionResult} />}
      </main>
      
      <footer className="app-footer">
        <p>Powered by Kotlin + Ktor backend with React frontend</p>
      </footer>
    </div>
  )
}

export default App
