import { useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import './FileUpload.css'

interface FileUploadProps {
  onFileSelected: (file: File) => void
  isProcessing: boolean
}

export default function FileUpload({ onFileSelected, isProcessing }: FileUploadProps) {
  const onDrop = useCallback((acceptedFiles: File[]) => {
    if (acceptedFiles.length > 0) {
      onFileSelected(acceptedFiles[0])
    }
  }, [onFileSelected])

  const { getRootProps, getInputProps, isDragActive, fileRejections } = useDropzone({
    onDrop,
    accept: {
      'text/csv': ['.csv'],
    },
    maxFiles: 1,
    maxSize: 100 * 1024 * 1024, // 100MB
    disabled: isProcessing,
  })

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  return (
    <div className="file-upload-container">
      <div 
        {...getRootProps()} 
        className={`dropzone ${isDragActive ? 'active' : ''} ${isProcessing ? 'disabled' : ''}`}
      >
        <input {...getInputProps()} />
        
        {isProcessing ? (
          <div className="upload-content processing">
            <div className="spinner"></div>
            <p className="upload-text">Processing your CSV file...</p>
            <p className="upload-subtext">Please wait while we convert your data</p>
          </div>
        ) : isDragActive ? (
          <div className="upload-content">
            <svg className="upload-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
            </svg>
            <p className="upload-text">Drop your CSV file here</p>
          </div>
        ) : (
          <div className="upload-content">
            <svg className="upload-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
            </svg>
            <p className="upload-text">Drag and drop a CSV file here</p>
            <p className="upload-subtext">or click to browse</p>
            <p className="upload-limit">Maximum file size: 100MB</p>
          </div>
        )}
      </div>
      
      {fileRejections.length > 0 && (
        <div className="file-rejection">
          {fileRejections.map(({ file, errors }) => (
            <div key={file.name}>
              <strong>{file.name}</strong> - {formatFileSize(file.size)}
              <ul>
                {errors.map(e => (
                  <li key={e.code}>{e.message}</li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
