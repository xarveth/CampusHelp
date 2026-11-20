import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { statusLabel } from './StudentDashboard.jsx'

export default function AdminDashboard() {
  const [stats, setStats] = useState(null)
  const [complaints, setComplaints] = useState([])
  const [categories, setCategories] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [query, setQuery] = useState('')
  const [newCategory, setNewCategory] = useState('')
  const [error, setError] = useState('')

  async function load() {
    try {
      const [s, list, cats] = await Promise.all([
        api('/admin/dashboard'),
        api(`/admin/complaints${statusFilter ? `?status=${statusFilter}` : ''}`),
        api('/categories', { auth: false }),
      ])
      setStats(s)
      setComplaints(list)
      setCategories(cats)
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

  return (
    <div>
      <h1>Admin Dashboard</h1>
      <p className="muted">Review all complaints, update their status, and manage categories.</p>
      {error && <div className="alert error">{error}</div>}

      {stats && (
        <div className="stats">
          <div className="stat"><strong>{stats.total}</strong><span>Total</span></div>
          <div className="stat"><strong>{stats.pending}</strong><span>Pending</span></div>
          <div className="stat"><strong>{stats.inProgress}</strong><span>In Progress</span></div>
          <div className="stat"><strong>{stats.resolved}</strong><span>Resolved</span></div>
        </div>
      )}

      {stats?.byCategory && (
        <div className="card">
          <h2>Complaints by Category</h2>
          <ul className="kv">
            {Object.entries(stats.byCategory).map(([name, count]) => (
              <li key={name}><span>{name}</span><strong>{count}</strong></li>
            ))}
          </ul>
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
            <tr><th>ID</th><th>Subject</th><th>Category</th><th>Student</th><th>Date</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {complaints.map((c) => (
              <tr key={c.id}>
                <td>{c.code}</td>
                <td>{c.subject}</td>
                <td>{c.category}</td>
                <td>{c.studentName}</td>
                <td>{new Date(c.createdAt).toLocaleDateString()}</td>
                <td><span className={`badge ${c.status.toLowerCase()}`}>{statusLabel(c.status)}</span></td>
                <td><Link to={`/complaints/${c.id}`}>Review</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
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
