import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, fileUrl, getUser } from '../api.js'
import { priorityLabel, starsText, statusLabel } from './StudentDashboard.jsx'

const ALL_STATUSES = ['SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED']

export default function ComplaintDetail() {
  const { id } = useParams()
  const user = getUser()
  const isAdmin = user?.role === 'ADMIN'
  const isStaff = user?.role === 'STAFF'
  const isStudent = user?.role === 'STUDENT'
  const [complaint, setComplaint] = useState(null)
  const [staffList, setStaffList] = useState([])
  const [assignTo, setAssignTo] = useState('')
  const [statusNote, setStatusNote] = useState('')
  const [priority, setPriority] = useState('MEDIUM')
  const [updateText, setUpdateText] = useState('')
  const [reopenReason, setReopenReason] = useState('')
  const [showReopen, setShowReopen] = useState(false)
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
      setPriority(c.priority || 'MEDIUM')
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
      const updated = await api(`${statusBase}/${id}/status`, {
        method: 'PATCH', body: { status, note: statusNote || undefined },
      })
      setComplaint(updated)
      setStatusNote('')
      setNotice(`Status changed to ${statusLabel(status)}.`)
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

  async function changePriority(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const updated = await api(`/admin/complaints/${id}/priority`, {
        method: 'PATCH', body: { priority },
      })
      setComplaint(updated)
      setNotice(`Priority set to ${priorityLabel(priority)}.`)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function postUpdate(e) {
    e.preventDefault()
    if (!updateText.trim()) return
    setBusy(true)
    setError('')
    try {
      const updated = await api(`/complaints/${id}/updates`, {
        method: 'POST', body: { message: updateText.trim() },
      })
      setComplaint(updated)
      setUpdateText('')
      setNotice('Update posted — the student has been notified.')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function reopen(e) {
    e.preventDefault()
    if (!reopenReason.trim()) {
      setError('Please say why the problem is not fixed — the admin needs the reason.')
      return
    }
    setBusy(true)
    setError('')
    try {
      const updated = await api(`/complaints/${id}/reopen`, {
        method: 'POST', body: { reason: reopenReason.trim() },
      })
      setComplaint(updated)
      setShowReopen(false)
      setReopenReason('')
      setNotice('Complaint reopened — it is back in the queue.')
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

  if (error && !complaint) return <div className="alert error">{error}</div>
  if (!complaint) return <p className="muted">Loading…</p>

  // One timeline: status changes + admin/staff updates, in time order.
  const timeline = [
    ...(complaint.history || []).map((h) => ({
      kind: 'status', at: h.changedAt, h,
    })),
    ...(complaint.updates || []).map((u) => ({
      kind: 'update', at: u.createdAt, u,
    })),
  ].sort((a, b) => new Date(a.at) - new Date(b.at))

  return (
    <div>
      <Link to={backTo}>&larr; Back to dashboard</Link>
      <div className="card">
        <h1>{complaint.code} — {complaint.subject}</h1>
        <p>
          <span className={`badge ${complaint.status.toLowerCase()}`}>{statusLabel(complaint.status)}</span>
          {' '}<span className={`priority ${complaint.priority?.toLowerCase()}`}>{priorityLabel(complaint.priority)}</span>
          {' '}· {complaint.category} · {new Date(complaint.createdAt).toLocaleString()}
        </p>
        <p><strong>Student:</strong> {complaint.studentName} ({complaint.studentEmail})</p>
        <p><strong>Assigned to:</strong> {complaint.assignedToName || 'Not assigned'}</p>
        {complaint.sameIssueCount > 0 && (
          <p><strong>Same problem:</strong> {complaint.sameIssueCount} other student{complaint.sameIssueCount > 1 ? 's' : ''} reported this too.</p>
        )}
        {complaint.duplicateOfCode && (
          <p><strong>Duplicate of:</strong> {complaint.duplicateOfCode}</p>
        )}
        {complaint.resolvedAt && (
          <p><strong>Resolved:</strong> {new Date(complaint.resolvedAt).toLocaleString()}</p>
        )}
        <p>{complaint.description}</p>

        {complaint.photoUrl && (
          <div>
            <h2>Attached Photo</h2>
            <img className="photo-full" src={fileUrl(complaint.photoUrl)} alt="Complaint photo" />
          </div>
        )}

        {error && <div className="alert error">{error}</div>}
        {notice && <div className="alert success">{notice}</div>}

        {(isAdmin || isStaff) && (
          <div>
            <div className="actions">
              <span>Update status:</span>
              {ALL_STATUSES.map((s) => (
                <button key={s} className="btn btn-primary" disabled={busy || complaint.status === s}
                  onClick={() => updateStatus(s)}>
                  {statusLabel(s)}
                </button>
              ))}
            </div>
            <label className="note-label">Note with this change (optional, shown in the timeline)
              <input value={statusNote} onChange={(e) => setStatusNote(e.target.value)}
                maxLength={500} placeholder="e.g. Electrician called, visiting tomorrow" />
            </label>
          </div>
        )}

        {isAdmin && (
          <div>
            <form onSubmit={assign} className="filters">
              <select value={assignTo} onChange={(e) => setAssignTo(e.target.value)}>
                <option value="">Not assigned</option>
                {staffList.map((s) => (
                  <option key={s.id} value={s.id}>{s.name} ({s.email})</option>
                ))}
              </select>
              <button className="btn btn-primary" type="submit" disabled={busy}>Assign</button>
            </form>
            <form onSubmit={changePriority} className="filters">
              <select value={priority} onChange={(e) => setPriority(e.target.value)}>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
              <button className="btn btn-primary" type="submit" disabled={busy}>Set Priority</button>
            </form>
          </div>
        )}

        {(isAdmin || isStaff) && (
          <form onSubmit={postUpdate} className="form">
            <label>Post an update for the student
              <textarea rows={2} value={updateText} onChange={(e) => setUpdateText(e.target.value)}
                maxLength={2000} placeholder="e.g. The electrician has been called and will visit tomorrow." />
            </label>
            <button className="btn btn-primary" disabled={busy || !updateText.trim()}>Post Update</button>
          </form>
        )}

        {isStudent && complaint.status === 'RESOLVED' && (
          <div className="card">
            {!showReopen ? (
              <button className="btn btn-ghost" onClick={() => setShowReopen(true)}>
                Not fixed? Reopen this complaint
              </button>
            ) : (
              <form onSubmit={reopen} className="form">
                <label>Why is it not fixed? (required — the admin sees this)
                  <textarea rows={3} value={reopenReason} onChange={(e) => setReopenReason(e.target.value)}
                    maxLength={500} required placeholder="e.g. The computers still do not start." />
                </label>
                <div className="actions">
                  <button className="btn btn-primary" disabled={busy}>Reopen Complaint</button>
                  <button type="button" className="btn btn-ghost" onClick={() => setShowReopen(false)}>Cancel</button>
                </div>
              </form>
            )}
          </div>
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

        <h2>Timeline</h2>
        <ol className="timeline">
          {timeline.map((t, i) => (
            <li key={i}>
              {t.kind === 'status' ? (
                <>
                  <strong>{t.h.oldStatus ? `${statusLabel(t.h.oldStatus)} → ${statusLabel(t.h.newStatus)}` : `Submitted · ${statusLabel(t.h.newStatus)}`}</strong>
                  {' '}— by {t.h.changedBy} on {new Date(t.h.changedAt).toLocaleString()}
                  {t.h.note && <div className="timeline-note">“{t.h.note}”</div>}
                </>
              ) : (
                <>
                  <strong>Update by {t.u.authorName} ({t.u.authorRole})</strong>
                  {' '}— {new Date(t.u.createdAt).toLocaleString()}
                  <div className="timeline-note">{t.u.message}</div>
                </>
              )}
            </li>
          ))}
        </ol>
      </div>
    </div>
  )
}
