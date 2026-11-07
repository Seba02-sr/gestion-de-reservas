import { beforeEach, describe, expect, it, vi } from "vitest"
import { screen, waitFor } from "@testing-library/react"
import userEvent from "@testing-library/user-event"
import Registro from "./Registro"
import { renderConApp } from "../test/render"
import { ApiError } from "../api/client"

vi.mock("../api/auth", () => ({
  listarBancos: vi.fn(),
  registrarHuesped: vi.fn(),
  login: vi.fn(),
}))
import { listarBancos, login, registrarHuesped } from "../api/auth"

async function completar(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText("Nombre y apellido"), "Ana Gómez")
  await user.type(screen.getByLabelText("Email"), "ana@example.com")
  await user.type(screen.getByLabelText("Teléfono"), "342-555")
  await user.type(screen.getByLabelText("DNI"), "30111222")
  await user.type(screen.getByLabelText("Fecha de nacimiento"), "1990-05-20")
  await user.type(screen.getByLabelText("Usuario"), "ana")
  await user.type(screen.getByLabelText("Contraseña"), "secreto")
  await user.type(screen.getByLabelText("Número de tarjeta"), "5500 0000 0000 0004")
  await user.type(screen.getByLabelText("Titular"), "Ana Gomez")
  await user.type(screen.getByLabelText(/Vencimiento/), "12/28")
  await user.type(screen.getByLabelText("Código de seguridad"), "123")
  await user.selectOptions(screen.getByLabelText("Banco emisor"), "2")
}

describe("Registro de huésped", () => {
  beforeEach(() => {
    vi.mocked(listarBancos).mockResolvedValue([
      { id: 1, nombre: "Banco Nación" },
      { id: 2, nombre: "Banco Galicia" },
    ])
    vi.mocked(registrarHuesped).mockReset()
    vi.mocked(login).mockReset()
  })

  it("carga los bancos desde la API", async () => {
    renderConApp(<Registro />, "/registro")
    expect(await screen.findByRole("option", { name: "Banco Galicia" })).toBeInTheDocument()
  })

  it("valida en el cliente antes de llamar al backend", async () => {
    const user = userEvent.setup()
    renderConApp(<Registro />, "/registro")
    await user.click(screen.getByRole("button", { name: /crear cuenta/i }))
    expect(await screen.findByText("Email inválido")).toBeInTheDocument()
    expect(screen.getByText("Elegí el banco emisor")).toBeInTheDocument()
    expect(registrarHuesped).not.toHaveBeenCalled()
  })

  it("registra con tarjeta principal, usa el id devuelto e inicia sesión", async () => {
    vi.mocked(registrarHuesped).mockResolvedValue({
      id: 42, nombre: "Ana Gómez", email: "ana@example.com", telefono: "", dni: "", username: "ana", fechaNacimiento: "", tarjetaCredito: [],
    })
    vi.mocked(login).mockResolvedValue({ userId: 42, email: "ana@example.com", nombre: "Ana Gómez" })
    const user = userEvent.setup()
    renderConApp(<Registro />, "/registro")
    await screen.findByRole("option", { name: "Banco Galicia" })
    await completar(user)
    await user.click(screen.getByRole("button", { name: /crear cuenta/i }))

    await waitFor(() => expect(registrarHuesped).toHaveBeenCalledTimes(1))
    const req = vi.mocked(registrarHuesped).mock.calls[0][0]
    expect(req.tarjetasCredito).toHaveLength(1)
    expect(req.tarjetasCredito[0]).toMatchObject({ numero: "5500000000000004", esPrincipal: true, idBanco: 2, cvc: "123" })
    expect(login).toHaveBeenCalledWith({ username: "ana", password: "secreto" })
    await waitFor(() => expect(JSON.parse(localStorage.getItem("user")!).userId).toBe(42))
    expect(screen.getByTestId("location")).toHaveTextContent(/^\/$/)
  })

  it("muestra el error del backend (409/400) sin navegar", async () => {
    vi.mocked(registrarHuesped).mockRejectedValue(new ApiError(409, "Ya existe un usuario con ese DNI"))
    const user = userEvent.setup()
    renderConApp(<Registro />, "/registro")
    await screen.findByRole("option", { name: "Banco Galicia" })
    await completar(user)
    await user.click(screen.getByRole("button", { name: /crear cuenta/i }))
    expect(await screen.findByRole("alert")).toHaveTextContent("Ya existe un usuario con ese DNI")
    expect(screen.getByTestId("location")).toHaveTextContent("/registro")
  })
})
