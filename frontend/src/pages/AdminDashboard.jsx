import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import axios from 'axios'
import MetricCard from '../components/MetricCard'
import Alert from '../components/Alert'
import '../styles/AdminDashboard.css'

function AdminDashboard() {
  const [metrics, setMetrics] = useState({
    resume_tailored: 0,
    website_visit: 0
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchMetrics()
  }, [])

  const fetchMetrics = async () => {
    try {
      setLoading(true)
      setError(null)
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
      const response = await axios.get(`${apiUrl}/analytics`)
      setMetrics(response.data)
    } catch (err) {
      console.error('Failed to fetch metrics:', err)
      setError('Failed to load analytics data')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="admin-container">
      <div className="admin-header">
        <h1>Admin Dashboard</h1>
        <button onClick={fetchMetrics} className="refresh-btn" disabled={loading}>
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clipRule="evenodd"/>
          </svg>
          Refresh
        </button>
      </div>

      {loading && (
        <div className="loading-state">
          <span className="spinner"></span>
          <p>Loading analytics...</p>
        </div>
      )}

      {error && <Alert type="error" message={error} />}

      {!loading && !error && (
        <div className="metrics-grid">
          <MetricCard
            title="Resumes Tailored"
            value={metrics.resume_tailored}
            description="Total number of resumes processed"
            icon="resume"
          />
          <MetricCard
            title="Website Visits"
            value={metrics.website_visit}
            description="Total number of unique visits"
            icon="visit"
          />
        </div>
      )}

      <div className="admin-footer">
        <Link to="/" className="back-link">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M9.707 16.707a1 1 0 01-1.414 0l-6-6a1 1 0 010-1.414l6-6a1 1 0 011.414 1.414L5.414 9H17a1 1 0 110 2H5.414l4.293 4.293a1 1 0 010 1.414z" clipRule="evenodd"/>
          </svg>
          Back to Home
        </Link>
      </div>
    </div>
  )
}

export default AdminDashboard
