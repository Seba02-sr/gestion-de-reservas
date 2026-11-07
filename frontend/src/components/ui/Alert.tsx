import React from "react"

type Props = {
  kind?: "error" | "success" | "info"
  children: React.ReactNode
  className?: string
}

/** Mensaje de estado (errores del backend, éxito, información). */
export default function Alert({ kind = "error", children, className = "" }: Props) {
  return (
    <div role={kind === "error" ? "alert" : "status"} className={`rarn-alert rarn-alert--${kind} ${className}`}>
      {children}
    </div>
  )
}
