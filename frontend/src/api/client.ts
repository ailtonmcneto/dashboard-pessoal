// Cliente HTTP da API. Mesmo domínio (proxy do Vite em dev): o cookie de sessão vai sozinho;
// em requisições que alteram dados, o token do cookie XSRF-TOKEN volta no header X-XSRF-TOKEN.

export class ApiError extends Error {
  readonly status: number
  readonly errors: Record<string, string>

  constructor(status: number, detail: string, errors: Record<string, string> = {}) {
    super(detail)
    this.status = status
    this.errors = errors
  }
}

function readCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith(`${name}=`))
    ?.slice(name.length + 1)
}

let csrfRequest: Promise<void> | undefined

/** Garante o cookie XSRF-TOKEN antes da primeira escrita. */
async function ensureCsrf(): Promise<string> {
  if (!readCookie('XSRF-TOKEN')) {
    csrfRequest ??= fetch('/api/auth/csrf').then(() => undefined)
    await csrfRequest
    csrfRequest = undefined
  }
  return decodeURIComponent(readCookie('XSRF-TOKEN') ?? '')
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

export async function api<T>(path: string, method: Method = 'GET', body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (method !== 'GET') headers['X-XSRF-TOKEN'] = await ensureCsrf()

  const res = await fetch(`/api${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!res.ok) {
    const problem = await res.json().catch(() => ({}))
    throw new ApiError(res.status, problem.detail ?? `Erro ${res.status}`, problem.errors)
  }
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}
