import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, getUser } from '../api.js'
import { statusLabel } from './StudentDashboard.jsx'

/** Staff view — only the complaints assigned to the logged-in staff member. */
export default function StaffDashboard() {
  const user = getUser()
  const [complaints, setComplaints] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    api('/staff/complaints').then(setComplaints).catch((err) => setError(err.message))
  }, [])

  return (
    <div>
      <h1>Welcome, {user?.name}</h1>
      <p className="muted">Complaints assigned to you. Open one to update its status.</p>
      {error && <div className="alert error">{error}</div>}
      <div className="card">
        <h2>Assigned Complaints ({complaints.length})</h2>
        {complaints.length === 0 && <p className="muted">No complaints are assigned to you yet.</p>}
        <table className="table">
          <thead>
            <tr><th>ID</th><th>Subject</th><th>Category</th><th>Student</th><th>Date</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {complaints.map((c) => (
              <tr key={c.id}>
                <td>{c.code}</td>
                <td>{c.subject}{c.photoUrl ? ' 📷' : ''}</td>
                <td>{c.category}</td>
                <td>{c.studentName}</td>
                <td>{new Date(c.createdAt).toLocaleDateString()}</td>
                <td><span className={`badge ${c.status.toLowerCase()}`}>{statusLabel(c.status)}</span></td>
                <td><Link to={`/complaints/${c.id}`}>Open</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
