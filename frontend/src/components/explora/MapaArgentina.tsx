// src/components/explora/MapaArgentina.tsx
"use client";

import { useEffect, useRef } from "react";
import { MAP_COLORS, MAP_THRESHOLDS } from "./mapColors";

interface MapaArgentinaProps {
  onProvinciaSelect: (provincia: string) => void;
}

export default function MapaArgentina({ onProvinciaSelect }: MapaArgentinaProps) {
  const chartRef = useRef<HTMLDivElement>(null);
  const rootRef = useRef<any>(null);

  // refs para evitar recrear el mapa y para la selección
  const onSelectRef = useRef(onProvinciaSelect);
  const selectedNameRef = useRef<string | null>(null);
  useEffect(() => { onSelectRef.current = onProvinciaSelect; }, [onProvinciaSelect]);

  useEffect(() => {
    const load = async () => {
      try {
        const am5 = await import("@amcharts/amcharts5");
        const am5map = await import("@amcharts/amcharts5/map");
        const geo = await import("@amcharts/amcharts5-geodata/argentinaHigh");
        const Animated = await import("@amcharts/amcharts5/themes/Animated");

        if (!chartRef.current) return;
        rootRef.current?.dispose();
        selectedNameRef.current = null;

        // Datos ficticios (luego van a venir de la API)
        const counts: Record<string, number> = {
          "Buenos Aires": 230, "Ciudad de Buenos Aires": 190, "Catamarca": 18, "Chaco": 45, "Chubut": 33,
          "Córdoba": 120, "Corrientes": 52, "Entre Ríos": 67, "Formosa": 8, "Jujuy": 21, "La Pampa": 12,
          "La Rioja": 9, "Mendoza": 140, "Misiones": 58, "Neuquén": 36, "Río Negro": 74, "Salta": 65,
          "San Juan": 22, "San Luis": 17, "Santa Cruz": 14, "Santa Fe": 132, "Santiago del Estero": 26,
          "Tierra del Fuego": 11, "Tucumán": 88,
        };

        const colorFor = (n?: number) => {
          if (!n || n <= 0) return MAP_COLORS.off;
          if (n <= MAP_THRESHOLDS.lowMax) return MAP_COLORS.low;
          if (n <= MAP_THRESHOLDS.midMax) return MAP_COLORS.mid;
          return MAP_COLORS.high;
        };

        const root = am5.Root.new(chartRef.current);
        rootRef.current = root;
        root.setThemes([Animated.default.new(root)]);

        const chart = root.container.children.push(
          am5map.MapChart.new(root, {
            panX: "none",
            panY: "none",
            wheelX: "none",
            wheelY: "none",
            projection: am5map.geoMercator(),
          })
        );

        // Serie base (rellenos por rango)
        const baseSeries = chart.series.push(
          am5map.MapPolygonSeries.new(root, { geoJSON: geo.default ?? geo })
        );
        baseSeries.mapPolygons.template.setAll({
          stroke: am5.color("#ffffff"),
          strokeWidth: 1,
          tooltipText: "{name}",
          interactive: true,
          cursorOverStyle: "pointer",
        });

        // Serie highlight (solo borde del seleccionado, arriba de la base)
        const highlightSeries = chart.series.push(
          am5map.MapPolygonSeries.new(root, { geoJSON: geo.default ?? geo })
        );

        const PRIMARY_STROKE =
          getComputedStyle(document.documentElement).getPropertyValue("--map-stroke-selected").trim() || "#0F172A";
          console.log("PRIMARY_STROKE:", PRIMARY_STROKE);
        highlightSeries.mapPolygons.template.setAll({
          fillOpacity: 0,
          stroke: am5.color(PRIMARY_STROKE), // gris intermedio (≃ $rarn-color-gray-border-strong)
          strokeWidth: 2,
          interactive: false,
          cursorOverStyle: "default",
          visible: false, // por default oculto
        });
        // aseguramos que quede por encima de la base
        // highlightSeries.set("zIndex", 10); // Removed: not a valid property
        highlightSeries.toFront?.(); // Use toFront to bring highlightSeries above baseSeries

        // Pintar base por rangos y ocultar todos los highlight
        baseSeries.events.on("datavalidated", () => {
          baseSeries.mapPolygons.each((poly: any) => {
            const name: string = poly.dataItem?.dataContext?.name ?? "";
            const n = counts[name] ?? 0;
            poly.setAll({
              fill: am5.color(colorFor(n)),
              tooltipText: `${name}\nHoteles: ${n > 0 ? n : "sin disponibilidad"}`,
            });
          });
        });
        highlightSeries.events.on("datavalidated", () => {
          highlightSeries.mapPolygons.each((p: any) => p.set("visible", false));
        });

        // Hover suave
        baseSeries.mapPolygons.template.states.create("hover", { fillOpacity: 0.9 });

        // Helpers
        const findHighlightByName = (name: string) => {
          let found: any = null;
          highlightSeries.mapPolygons.each((p: any) => {
            const n = p.dataItem?.dataContext?.name ?? "";
            if (n === name) { found = p; }
          });
          return found;
        };

        const selectByName = (name: string) => {
          // toggle: si ya estaba seleccionado → deselecciona
          if (selectedNameRef.current === name) {
            const prev = findHighlightByName(name);
            prev?.setAll({ visible: false, zIndex: 0 });
            selectedNameRef.current = null;
            return;
          }

          // apaga el anterior (si existe)
          if (selectedNameRef.current) {
            const prev = findHighlightByName(selectedNameRef.current);
            prev?.setAll({ visible: false, zIndex: 0 });
          }

          // muestra el nuevo
          const hi = findHighlightByName(name);
          hi?.setAll({ visible: true, zIndex: 20 });
          hi?.toFront?.();

          selectedNameRef.current = name;
        };

        // Click → seleccionar
        baseSeries.mapPolygons.template.events.on("click", (ev: any) => {
          const name: string = ev.target?.dataItem?.dataContext?.name ?? "";
          if (!name) return;
          selectByName(name);
          onSelectRef.current?.(name);
        });
      } catch (e) {
        console.error("MapaArgentina error:", e);
      }
    };

    load();
    return () => { rootRef.current?.dispose(); rootRef.current = null; };
    // deps vacías: no se re-crea al click → sin titileo
  }, []);

  return <div ref={chartRef} className="mapa-ar" />;
}
