import { useState } from 'react'
import axios from 'axios'
import FileUpload from '../components/FileUpload'
import JobDescriptionInput from '../components/JobDescriptionInput'
import ResumePreview from '../components/ResumePreview'
import Alert from '../components/Alert'
import '../styles/Home.css'

function Home() {
  const [step, setStep] = useState(1)
  const [masterResumeFile, setMasterResumeFile] = useState(null)
  const [jobDescription, setJobDescription] = useState('')
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [pdfBase64, setPdfBase64] = useState(null)
  const [error, setError] = useState(null)
  const [emailStatus, setEmailStatus] = useState(null)

  const convertToBase64 = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader()
      reader.readAsDataURL(file)
      reader.onload = () => resolve(reader.result.split(',')[1])
      reader.onerror = (error) => reject(error)
    })
  }

  const handleTailorResume = async (e) => {
    e.preventDefault()
    if (!masterResumeFile) {
      setError('Please upload a Master Resume PDF.')
      return
    }

    setLoading(true)
    setError(null)
    setPdfBase64(null)

    try {
      const base64Pdf = await convertToBase64(masterResumeFile)
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
      const response = await axios.post(`${apiUrl}/tailorResume`, {
        resumePdfBase64: base64Pdf,
        jobDescription: jobDescription
      })

      if (response.data?.pdfBase64) {
        setPdfBase64(response.data.pdfBase64)
        setStep(2)
      } else {
        setError('Failed to generate PDF. Unexpected response format.')
      }
    } catch (err) {
      console.error(err)
      setError(`Error: Could not reach API. ${err.message}`)
    } finally {
      setLoading(false)
    }
  }

  const handleSendEmail = async () => {
    if (!email) {
      setError('Please enter an email address.')
      return
    }

    setLoading(true)
    setEmailStatus(null)

    try {
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
      await axios.post(`${apiUrl}/sendEmail`, {
        email: email,
        pdfBase64: pdfBase64
      })
      setEmailStatus('success')
    } catch (err) {
      console.error(err)
      setEmailStatus('error')
    } finally {
      setLoading(false)
    }
  }

  const handleReset = () => {
    setStep(1)
    setPdfBase64(null)
    setMasterResumeFile(null)
    setJobDescription('')
    setEmailStatus(null)
    setError(null)
    setEmail('')
  }

  return (
    <div className="home-container">
      {step === 1 && (
        <div className="card">
          <div className="card-header">
            <h2>Get Started</h2>
            <p>Upload your resume and paste the job description</p>
          </div>
          
          <form onSubmit={handleTailorResume}>
            <FileUpload
              file={masterResumeFile}
              onChange={setMasterResumeFile}
            />

            <JobDescriptionInput
              value={jobDescription}
              onChange={setJobDescription}
            />

            {error && <Alert type="error" message={error} />}

            <button type="submit" disabled={loading} className="btn btn-primary">
              {loading ? (
                <>
                  <span className="spinner"></span>
                  Tailoring Your Resume...
                </>
              ) : (
                <>
                  <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5-1.429A1 1 0 009 15.571V11a1 1 0 112 0v4.571a1 1 0 00.725.962l5 1.428a1 1 0 001.17-1.408l-7-14z"/>
                  </svg>
                  Tailor Resume with AI
                </>
              )}
            </button>
          </form>
        </div>
      )}

      {step === 2 && (
        <ResumePreview
          pdfBase64={pdfBase64}
          email={email}
          setEmail={setEmail}
          emailStatus={emailStatus}
          loading={loading}
          onSendEmail={handleSendEmail}
          onReset={handleReset}
        />
      )}
    </div>
  )
}

export default Home
