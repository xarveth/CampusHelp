import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, saveAuth } from '../api.js'

export default function Register() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()

  async function submit(e) {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const data = await api('/auth/register', {
        method: 'POST', body: { name, email, password }, auth: false,
      })
      saveAuth(data)
      navigate('/student')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card narrow">
      <h1>Student Registration</h1>
      <p className="muted">Create a student account to submit and track complaints.</p>
      {error && <div className="alert error">{error}</div>}
      <form onSubmit={submit} className="form">
        <label>Full name
          <input value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>Password (min. 6 characters)
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={6} />
        </label>
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Creating…' : 'Register'}</button>
      </form>
      <p className="muted">Already have an account? <Link to="/login">Login</Link></p>
    </div>
  )
}
