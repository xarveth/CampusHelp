import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { starsText, statusLabel } from './StudentDashboard.jsx'

/** Simple SVG/CSS bar chart (no chart library). `data` is {label: count}. */
function BarChart({ data }) {
  const entries = Object.entries(data || {})
  const max = Math.max(1, ...entries.map(([, v]) => v))
  if (entries.length === 0) return <p className="muted">No data yet.</p>
  return (
    <div className="chart">
      {entries.map(([label, value]) => (
        <div className="chart-row" key={label}>
          <span className="chart-label">{label}</span>
          <div className="chart-track">
            <div className="chart-bar" style={{ width: `${(value / max) * 100}%` }} />
          </div>
          <strong className="chart-value">{value}</strong>
        </div>
      ))}
    </div>
  )
}

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [complaints, setComplaints] = useState([])
  const [categories, setCategories] = useState([])
  const [staffList, setStaffList] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [query, setQuery] = useState('')
  const [newCategory, setNewCategory] = useState('')
  const [staffName, setStaffName] = useState('')
  const [staffEmail, setStaffEmail] = useState('')
  const [staffPassword, setStaffPassword] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function load() {
    try {
      const [s, list, cats, staff] = await Promise.all([
        api('/admin/dashboard'),
        api(`/admin/complaints${statusFilter ? `?status=${statusFilter}` : ''}`),
        api('/categories', { auth: false }),
        api('/admin/staff'),
      ])
      setStats(s)
      setComplaints(list)
      setCategories(cats)
      setStaffList(staff)
    } catch (err) {
      setError(err.message)
    }
  }

  useEffect(() => { load() }, [statusFilter])

  async function search(e) {
    e.preventDefault()
    try {
      const list = await api(`/admin/complaints?q=${encodeURIComponent(query)}`)
      setComplaints(list)
    } catch (err) {
      setError(err.message)
    }
  }

  async function addCategory(e) {
    e.preventDefault()
    if (!newCategory.trim()) return
    try {
      await api('/categories', { method: 'POST', body: { name: newCategory.trim() } })
      setNewCategory('')
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function deleteCategory(id) {
    if (!window.confirm('Delete this category? Complaints in it must be moved first.')) return
    try {
      await api(`/categories/${id}`, { method: 'DELETE' })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function addStaff(e) {
    e.preventDefault()
    setError('')
    setNotice('')
    try {
      await api('/admin/staff', {
        method: 'POST',
        body: { name: staffName.trim(), email: staffEmail.trim(), password: staffPassword },
      })
      setStaffName('')
      setStaffEmail('')
      setStaffPassword('')
      setNotice('Staff account created.')
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const statusChart = stats?.byStatus ? {
    Pending: stats.byStatus.PENDING || 0,
    'In Progress': stats.byStatus.IN_PROGRESS || 0,
    Resolved: stats.byStatus.RESOLVED || 0,
  } : {}

  return (
    <div>
      <h1>Admin Dashboard</h1>
      <p className="muted">Review all complaints, update their status, and manage categories.</p>
      {error && <div className="alert error">{error}</div>}
      {notice && <div className="alert success">{notice}</div>}

      {stats && (
        <div className="stats">
          <div className="stat"><strong>{stats.total}</strong><span>Total</span></div>
          <div className="stat"><strong>{stats.pending}</strong><span>Pending</span></div>
          <div className="stat"><strong>{stats.inProgress}</strong><span>In Progress</span></div>
          <div className="stat"><strong>{stats.resolved}</strong><span>Resolved</span></div>
          <div className="stat"><strong>{stats.averageRating == null ? '—' : `★ ${stats.averageRating}`}</strong>
            <span>Average rating ({stats.ratingCount || 0})</span></div>
        </div>
      )}

      {stats?.byCategory && (
        <div className="card">
          <h2>Complaints by Category</h2>
          <BarChart data={stats.byCategory} />
        </div>
      )}

      {stats && (
        <div className="card">
          <h2>Status Distribution</h2>
          <BarChart data={statusChart} />
        </div>
      )}

      <div className="card">
        <h2>All Complaints</h2>
        <form onSubmit={search} className="filters">
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
            <option value="">All statuses</option>
            <option value="PENDING">Pending</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
          </select>
          <input value={query} onChange={(e) => setQuery(e.target.value)}
            placeholder="Search ID, subject, student, category…" />
          <button className="btn btn-primary" type="submit">Search</button>
          <button className="btn btn-ghost" type="button" onClick={() => { setQuery(''); setStatusFilter(''); load() }}>
            Clear
          </button>
        </form>
        {complaints.length === 0 && <p className="muted">No complaints match.</p>}
        <table className="table">
          <thead>
            <tr><th>ID</th><th>Subject</th><th>Category</th><th>Student</th><th>Assigned To</th><th>Rating</th><th>Date</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {complaints.map((c) => (
              <tr key={c.id}>
                <td>{c.code}</td>
                <td>{c.subject}{c.photoUrl ? ' 📷' : ''}</td>
                <td>{c.category}</td>
                <td>{c.studentName}</td>
                <td>{c.assignedToName || '—'}</td>
                <td>{c.rating ? <span className="stars">{starsText(c.rating.stars)}</span> : '—'}</td>
                <td>{new Date(c.createdAt).toLocaleDateString()}</td>
                <td><span className={`badge ${c.status.toLowerCase()}`}>{statusLabel(c.status)}</span></td>
                <td><Link to={`/complaints/${c.id}`}>Review</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="card">
        <h2>Manage Staff</h2>
        <ul className="kv">
          {staffList.map((s) => (
            <li key={s.id}><span>{s.name} ({s.email})</span></li>
          ))}
        </ul>
        {staffList.length === 0 && <p className="muted">No staff accounts yet.</p>}
        <form onSubmit={addStaff} className="filters">
          <input value={staffName} onChange={(e) => setStaffName(e.target.value)} placeholder="Staff name" required />
          <input value={staffEmail} onChange={(e) => setStaffEmail(e.target.value)} placeholder="Staff email" type="email" required />
          <input value={staffPassword} onChange={(e) => setStaffPassword(e.target.value)} placeholder="Password (min 6 chars)" type="password" required minLength={6} />
          <button className="btn btn-primary" type="submit">Add Staff</button>
        </form>
      </div>

      <div className="card">
        <h2>Manage Categories</h2>
        <ul className="kv">
          {categories.map((c) => (
            <li key={c.id}>
              <span>{c.name}</span>
              <button className="btn btn-ghost btn-small" onClick={() => deleteCategory(c.id)}>Delete</button>
            </li>
          ))}
        </ul>
        <form onSubmit={addCategory} className="filters">
          <input value={newCategory} onChange={(e) => setNewCategory(e.target.value)} placeholder="New category name" />
          <button className="btn btn-primary" type="submit">Add Category</button>
        </form>
      </div>
    </div>
  )
}
