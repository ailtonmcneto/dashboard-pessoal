import { type FormEvent, useState } from 'react'
import { Navigate } from 'react-router'
import { ApiError } from '../api/client'
import { useAuthStatus, useLogin, useMe, useRegister } from '../auth/auth'
import { Button, Card, ErrorText, Field, Input } from '../components/ui'
import { browserTimezone } from '../lib/format'

export function LoginPage() {
  const me = useMe()
  const status = useAuthStatus()
  const login = useLogin()
  const register = useRegister()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')

  if (me.data) return <Navigate to="/" replace />
  if (status.isPending) return null

  // Sem nenhum usuário ainda: a tela vira "criar conta" (uso pessoal, um cadastro só).
  const signup = status.data?.signupOpen ?? false
  const mutation = signup ? register : login
  const fieldErrors = mutation.error instanceof ApiError ? mutation.error.errors : {}

  function submit(e: FormEvent) {
    e.preventDefault()
    if (signup) {
      register.mutate({ name, email: email.trim(), password, timezone: browserTimezone() })
    } else {
      login.mutate({ email: email.trim(), password })
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-sm">
        <h1 className="text-2xl font-bold tracking-tight">Vida</h1>
        <p className="mb-6 mt-1 text-sm text-muted">
          {signup ? 'Crie sua conta para começar.' : 'Entre para ver seu painel.'}
        </p>
        <form onSubmit={submit} className="space-y-4">
          {signup && (
            <Field label="Nome" error={fieldErrors.name}>
              <Input value={name} onChange={(e) => setName(e.target.value)} required autoComplete="name" />
            </Field>
          )}
          <Field label="E-mail" error={fieldErrors.email}>
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
          </Field>
          <Field label="Senha" error={fieldErrors.password}>
            <Input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={signup ? 8 : undefined}
              autoComplete={signup ? 'new-password' : 'current-password'}
            />
          </Field>
          {Object.keys(fieldErrors).length === 0 && <ErrorText error={mutation.error} />}
          <Button type="submit" className="w-full" disabled={mutation.isPending}>
            {signup ? 'Criar conta' : 'Entrar'}
          </Button>
        </form>
      </Card>
    </div>
  )
}
