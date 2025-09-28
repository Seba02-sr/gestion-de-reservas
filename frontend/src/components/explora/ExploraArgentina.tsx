"use client";

import { useState } from "react";
import MapaArgentina from "./MapaArgentina";
import SectionHeader from "../ui/SectionHeader";
import Section from "../ui/Section";
import { MAP_COLORS, MAP_THRESHOLDS } from "./mapColors";

interface DestinoPopular { nombre: string; hoteles: number; precioDesde: number; }

const destinosPopulares: DestinoPopular[] = [
  { nombre: "Buenos Aires", hoteles: 156, precioDesde: 8500 },
  { nombre: "Bariloche",    hoteles: 89,  precioDesde: 12000 },
  { nombre: "Mendoza",      hoteles: 67,  precioDesde: 9800 },
  { nombre: "Córdoba",      hoteles: 94,  precioDesde: 7200 },
  { nombre: "Salta",        hoteles: 43,  precioDesde: 6500 },
];

export default function ExploraArgentina() {
  const [provinciaSeleccionada, setProvinciaSeleccionada] = useState<string | null>(null);
  const onProvinciaSelect = (provincia: string) => {
    setProvinciaSeleccionada(provincia);
    // TODO: navegación/filtro
  };

  return (
    <Section className="explora" containerClassName="explora__container">
        <SectionHeader
          className="explora__header"
          titleClassName="explora__title rarn-typography-h6"
          subtitleClassName="explora__subtitle rarn-typography-p"
          title="Explora destinos en Argentina"
          subtitle="Haz clic en cualquier provincia para ver hoteles disponibles"
        />

        <div className="explora__card rarn-card rarn-card--hoverable">
          <div className="explora__grid">
            {/* Mapa + leyenda */}
            <div className="explora__map">
              <div className="explora__legend">
                <div className="explora__legend-item">
                  <span className="explora__dot" style={{ backgroundColor: MAP_COLORS.low }} />
                  <span>1–{MAP_THRESHOLDS.lowMax}</span>
                </div>
                <div className="explora__legend-item">
                  <span className="explora__dot" style={{ backgroundColor: MAP_COLORS.mid }} />
                  <span>{MAP_THRESHOLDS.lowMax + 1}–{MAP_THRESHOLDS.midMax}</span>
                </div>
                <div className="explora__legend-item">
                  <span className="explora__dot" style={{ backgroundColor: MAP_COLORS.high }} />
                  <span>{MAP_THRESHOLDS.midMax + 1}+</span>
                </div>
                <div className="explora__legend-item">
                  <span className="explora__dot" style={{ backgroundColor: MAP_COLORS.off }} />
                  <span>Sin disponibilidad</span>
                </div>
              </div>

              <div className="explora__mapbox">
                <MapaArgentina onProvinciaSelect={onProvinciaSelect} />
              </div>
            </div>

            {/* Lista derecha (igual que la tenías) */}
            <aside className="explora__list">
              <h3 className="explora__list-title rarn-typography-h6">Destinos Populares</h3>

              <ul className="explora__items">
                {destinosPopulares.map((d) => (
                  <li key={d.nombre} className="explora__item">
                    <div className="explora__item-info">
                      <h4 className="explora__item-name rarn-typography-h6">{d.nombre}</h4>
                      <p className="explora__item-meta">{d.hoteles} hoteles disponibles</p>
                    </div>
                    <div className="explora__item-price">
                      <p className="explora__price rarn-typography-h6">
                        Desde ${d.precioDesde.toLocaleString("es-AR")}
                      </p>
                      <span className="explora__pernight">por noche</span>
                    </div>
                  </li>
                ))}
              </ul>

              <button className="explora__cta rarn-btn rarn-btn--primary rarn-btn--lg">
                Ver todos los destinos
              </button>
            </aside>
          </div>
        </div>
    </Section>
  );
}
