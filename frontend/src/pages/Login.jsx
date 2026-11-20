import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, saveAuth } from '../api.js'

export default function Login() {
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
      const data = await api('/auth/login', { method: 'POST', body: { email, password }, auth: false })
      saveAuth(data)
      navigate(data.role === 'ADMIN' ? '/admin' : '/student')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  function fillDemo(emailValue) {
    setEmail(emailValue)
    setPassword('campus123')
  }

  return (
    <div className="card narrow">
      <h1>Login</h1>
      <p className="muted">Sign in as a student or administrator.</p>
      {error && <div className="alert error">{error}</div>}
      <form onSubmit={submit} className="form">
        <label>Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <button className="btn btn-primary" disabled={busy}>{busy ? 'Signing in…' : 'Login'}</button>
      </form>
      <p className="muted">No account? <Link to="/register">Register as a student</Link></p>
      <div className="demo">
        <p><strong>Demo logins</strong> (password: campus123)</p>
        <button className="btn btn-ghost" type="button" onClick={() => fillDemo('himanshu@campushelp.edu')}>
          Student — himanshu@campushelp.edu
        </button>
        <button className="btn btn-ghost" type="button" onClick={() => fillDemo('admin@campushelp.edu')}>
          Admin — admin@campushelp.edu
        </button>
      </div>
    </div>
  )
}
