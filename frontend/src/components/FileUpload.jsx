function FileUpload({ file, onChange }) {
  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      onChange(e.target.files[0])
    }
  }

  return (
    <div className="form-group">
      <label htmlFor="resumeFile">
        <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
          <path d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z"/>
        </svg>
        Master Resume (PDF)
      </label>
      <div className="file-input-wrapper">
        <input
          type="file"
          id="resumeFile"
          accept=".pdf"
          onChange={handleFileChange}
          required
        />
        <div className="file-input-display">
          {file ? (
            <span className="file-name">{file.name}</span>
          ) : (
            <span className="file-placeholder">Choose a PDF file or drag it here</span>
          )}
        </div>
      </div>
    </div>
  )
}

export default FileUpload
