import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, fileUrl, getUser } from '../api.js'
import { starsText, statusLabel } from './StudentDashboard.jsx'

export default function ComplaintDetail() {
  const { id } = useParams()
  const user = getUser()
  const isAdmin = user?.role === 'ADMIN'
  const isStaff = user?.role === 'STAFF'
  const isStudent = user?.role === 'STUDENT'
  const [complaint, setComplaint] = useState(null)
  const [staffList, setStaffList] = useState([])
  const [assignTo, setAssignTo] = useState('')
  const [stars, setStars] = useState(5)
  const [ratingComment, setRatingComment] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)

  const base = isAdmin ? '/admin/complaints' : isStaff ? '/staff/complaints' : '/complaints'
  const backTo = isAdmin ? '/admin' : isStaff ? '/staff' : '/student'
  const statusBase = isStaff ? '/staff/complaints' : '/admin/complaints'

  async function load() {
    try {
      const c = await api(`${base}/${id}`)
      setComplaint(c)
      setAssignTo(c.assignedToId ? String(c.assignedToId) : '')
    } catch (err) {
      setError(err.message)
    }
  }

  useEffect(() => { load() }, [id])

  useEffect(() => {
    if (isAdmin) {
      api('/admin/staff').then(setStaffList).catch(() => {})
    }
  }, [isAdmin])

  async function updateStatus(status) {
    setBusy(true)
    setError('')
    try {
      setComplaint(await api(`${statusBase}/${id}/status`, {
        method: 'PATCH', body: { status },
      }))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function assign(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const updated = await api(`/admin/complaints/${id}/assign`, {
        method: 'PATCH', body: { staffId: assignTo ? Number(assignTo) : null },
      })
      setComplaint(updated)
      setNotice(assignTo ? 'Complaint assigned.' : 'Assignment removed.')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function submitRating(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      setComplaint(await api(`/complaints/${id}/rating`, {
        method: 'POST', body: { stars, comment: ratingComment },
      }))
      setNotice('Thank you for your rating!')
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
      <Link to={backTo}>&larr; Back to dashboard</Link>
      <div className="card">
        <h1>{complaint.code} — {complaint.subject}</h1>
        <p>
          <span className={`badge ${complaint.status.toLowerCase()}`}>{statusLabel(complaint.status)}</span>
          {' '}· {complaint.category} · {new Date(complaint.createdAt).toLocaleString()}
        </p>
        <p><strong>Student:</strong> {complaint.studentName} ({complaint.studentEmail})</p>
        <p><strong>Assigned to:</strong> {complaint.assignedToName || 'Not assigned'}</p>
        <p>{complaint.description}</p>

        {complaint.photoUrl && (
          <div>
            <h2>Attached Photo</h2>
            <img className="photo-full" src={fileUrl(complaint.photoUrl)} alt="Complaint photo" />
          </div>
        )}

        {notice && <div className="alert success">{notice}</div>}

        {(isAdmin || isStaff) && (
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

        {isAdmin && (
          <form onSubmit={assign} className="filters">
            <select value={assignTo} onChange={(e) => setAssignTo(e.target.value)}>
              <option value="">Not assigned</option>
              {staffList.map((s) => (
                <option key={s.id} value={s.id}>{s.name} ({s.email})</option>
              ))}
            </select>
            <button className="btn btn-primary" type="submit" disabled={busy}>Assign</button>
          </form>
        )}

        {complaint.rating && (
          <div className="card">
            <h2>Rating</h2>
            <p><span className="stars">{starsText(complaint.rating.stars)}</span>
              {' '}({complaint.rating.stars}/5)</p>
            {complaint.rating.comment && <p>“{complaint.rating.comment}”</p>}
          </div>
        )}

        {isStudent && complaint.status === 'RESOLVED' && !complaint.rating && (
          <div className="card">
            <h2>Rate this resolution</h2>
            <form onSubmit={submitRating} className="form">
              <div className="stars-picker">
                {[1, 2, 3, 4, 5].map((n) => (
                  <button type="button" key={n} className={`star-btn${stars >= n ? ' on' : ''}`}
                    onClick={() => setStars(n)} aria-label={`${n} star${n > 1 ? 's' : ''}`}>
                    ★
                  </button>
                ))}
                <span>{stars}/5</span>
              </div>
              <label>Comment (optional)
                <input value={ratingComment} onChange={(e) => setRatingComment(e.target.value)}
                  maxLength={500} placeholder="How was the resolution?" />
              </label>
              <button className="btn btn-primary" disabled={busy}>Submit Rating</button>
            </form>
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
