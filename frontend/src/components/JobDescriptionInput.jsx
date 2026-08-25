function JobDescriptionInput({ value, onChange }) {
  return (
    <div className="form-group">
      <label htmlFor="jd">
        <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
          <path fillRule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2h-1.528A6 6 0 004 9.528V4zm2 6a4 4 0 008 0v-1h-2v1a2 2 0 11-4 0v-1H6v1z" clipRule="evenodd"/>
        </svg>
        Job Description
      </label>
      <textarea
        id="jd"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder="Paste the complete job description here..."
        required
        rows={12}
      />
    </div>
  )
}

export default JobDescriptionInput
