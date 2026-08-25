function MetricCard({ title, value, description, icon }) {
  const icons = {
    resume: (
      <svg width="40" height="40" viewBox="0 0 20 20" fill="currentColor">
        <path d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z"/>
      </svg>
    ),
    visit: (
      <svg width="40" height="40" viewBox="0 0 20 20" fill="currentColor">
        <path d="M10 12a2 2 0 100-4 2 2 0 000 4z"/>
        <path fillRule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clipRule="evenodd"/>
      </svg>
    )
  }

  return (
    <div className="metric-card">
      <div className={`metric-icon ${icon}-icon`}>
        {icons[icon]}
      </div>
      <div className="metric-content">
        <h3>{title}</h3>
        <p className="metric-value">{value.toLocaleString()}</p>
        <p className="metric-description">{description}</p>
      </div>
    </div>
  )
}

export default MetricCard
