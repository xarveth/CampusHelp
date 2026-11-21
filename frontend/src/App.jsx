import { Navigate, Route, Routes, Link, useNavigate } from 'react-router-dom'
import { getUser, logout } from './api.js'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import StudentDashboard from './pages/StudentDashboard.jsx'
import AdminDashboard from './pages/AdminDashboard.jsx'
import StaffDashboard from './pages/StaffDashboard.jsx'
import ComplaintDetail from './pages/ComplaintDetail.jsx'

function homeFor(role) {
  return role === 'ADMIN' ? '/admin' : role === 'STAFF' ? '/staff' : '/student'
}

function roleLabel(role) {
  return role === 'ADMIN' ? 'Admin' : role === 'STAFF' ? 'Staff' : 'Student'
}

function Protected({ children, role }) {
  const user = getUser()
  if (!user) return <Navigate to="/login" replace />
  if (role && user.role !== role) {
    return <Navigate to={homeFor(user.role)} replace />
  }
  return children
}

function Home() {
  const user = getUser()
  if (user) return <Navigate to={homeFor(user.role)} replace />
  return <Navigate to="/login" replace />
}

export default function App() {
  const user = getUser()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="app">
      <header className="topbar">
        <Link to="/" className="brand">CampusHelp</Link>
        <span className="tagline">Student Grievance &amp; Complaint Management</span>
        {user && (
          <div className="userbox">
            <span>{user.name} · {roleLabel(user.role)}</span>
            <button className="btn btn-ghost" onClick={handleLogout}>Logout</button>
          </div>
        )}
      </header>

      <main className="container">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/student" element={<Protected role="STUDENT"><StudentDashboard /></Protected>} />
          <Route path="/admin" element={<Protected role="ADMIN"><AdminDashboard /></Protected>} />
          <Route path="/staff" element={<Protected role="STAFF"><StaffDashboard /></Protected>} />
          <Route path="/complaints/:id" element={<Protected><ComplaintDetail /></Protected>} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>

      <footer className="footer">
        CampusHelp · React + Spring Boot + MySQL · Mini Project
      </footer>
    </div>
  )
}
