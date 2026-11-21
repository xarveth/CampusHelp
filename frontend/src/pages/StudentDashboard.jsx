import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, apiForm, fileUrl, getUser } from '../api.js'

export function statusLabel(status) {
  return status === 'IN_PROGRESS' ? 'In Progress'
    : status === 'RESOLVED' ? 'Resolved' : 'Pending'
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
  const [photo, setPhoto] = useState(null)
  const [photoPreview, setPhotoPreview] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [busy, setBusy] = useState(false)

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

  async function submit(e) {
    e.preventDefault()
    setError('')
    setSuccess('')
    setBusy(true)
    try {
      let created
      if (photo) {
        const form = new FormData()
        form.append('subject', subject)
        form.append('categoryId', String(Number(categoryId)))
        form.append('description', description)
        form.append('photo', photo)
        created = await apiForm('/complaints', { formData: form })
      } else {
        created = await api('/complaints', {
          method: 'POST',
          body: { subject, categoryId: Number(categoryId), description },
        })
      }
      setSuccess(`Complaint submitted. Your complaint ID is ${created.code}.`)
      setSubject('')
      setDescription('')
      setPhoto(null)
      setPhotoPreview('')
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

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
            <input value={subject} onChange={(e) => setSubject(e.target.value)} required maxLength={200}
              placeholder="e.g. Computers not working" />
          </label>
          <label>Category
            <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)} required>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </label>
          <label>Description
            <textarea rows={4} value={description} onChange={(e) => setDescription(e.target.value)} required
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
      </div>

      <div className="card">
        <h2>My Complaints ({complaints.length})</h2>
        {complaints.length === 0 && <p className="muted">You have not submitted any complaints yet.</p>}
        <table className="table">
          <thead>
            <tr><th>ID</th><th>Subject</th><th>Category</th><th>Date</th><th>Status</th><th>Rating</th><th></th></tr>
          </thead>
          <tbody>
            {complaints.map((c) => (
              <tr key={c.id}>
                <td>{c.code}</td>
                <td>{c.subject}{c.photoUrl ? ' 📷' : ''}</td>
                <td>{c.category}</td>
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
