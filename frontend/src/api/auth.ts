import { get, post } from "./client"
import type { AuthResponse, BancoResponse, HuespedRequest, HuespedResponse, LoginRequest } from "./types"

// Rutas por el gateway: /users/** -> user-svc (StripPrefix=1). UserController es /users, de ahí /users/users/...

export const login = (req: LoginRequest) => post<AuthResponse>("/users/auth/login", req)

export const loginConGoogle = (credential: string) => post<AuthResponse>("/users/auth/google", { credential })

/** El alta responde 201 con el huésped creado (id incluido) y el header Location. */
export const registrarHuesped = (req: HuespedRequest) => post<HuespedResponse>("/users/users/huesped", req)

export const listarBancos = () => get<BancoResponse[]>("/users/bancos")
