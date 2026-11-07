import React, { useState } from "react"
import { Link, useNavigate, useSearchParams } from "react-router-dom"
import Alert from "../components/ui/Alert"
import Button from "../components/ui/Button"
import Card from "../components/ui/Card"
import { Field, Input, Select } from "../components/ui/Field"
import { listarBancos, login, registrarHuesped } from "../api/auth"
import { mensajeAmigable } from "../api/client"
import { useAuth } from "../context/auth"
import { useAsync } from "../hooks/useAsync"
import { todayISO } from "../util/date"
import { validarRegistro, type DatosRegistro, type ErroresRegistro } from "../util/validaciones"
import { destinoSeguro } from "../util/redirect"

const VACIO: DatosRegistro = {
  nombre: "", email: "", telefono: "", dni: "", username: "", password: "", fechaNacimiento: "",
  numero: "", nombreTitular: "", fechaVencimiento: "", cvc: "", idBanco: "",
}

const Registro: React.FC = () => {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { signIn } = useAuth()
  const bancos = useAsync(() => listarBancos(), [])
  const [datos, setDatos] = useState<DatosRegistro>(VACIO)
  const [errores, setErrores] = useState<ErroresRegistro>({})
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const set = (campo: keyof DatosRegistro) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setDatos((d) => ({ ...d, [campo]: e.target.value }))

  const campo = (id: keyof DatosRegistro, label: string, props: React.InputHTMLAttributes<HTMLInputElement> = {}) => (
    <Field label={label} htmlFor={id}>
      <Input id={id} value={datos[id]} onChange={set(id)} aria-invalid={!!errores[id]} {...props} />
      {errores[id] && <p className="rarn-form-error">{errores[id]}</p>}
    </Field>
  )

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    const errs = validarRegistro(datos, todayISO())
    setErrores(errs)
    if (Object.keys(errs).length) return

    setLoading(true)
    try {
      // el alta devuelve el huésped creado (con su id): no hace falta buscarlo por DNI
      const creado = await registrarHuesped({
        nombre: datos.nombre.trim(),
        email: datos.email.trim(),
        telefono: datos.telefono.trim(),
        dni: datos.dni.trim(),
        username: datos.username.trim(),
        password: datos.password,
        fechaNacimiento: datos.fechaNacimiento,
        tarjetasCredito: [
          {
            numero: datos.numero.replace(/\s+/g, ""),
            nombreTitular: datos.nombreTitular.trim(),
            fechaVencimiento: datos.fechaVencimiento,
            cvc: datos.cvc,
            esPrincipal: true,
            idBanco: Number(datos.idBanco),
          },
        ],
      })
      try {
        signIn(await login({ username: datos.username.trim(), password: datos.password }))
      } catch {
        // cuenta creada pero no se pudo iniciar sesión sola: se la manda al login
        signIn({ userId: creado.id, email: creado.email, nombre: creado.nombre })
      }
      navigate(destinoSeguro(params.get("redirect")), { replace: true })
    } catch (err) {
      setError(mensajeAmigable(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="rarn-page rarn-page--medium">
      <Card>
        <h1 className="rarn-page__title">Crear cuenta de huésped</h1>
        <p className="rarn-page__subtitle">Necesitamos una tarjeta de crédito principal para tus reservas</p>

        {error && <Alert kind="error">{error}</Alert>}
        {bancos.error && <Alert kind="error">No se pudieron cargar los bancos: {bancos.error}</Alert>}

        <form onSubmit={onSubmit} className="rarn-form" noValidate>
          <h2 className="rarn-form__section">Tus datos</h2>
          <div className="rarn-form__grid">
            {campo("nombre", "Nombre y apellido", { autoComplete: "name" })}
            {campo("email", "Email", { type: "email", autoComplete: "email" })}
            {campo("telefono", "Teléfono", { type: "tel", autoComplete: "tel" })}
            {campo("dni", "DNI", { inputMode: "numeric" })}
            {campo("fechaNacimiento", "Fecha de nacimiento", { type: "date", max: todayISO() })}
            {campo("username", "Usuario", { autoComplete: "username" })}
            {campo("password", "Contraseña", { type: "password", autoComplete: "new-password" })}
          </div>

          <h2 className="rarn-form__section">Tarjeta de crédito principal</h2>
          <div className="rarn-form__grid">
            {campo("numero", "Número de tarjeta", { inputMode: "numeric", autoComplete: "cc-number", placeholder: "5500 0000 0000 0004" })}
            {campo("nombreTitular", "Titular", { autoComplete: "cc-name" })}
            {campo("fechaVencimiento", "Vencimiento (MM/AA)", { placeholder: "12/28", autoComplete: "cc-exp" })}
            {campo("cvc", "Código de seguridad", { inputMode: "numeric", autoComplete: "cc-csc", maxLength: 4 })}
            <Field label="Banco emisor" htmlFor="idBanco">
              <Select id="idBanco" value={datos.idBanco} onChange={set("idBanco")} aria-invalid={!!errores.idBanco}>
                <option value="">{bancos.loading ? "Cargando..." : "Elegí un banco"}</option>
                {bancos.data?.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.nombre}
                  </option>
                ))}
              </Select>
              {errores.idBanco && <p className="rarn-form-error">{errores.idBanco}</p>}
            </Field>
          </div>
          <p className="rarn-form__hint">Es un entorno de práctica: usá tarjetas de prueba. El CVC no se guarda en las respuestas de la API.</p>

          <Button type="submit" className="rarn-btn--block" disabled={loading}>
            {loading ? "Creando cuenta..." : "Crear cuenta"}
          </Button>
        </form>

        <p className="mt-6 text-center text-sm">
          ¿Ya tenés cuenta?{" "}
          <Link to="/login" className="rarn-link">
            Iniciá sesión
          </Link>
        </p>
      </Card>
    </main>
  )
}

export default Registro
