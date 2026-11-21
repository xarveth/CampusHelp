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
