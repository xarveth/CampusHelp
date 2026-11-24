import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, apiForm, fileUrl, getUser } from '../api.js'

export function statusLabel(status) {
  return status === 'SUBMITTED' ? 'Submitted'
    : status === 'UNDER_REVIEW' ? 'Under Review'
    : status === 'ASSIGNED' ? 'Assigned'
    : status === 'IN_PROGRESS' ? 'In Progress'
    : status === 'RESOLVED' ? 'Resolved'
    : status === 'REOPENED' ? 'Reopened'
    : status
}

export function priorityLabel(priority) {
  return priority === 'URGENT' ? 'Urgent'
    : priority === 'HIGH' ? 'High'
    : priority === 'LOW' ? 'Low'
    : 'Medium'
}

export function starsText(stars) {
  return '★'.repeat(stars) + '☆'.repeat(5 - stars)
}

export default function StudentDashboard() {
  const user = getUser()
  const [categories, setCategories] = useState([])
  const [complaints, setComplaints] = useState([])
  const [subject, setSubject] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState('MEDIUM')
  const [photo, setPhoto] = useState(null)
  const [photoPreview, setPhotoPreview] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [busy, setBusy] = useState(false)
  const [duplicates, setDuplicates] = useState(null)
  const [statusFilter, setStatusFilter] = useState('')
  // Set when the student explicitly chose "Submit anyway" after the warning.
  const confirmedSubmit = useRef(false)

  // Editing the complaint clears a stale duplicate warning.
  function clearDuplicates() {
    if (duplicates) setDuplicates(null)
    confirmedSubmit.current = false
  }

  function onPhotoChange(e) {
    const file = e.target.files?.[0] || null
    if (file && file.size > 5 * 1024 * 1024) {
      setError('Photo must be 5 MB or smaller.')
      e.target.value = ''
      return
    }
    setPhoto(file)
    setPhotoPreview(file ? URL.createObjectURL(file) : '')
  }

  async function load() {
    try {
      const [cats, mine] = await Promise.all([
        api('/categories', { auth: false }),
        api('/complaints/mine'),
      ])
      setCategories(cats)
      setComplaints(mine)
      if (cats.length && !categoryId) setCategoryId(String(cats[0].id))
    } catch (err) {
      setError(err.message)
    }
  }

  useEffect(() => { load() }, [])

  /** First submit: check for duplicates. If any look similar, show them and wait. */
  async function submit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    if (!confirmedSubmit.current) {
      setBusy(true)
      try {
        const check = await api(
          `/complaints/check-duplicate?categoryId=${Number(categoryId)}&subject=${encodeURIComponent(subject)}&description=${encodeURIComponent(description)}`
        )
        if (check.matches && check.matches.length > 0) {
          setDuplicates(check.matches)
          setBusy(false)
          return
        }
      } catch (err) {
        // A failed duplicate check must not block submission.
        setDuplicates(null)
      }
      setBusy(false)
    }
    confirmedSubmit.current = false
    await doSubmit()
  }

  async function doSubmit() {
    setBusy(true)
    try {
      let created
      if (photo) {
        const form = new FormData()
        form.append('subject', subject)
        form.append('categoryId', String(Number(categoryId)))
        form.append('description', description)
        form.append('priority', priority)
        form.append('photo', photo)
        created = await apiForm('/complaints', { formData: form })
      } else {
        created = await api('/complaints', {
          method: 'POST',
          body: { subject, categoryId: Number(categoryId), description, priority },
        })
      }
      setSuccess(`Complaint submitted. Your complaint ID is ${created.code}.`)
      setSubject('')
      setDescription('')
      setPriority('MEDIUM')
      setPhoto(null)
      setPhotoPreview('')
      setDuplicates(null)
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function sameIssue(match) {
    try {
      await api(`/complaints/${match.id}/same-issue`, { method: 'POST' })
      setSuccess(`Noted — you were added to ${match.code} instead of filing a duplicate.`)
      setDuplicates(null)
      setSubject('')
      setDescription('')
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const visible = statusFilter
    ? complaints.filter((c) => c.status === statusFilter)
    : complaints

  return (
    <div>
      <h1>Welcome, {user?.name}</h1>
      <p className="muted">Submit a complaint and track its progress below.</p>
      {error && <div className="alert error">{error}</div>}
      {success && <div className="alert success">{success}</div>}

      <div className="card">
        <h2>Submit a Complaint</h2>
        <form onSubmit={submit} className="form">
          <label>Subject
            <input value={subject} onChange={(e) => { setSubject(e.target.value); clearDuplicates() }} required maxLength={200}
              placeholder="e.g. Computers not working" />
          </label>
          <label>Category
            <select value={categoryId} onChange={(e) => { setCategoryId(e.target.value); clearDuplicates() }} required>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </label>
          <label>Priority
            <select value={priority} onChange={(e) => setPriority(e.target.value)}>
              <option value="LOW">Low — can wait</option>
              <option value="MEDIUM">Medium — normal</option>
              <option value="HIGH">High — affects classes</option>
              <option value="URGENT">Urgent — blocks work now</option>
            </select>
          </label>
          <label>Description
            <textarea rows={4} value={description} onChange={(e) => { setDescription(e.target.value); clearDuplicates() }} required
              placeholder="Describe the problem in detail…" />
          </label>
          <label>Photo (optional, JPG/PNG/WebP, max 5 MB)
            <input type="file" accept="image/jpeg,image/png,image/webp" onChange={onPhotoChange} />
          </label>
          {photoPreview && (
            <div>
              <img className="photo-preview" src={photoPreview} alt="Photo preview" />
              <button type="button" className="btn btn-ghost btn-small"
                onClick={() => { setPhoto(null); setPhotoPreview('') }}>Remove photo</button>
            </div>
          )}
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Submitting…' : 'Submit Complaint'}</button>
        </form>

        {duplicates && (
          <div className="card dup-warning">
            <h2>Similar complaints already exist</h2>
            <p>These open complaints look like yours. If one of them is your problem too,
              join it instead of filing a duplicate — or submit anyway.</p>
            <ul className="kv">
              {duplicates.map((m) => (
                <li key={m.id}>
                  <span>
                    <Link to={`/complaints/${m.id}`}>{m.code}</Link> — {m.subject}
                    {' '}· <span className={`badge ${m.status.toLowerCase()}`}>{statusLabel(m.status)}</span>
                    {m.sameIssueCount > 0 && ` · ${m.sameIssueCount} student${m.sameIssueCount > 1 ? 's' : ''} with the same problem`}
                  </span>
                  <button className="btn btn-ghost btn-small" onClick={() => sameIssue(m)}>
                    Same problem
                  </button>
                </li>
              ))}
            </ul>
            <div className="actions">
              <button className="btn btn-primary" onClick={() => { confirmedSubmit.current = true; doSubmit() }} disabled={busy}>
                {busy ? 'Submitting…' : 'Submit anyway — this is a different problem'}
              </button>
              <button className="btn btn-ghost" onClick={() => setDuplicates(null)}>Cancel</button>
            </div>
          </div>
        )}
      </div>

      <div className="card">
        <h2>My Complaints ({complaints.length})</h2>
        <div className="filters">
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All statuses</option>
            <option value="SUBMITTED">Submitted</option>
            <option value="UNDER_REVIEW">Under Review</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="REOPENED">Reopened</option>
          </select>
        </div>
        {visible.length === 0 && <p className="muted">No complaints match.</p>}
        <table className="table">
          <thead>
            <tr><th>ID</th><th>Subject</th><th>Category</th><th>Priority</th><th>Date</th><th>Status</th><th>Rating</th><th></th></tr>
          </thead>
          <tbody>
            {visible.map((c) => (
              <tr key={c.id}>
                <td>{c.code}</td>
                <td>{c.subject}{c.photoUrl ? ' 📷' : ''}</td>
                <td>{c.category}</td>
                <td><span className={`priority ${c.priority?.toLowerCase()}`}>{priorityLabel(c.priority)}</span></td>
                <td>{new Date(c.createdAt).toLocaleDateString()}</td>
                <td><span className={`badge ${c.status.toLowerCase()}`}>{statusLabel(c.status)}</span></td>
                <td>{c.rating ? <span className="stars" title={c.rating.comment || ''}>{starsText(c.rating.stars)}</span> : '—'}</td>
                <td><Link to={`/complaints/${c.id}`}>View</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
