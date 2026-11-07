/** Solo se aceptan redirecciones internas (evita un open-redirect por ?redirect=http://sitio-malo). */
export function destinoSeguro(redirect: string | null): string {
  return redirect && redirect.startsWith("/") && !redirect.startsWith("//") ? redirect : "/"
}
