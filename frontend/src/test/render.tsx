import React from "react"
import { render } from "@testing-library/react"
import { MemoryRouter } from "react-router-dom"
import { AuthProvider } from "../context/AuthContext"
import LocationProbe from "./LocationProbe"

export function renderConApp(ui: React.ReactElement, ruta = "/") {
  return render(
    <MemoryRouter initialEntries={[ruta]}>
      <AuthProvider>
        {ui}
        <LocationProbe />
      </AuthProvider>
    </MemoryRouter>
  )
}
