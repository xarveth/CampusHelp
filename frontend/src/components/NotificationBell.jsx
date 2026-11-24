import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api.js'

/** Bell icon in the top bar: unread count + dropdown of notifications. */
export default function NotificationBell() {
  const [unread, setUnread] = useState(0)
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState([])

  async function refreshCount() {
    try {
      const d = await api('/notifications/unread-count')
      setUnread(d.unread || 0)
    } catch {
      // Notifications are best-effort; a failed count hides the badge.
    }
  }

  useEffect(() => {
    refreshCount()
    const t = setInterval(refreshCount, 60000)
    return () => clearInterval(t)
  }, [])

  async function toggle() {
    if (!open) {
      try {
        setItems(await api('/notifications'))
      } catch {
        setItems([])
      }
    }
    setOpen(!open)
  }

  async function markAllRead() {
    try {
      await api('/notifications/read-all', { method: 'PATCH' })
      setUnread(0)
      setItems(items.map((n) => ({ ...n, read: true })))
    } catch { /* ignore */ }
  }

  async function openNotification(n) {
    if (!n.read) {
      try {
        await api(`/notifications/${n.id}/read`, { method: 'PATCH' })
        setUnread((u) => Math.max(0, u - 1))
        setItems(items.map((x) => (x.id === n.id ? { ...x, read: true } : x)))
      } catch { /* ignore */ }
    }
    setOpen(false)
  }

  return (
    <div className="bell-wrap">
      <button className="btn btn-ghost bell-btn" onClick={toggle} aria-label="Notifications">
        🔔{unread > 0 && <span className="bell-badge">{unread}</span>}
      </button>
      {open && (
        <div className="bell-panel">
          <div className="bell-head">
            <strong>Notifications</strong>
            {unread > 0 && (
              <button className="btn btn-ghost btn-small" onClick={markAllRead}>Mark all read</button>
            )}
          </div>
          {items.length === 0 && <p className="muted bell-empty">No notifications yet.</p>}
          {items.slice(0, 20).map((n) => (
            <div key={n.id} className={`bell-item${n.read ? '' : ' unread'}`}>
              {n.complaintId ? (
                <Link to={`/complaints/${n.complaintId}`} onClick={() => openNotification(n)}>
                  {n.message}
                </Link>
              ) : (
                <span>{n.message}</span>
              )}
              <span className="muted bell-time">{new Date(n.createdAt).toLocaleString()}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
