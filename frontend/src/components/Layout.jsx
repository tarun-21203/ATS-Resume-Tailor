import { Link, useLocation } from 'react-router-dom'
import { useEffect } from 'react'
import axios from 'axios'

function Layout({ children }) {
  const location = useLocation()
  const isAdminPage = location.pathname === '/admin'

  useEffect(() => {
    // Track website visit only on home page
    if (location.pathname === '/') {
      const trackVisit = async () => {
        try {
          const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
          await axios.post(`${apiUrl}/track`, {
            metricType: 'website_visit'
          })
        } catch (err) {
          console.error('Failed to track visit:', err)
        }
      }
      trackVisit()
    }
  }, [location.pathname])

  return (
    <div className="app">
      <div className="container">
        {!isAdminPage && (
          <header className="header">
            <div className="logo">
              <img src="/resume-tailor.png" alt="Resume Tailor Logo" />
            </div>
            <h1>ATS Resume Tailor</h1>
            <p className="subtitle">Transform your resume with AI to match any job description</p>
            <Link to="/admin" className="admin-link">
              <svg width="16" height="16" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M3 3a1 1 0 000 2v8a2 2 0 002 2h2.586l-1.293 1.293a1 1 0 101.414 1.414L10 15.414l2.293 2.293a1 1 0 001.414-1.414L12.414 15H15a2 2 0 002-2V5a1 1 0 100-2H3zm11.707 4.707a1 1 0 00-1.414-1.414L10 9.586 8.707 8.293a1 1 0 00-1.414 0l-2 2a1 1 0 101.414 1.414L8 10.414l1.293 1.293a1 1 0 001.414 0l4-4z" clipRule="evenodd"/>
              </svg>
              Admin
            </Link>
          </header>
        )}

        <main>
          {children}
        </main>

        {!isAdminPage && (
          <footer className="footer">
            <p className="copyright">© {new Date().getFullYear()} sanifalimomin. All rights reserved.</p>
          </footer>
        )}
      </div>
    </div>
  )
}

export default Layout
