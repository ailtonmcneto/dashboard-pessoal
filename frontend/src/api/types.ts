// Tipos gerados do OpenAPI da API (npm run api:types). Não edite schema.d.ts à mão.
import type { components } from './schema'

type Schemas = components['schemas']

export type Me = Schemas['MeResponse']
export type AuthStatus = Schemas['StatusResponse']
export type LoginRequest = Schemas['LoginRequest']
export type RegisterRequest = Schemas['RegisterRequest']
export type EventItem = Schemas['EventResponse']
export type EventRequest = Schemas['EventRequest']
export type EventKind = EventItem['kind']
