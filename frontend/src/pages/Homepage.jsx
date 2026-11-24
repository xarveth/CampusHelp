import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, getUser } from '../api.js'
import { statusLabel } from './StudentDashboard.jsx'

/** Public landing page: what CampusHelp is, live numbers, how it works, open complaints. */
export default function Homepage() {
  const user = getUser()
  const [stats, setStats] = useState(null)
  const [categories, setCategories] = useState([])
  const [board, setBoard] = useState([])
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    api('/public/stats', { auth: false }).then(setStats).catch(() => {})
    api('/categories', { auth: false }).then(setCategories).catch(() => {})
    api('/public/complaints', { auth: false }).then(setBoard).catch(() => {})
  }, [])

  async function sameIssue(id) {
    if (!user) {
      setNotice('Log in first to report the same problem.')
      return
    }
    setNotice('')
    setError('')
    try {
      await api(`/complaints/${id}/same-issue`, { method: 'POST' })
      setNotice('Noted — the admin can see this problem affects more than one student.')
      const fresh = await api('/public/complaints', { auth: false })
      setBoard(fresh)
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div>
      <section className="hero">
        <h1>Report a campus problem.<br />Track it to resolution.</h1>
        <p className="hero-sub">
          CampusHelp is the college's single place for grievances and complaints.
          Submit one, follow it through Submitted → Under Review → Assigned →
          In Progress → Resolved, and see every step in its history.
        </p>
        <div className="hero-cta">
          {user ? (
            <Link className="btn btn-primary" to={user.role === 'ADMIN' ? '/admin' : user.role === 'STAFF' ? '/staff' : '/student'}>
              Go to my dashboard
            </Link>
          ) : (
            <>
              <Link className="btn btn-primary" to="/register">Register</Link>
              <Link className="btn btn-ghost" to="/login">Log in</Link>
            </>
          )}
        </div>
      </section>

      {stats && (
        <div className="stats">
          <div className="stat"><strong>{stats.total}</strong><span>Total complaints</span></div>
          <div className="stat"><strong>{stats.resolved}</strong><span>Resolved</span></div>
          <div className="stat"><strong>{stats.resolutionRate}%</strong><span>Resolution rate</span></div>
          <div className="stat"><strong>{stats.averageResolutionHours == null ? '—' : `${stats.averageResolutionHours} h`}</strong><span>Average resolution time</span></div>
        </div>
      )}

      <div className="card">
        <h2>How it works</h2>
        <ol className="steps">
          <li><strong>1. Submit</strong> — pick a category, describe the problem, add a photo if it helps. Your complaint gets a unique ID like CH-1001.</li>
          <li><strong>2. Track</strong> — follow it through Submitted, Under Review, Assigned and In Progress, with every status change and admin update in its timeline.</li>
          <li><strong>3. Resolved</strong> — rate the resolution. Not fixed properly? Reopen it with a reason and it goes back into the queue.</li>
        </ol>
      </div>

      <div className="card">
        <h2>Categories</h2>
        <p className="muted">
          {categories.length ? categories.map((c) => c.name).join(' · ') : 'Loading…'}
        </p>
      </div>

      <div className="card">
        <h2>Open complaints</h2>
        <p className="muted">
          Problems other students are already tracking. If one of these is your problem too,
          say so instead of filing a duplicate.
        </p>
        {notice && <div className="alert success">{notice}</div>}
        {error && <div className="alert error">{error}</div>}
        {board.length === 0 && <p className="muted">No open complaints right now.</p>}
        <table className="table">
          <thead>
            <tr><th>ID</th><th>Subject</th><th>Category</th><th>Date</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {board.map((c) => (
              <tr key={c.id}>
                <td>{c.code}</td>
                <td>{c.subject}</td>
                <td>{c.category}</td>
                <td>{new Date(c.createdAt).toLocaleDateString()}</td>
                <td><span className={`badge ${c.status.toLowerCase()}`}>{statusLabel(c.status)}</span></td>
                <td>
                  <button className="btn btn-ghost btn-small" onClick={() => sameIssue(c.id)}>
                    Same problem
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
