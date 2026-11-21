const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

function getToken() {
  return localStorage.getItem('campushelp_token')
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem('campushelp_user') || 'null')
  } catch {
    return null
  }
}

export function saveAuth(data) {
  localStorage.setItem('campushelp_token', data.token)
  localStorage.setItem('campushelp_user', JSON.stringify({
    id: data.userId, name: data.name, email: data.email, role: data.role,
  }))
}

export function logout() {
  localStorage.removeItem('campushelp_token')
  localStorage.removeItem('campushelp_user')
}

export async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth && getToken()) headers['Authorization'] = `Bearer ${getToken()}`
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) {
    throw new Error((data && data.error) || `Request failed (${res.status})`)
  }
  return data
}

/** Multipart (FormData) request — e.g. complaint submission with a photo. */
export async function apiForm(path, { method = 'POST', formData } = {}) {
  const headers = {}
  if (getToken()) headers['Authorization'] = `Bearer ${getToken()}`
  const res = await fetch(`${API_BASE}${path}`, { method, headers, body: formData })
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) {
    throw new Error((data && data.error) || `Request failed (${res.status})`)
  }
  return data
}

/** Turn a backend photo path (/api/uploads/...) into a full URL for <img>. */
export function fileUrl(path) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  // Already an absolute /api path (e.g. when VITE_API_URL is '/api' behind
  // a same-origin proxy) — use it as-is instead of doubling the prefix.
  if (path.startsWith('/api')) return path
  return `${API_BASE}${path}`
}
