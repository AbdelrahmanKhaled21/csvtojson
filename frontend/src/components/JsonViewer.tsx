import JsonView from 'react18-json-view'
import 'react18-json-view/src/style.css'
import CopyButton from './CopyButton.tsx'
import './JsonViewer.css'

interface JsonViewerProps {
  data: {
    metadata: {
      rows_processed: number
      processing_time_ms: number
      columns: string[]
      timestamp: string
      has_header: boolean
    }
    data: any[]
  }
}

export default function JsonViewer({ data }: JsonViewerProps) {
  return (
    <div className="json-viewer">
      <div className="viewer-header">
        <h2>Conversion Result</h2>
        <CopyButton data={data} />
      </div>
      
      <div className="metadata-section">
        <h3>Metadata</h3>
        <div className="metadata-grid">
          <div className="metadata-item">
            <span className="metadata-label">Rows Processed:</span>
            <span className="metadata-value">{data.metadata.rows_processed.toLocaleString()}</span>
          </div>
          <div className="metadata-item">
            <span className="metadata-label">Processing Time:</span>
            <span className="metadata-value">{data.metadata.processing_time_ms}ms</span>
          </div>
          <div className="metadata-item">
            <span className="metadata-label">Columns:</span>
            <span className="metadata-value">{data.metadata.columns.length}</span>
          </div>
          <div className="metadata-item">
            <span className="metadata-label">Has Header:</span>
            <span className="metadata-value">{data.metadata.has_header ? 'Yes' : 'No'}</span>
          </div>
        </div>
        
        <div className="columns-list">
          <strong>Column Names:</strong>
          <div className="columns-tags">
            {data.metadata.columns.map((col, idx) => (
              <span key={idx} className="column-tag">{col}</span>
            ))}
          </div>
        </div>
      </div>
      
      <div className="data-section">
        <h3>Data Preview ({data.data.length} rows)</h3>
        <div className="json-container">
          <JsonView 
            src={data.data}
            collapsed={1}
            theme="a11y"
            dark={true}
          />
        </div>
      </div>
    </div>
  )
}
