import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, getUser } from '../api.js'
import { statusLabel } from './StudentDashboard.jsx'

export default function ComplaintDetail() {
  const { id } = useParams()
  const user = getUser()
  const isAdmin = user?.role === 'ADMIN'
  const [complaint, setComplaint] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const base = isAdmin ? '/admin/complaints' : '/complaints'

  async function load() {
    try {
      setComplaint(await api(`${base}/${id}`))
    } catch (err) {
      setError(err.message)
    }
  }

  useEffect(() => { load() }, [id])

  async function updateStatus(status) {
    setBusy(true)
    setError('')
    try {
      setComplaint(await api(`/admin/complaints/${id}/status`, {
        method: 'PATCH', body: { status },
      }))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  if (error) return <div className="alert error">{error}</div>
  if (!complaint) return <p className="muted">Loading…</p>

  return (
    <div>
      <Link to={isAdmin ? '/admin' : '/student'}>&larr; Back to dashboard</Link>
      <div className="card">
        <h1>{complaint.code} — {complaint.subject}</h1>
        <p>
          <span className={`badge ${complaint.status.toLowerCase()}`}>{statusLabel(complaint.status)}</span>
          {' '}· {complaint.category} · {new Date(complaint.createdAt).toLocaleString()}
        </p>
        <p><strong>Student:</strong> {complaint.studentName} ({complaint.studentEmail})</p>
        <p>{complaint.description}</p>

        {isAdmin && (
          <div className="actions">
            <span>Update status:</span>
            {['PENDING', 'IN_PROGRESS', 'RESOLVED'].map((s) => (
              <button key={s} className="btn btn-primary" disabled={busy || complaint.status === s}
                onClick={() => updateStatus(s)}>
                {statusLabel(s)}
              </button>
            ))}
          </div>
        )}

        <h2>Status History</h2>
        <ol className="timeline">
          {(complaint.history || []).map((h, i) => (
            <li key={i}>
              <strong>{h.oldStatus ? `${statusLabel(h.oldStatus)} → ${statusLabel(h.newStatus)}` : `Submitted · ${statusLabel(h.newStatus)}`}</strong>
              {' '}— by {h.changedBy} on {new Date(h.changedAt).toLocaleString()}
            </li>
          ))}
        </ol>
      </div>
    </div>
  )
}
