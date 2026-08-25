import Alert from './Alert'

function ResumePreview({ pdfBase64, email, setEmail, emailStatus, loading, onSendEmail, onReset }) {
  return (
    <div className="card card-wide">
      <div className="card-header">
        <h2>Your Tailored Resume</h2>
        <div className="action-buttons">
          <button onClick={onReset} className="btn btn-secondary">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd"/>
            </svg>
            Create New
          </button>
          <a
            href={`data:application/pdf;base64,${pdfBase64}`}
            download="Tailored_Resume.pdf"
            className="btn btn-primary"
          >
            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clipRule="evenodd"/>
            </svg>
            Download PDF
          </a>
        </div>
      </div>

      <div className="pdf-viewer">
        <iframe
          src={`data:application/pdf;base64,${pdfBase64}`}
          title="Resume Preview"
        />
      </div>

      <div className="email-section">
        <h3>
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path d="M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z"/>
            <path d="M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z"/>
          </svg>
          Send to Email
        </h3>
        <div className="email-input-group">
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="Enter your email address"
          />
          <button onClick={onSendEmail} disabled={loading} className="btn btn-accent">
            {loading ? (
              <>
                <span className="spinner"></span>
                Sending...
              </>
            ) : (
              'Send Email'
            )}
          </button>
        </div>
        {emailStatus === 'success' && (
          <Alert type="success" message="Email sent successfully!" />
        )}
        {emailStatus === 'error' && (
          <Alert type="error" message="Failed to send email. Please try again." />
        )}
      </div>
    </div>
  )
}

export default ResumePreview
