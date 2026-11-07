import { beforeEach, describe, expect, it, vi } from "vitest"
import { screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import Login from "./Login"
import { renderConApp } from "../test/render"
import { ApiError } from "../api/client"

vi.mock("../api/auth", () => ({ login: vi.fn(), loginConGoogle: vi.fn() }))
import { login } from "../api/auth"

describe("Login", () => {
  beforeEach(() => {
    // con llaves: si el callback devuelve el mock, Vitest lo ejecuta como "cleanup"
    vi.mocked(login).mockReset()
  })

  it("inicia sesión, guarda el usuario y vuelve a la ruta pedida", async () => {
    vi.mocked(login).mockResolvedValue({ userId: 5, email: "ana@x.com", nombre: "Ana" })
    const user = userEvent.setup()
    renderConApp(<Login />, "/login?redirect=%2Fmis-reservas")

    await user.type(screen.getByLabelText("Usuario"), "ana")
    await user.type(screen.getByLabelText("Contraseña"), "secreto")
    await user.click(screen.getByRole("button", { name: /iniciar sesión/i }))

    await waitFor(() => expect(login).toHaveBeenCalledWith({ username: "ana", password: "secreto" }))
    expect(JSON.parse(localStorage.getItem("user")!)).toMatchObject({ userId: 5 })
    await waitFor(() => expect(screen.getByTestId("location")).toHaveTextContent("/mis-reservas"))
  })

  it("401: muestra el mensaje y no guarda sesión", async () => {
    vi.mocked(login).mockRejectedValue(new ApiError(401, "Usuario o contraseña incorrectos"))
    const user = userEvent.setup()
    renderConApp(<Login />, "/login")
    await user.type(screen.getByLabelText("Usuario"), "ana")
    await user.type(screen.getByLabelText("Contraseña"), "mala")
    await user.click(screen.getByRole("button", { name: /iniciar sesión/i }))

    expect(await screen.findByRole("alert")).toHaveTextContent("Usuario o contraseña incorrectos")
    expect(localStorage.getItem("user")).toBeNull()
  })

  it("503: mensaje de servicio no disponible", async () => {
    vi.mocked(login).mockRejectedValue(new ApiError(503, "user-svc no responde"))
    const user = userEvent.setup()
    renderConApp(<Login />, "/login")
    await user.type(screen.getByLabelText("Usuario"), "ana")
    await user.type(screen.getByLabelText("Contraseña"), "x")
    await user.click(screen.getByRole("button", { name: /iniciar sesión/i }))
    expect(await screen.findByRole("alert")).toHaveTextContent(/no está disponible, probá en unos segundos/i)
  })

  it("sin VITE_GOOGLE_CLIENT_ID no hay botón ni separador de Google", () => {
    vi.stubEnv("VITE_GOOGLE_CLIENT_ID", "")
    renderConApp(<Login />, "/login")
    expect(screen.queryByLabelText(/botón google login/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/google/i)).not.toBeInTheDocument()
  })

  it("con VITE_GOOGLE_CLIENT_ID se renderiza el contenedor del botón de Google", () => {
    vi.stubEnv("VITE_GOOGLE_CLIENT_ID", "abc.apps.googleusercontent.com")
    renderConApp(<Login />, "/login")
    expect(screen.getByLabelText(/botón google login/i)).toBeInTheDocument()
  })

  it("ignora redirecciones a sitios externos", async () => {
    vi.mocked(login).mockResolvedValue({ userId: 1, email: "a@x.com" })
    const user = userEvent.setup()
    renderConApp(<Login />, "/login?redirect=https%3A%2F%2Fmalo.com")
    await user.type(screen.getByLabelText("Usuario"), "a")
    await user.type(screen.getByLabelText("Contraseña"), "b")
    await user.click(screen.getByRole("button", { name: /iniciar sesión/i }))
    await waitFor(() => expect(screen.getByTestId("location")).toHaveTextContent(/^\/$/))
  })
})
