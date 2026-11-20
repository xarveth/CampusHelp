import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, getUser } from '../api.js'

export function statusLabel(status) {
  return status === 'IN_PROGRESS' ? 'In Progress'
    : status === 'RESOLVED' ? 'Resolved' : 'Pending'
}

export default function StudentDashboard() {
  const user = getUser()
  const [categories, setCategories] = useState([])
  const [complaints, setComplaints] = useState([])
  const [subject, setSubject] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [busy, setBusy] = useState(false)

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
      const created = await api('/complaints', {
        method: 'POST',
        body: { subject, categoryId: Number(categoryId), description },
      })
      setSuccess(`Complaint submitted. Your complaint ID is ${created.code}.`)
      setSubject('')
      setDescription('')
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
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Submitting…' : 'Submit Complaint'}</button>
        </form>
      </div>

      <div className="card">
        <h2>My Complaints ({complaints.length})</h2>
        {complaints.length === 0 && <p className="muted">You have not submitted any complaints yet.</p>}
        <table className="table">
          <thead>
            <tr><th>ID</th><th>Subject</th><th>Category</th><th>Date</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {complaints.map((c) => (
              <tr key={c.id}>
                <td>{c.code}</td>
                <td>{c.subject}</td>
                <td>{c.category}</td>
                <td>{new Date(c.createdAt).toLocaleDateString()}</td>
                <td><span className={`badge ${c.status.toLowerCase()}`}>{statusLabel(c.status)}</span></td>
                <td><Link to={`/complaints/${c.id}`}>View</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
