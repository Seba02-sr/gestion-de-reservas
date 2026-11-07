import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { MapPinIcon, CalendarIcon, UserGroupIcon, MagnifyingGlassIcon } from "@heroicons/react/24/outline";
import Button from "../ui/Button";
import { Field, InputIcon, SelectIcon } from "../ui/Field";
import { todayISO, isPastDate, isInvalidCheckOut } from "../../util/date";
import { CIUDADES, RADIO_DEFAULT_KM } from "../../util/ciudades";

const UBICACION = "ubicacion";
const CUALQUIERA = "";

type Coords = { lat: number; lng: number };

export default function SearchBar() {
  const navigate = useNavigate();
  const [destino, setDestino] = useState(CUALQUIERA);
  const [coords, setCoords] = useState<Coords | null>(null);
  const [geoError, setGeoError] = useState<string | null>(null);
  const [radioKm, setRadioKm] = useState(String(RADIO_DEFAULT_KM));
  const [checkIn, setCheckIn] = useState("");
  const [checkOut, setCheckOut] = useState("");
  const [huespedes, setHuespedes] = useState("1");
  const [errors, setErrors] = useState<{ checkIn?: string; checkOut?: string }>({});

  const today = useMemo(() => todayISO(), []);

  useEffect(() => {
    const nextErrors: typeof errors = {};
    if (isPastDate(checkIn)) nextErrors.checkIn = "El check-in no puede ser anterior a hoy.";
    if (isInvalidCheckOut(checkIn, checkOut)) {
      nextErrors.checkOut = `El check-out no puede ser anterior a ${checkIn ? "la fecha de check-in" : "hoy"}.`;
    }
    if (checkIn && checkOut && checkOut === checkIn) {
      nextErrors.checkOut = "El check-out debe ser posterior al check-in.";
    }
    if (!!checkIn !== !!checkOut && !nextErrors.checkIn && !nextErrors.checkOut) {
      if (checkIn) nextErrors.checkOut = "Indicá también el check-out.";
      else nextErrors.checkIn = "Indicá también el check-in.";
    }
    setErrors(nextErrors);
  }, [checkIn, checkOut]);

  const onDestino = (valor: string) => {
    setDestino(valor);
    setGeoError(null);
    setCoords(null);
    if (valor === UBICACION) {
      if (!navigator.geolocation) {
        setGeoError("Tu navegador no permite obtener la ubicación.");
        return;
      }
      navigator.geolocation.getCurrentPosition(
        (pos) => setCoords({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
        () => setGeoError("No pudimos obtener tu ubicación. Elegí una ciudad de la lista."),
        { timeout: 8000 }
      );
    }
  };

  const onSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (Object.keys(errors).length) return;

    const params = new URLSearchParams();
    if (checkIn && checkOut) {
      params.set("checkIn", checkIn);
      params.set("checkOut", checkOut);
    }
    params.set("cantidadHuespedes", String(parseInt(huespedes, 10)));

    const ciudad = CIUDADES.find((c) => c.id === destino);
    const punto = ciudad ? { lat: ciudad.lat, lng: ciudad.lng } : destino === UBICACION ? coords : null;
    if (punto) {
      params.set("lat", String(punto.lat));
      params.set("lng", String(punto.lng));
      params.set("radioKm", radioKm);
      if (ciudad) params.set("destino", ciudad.nombre);
    }
    navigate(`/buscar?${params.toString()}`);
  };

  const esperandoUbicacion = destino === UBICACION && !coords && !geoError;
  const mostrarRadio = destino !== CUALQUIERA;

  return (
    <form className="hero__form searchbar" onSubmit={onSubmit} noValidate>
      <div className="searchbar__grid">
        <Field label="Destino" htmlFor="destino">
          <SelectIcon
            id="destino"
            icon={<MapPinIcon className="rarn-input-icon__svg h-5 w-5" />}
            value={destino}
            onChange={(e) => onDestino(e.target.value)}
          >
            <option value={CUALQUIERA}>Cualquier lugar</option>
            {CIUDADES.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre}
              </option>
            ))}
            <option value={UBICACION}>Cerca de mi ubicación</option>
          </SelectIcon>
          {geoError && <p className="rarn-form-error">{geoError}</p>}
          {esperandoUbicacion && <p className="rarn-form__hint">Obteniendo tu ubicación...</p>}
        </Field>

        <Field label="Check-in" htmlFor="checkin">
          <InputIcon
            id="checkin"
            icon={<CalendarIcon className="rarn-input-icon__svg h-5 w-5" />}
            type="date"
            min={today}
            value={checkIn}
            onChange={(e) => setCheckIn(e.target.value)}
          />
          {errors.checkIn && <p className="rarn-form-error">{errors.checkIn}</p>}
        </Field>

        <Field label="Check-out" htmlFor="checkout">
          <InputIcon
            id="checkout"
            icon={<CalendarIcon className="rarn-input-icon__svg h-5 w-5" />}
            type="date"
            min={checkIn || today}
            value={checkOut}
            onChange={(e) => setCheckOut(e.target.value)}
          />
          {errors.checkOut && <p className="rarn-form-error">{errors.checkOut}</p>}
        </Field>

        <Field label="Huéspedes" htmlFor="huespedes">
          <SelectIcon
            id="huespedes"
            icon={<UserGroupIcon className="rarn-input-icon__svg h-5 w-5" />}
            value={huespedes}
            onChange={(e) => setHuespedes(e.target.value)}
          >
            <option value="1">1 huésped</option>
            <option value="2">2 huéspedes</option>
            <option value="3">3 huéspedes</option>
            <option value="4">4 huéspedes</option>
            <option value="5">5+ huéspedes</option>
          </SelectIcon>
        </Field>
      </div>

      {mostrarRadio && (
        <div className="searchbar__radio mt-4 max-w-xs">
          <Field label="Distancia máxima" htmlFor="radio">
            <SelectIcon
              id="radio"
              icon={<MapPinIcon className="rarn-input-icon__svg h-5 w-5" />}
              value={radioKm}
              onChange={(e) => setRadioKm(e.target.value)}
            >
              {[10, 30, 50, 100, 300].map((km) => (
                <option key={km} value={km}>
                  Hasta {km} km
                </option>
              ))}
            </SelectIcon>
          </Field>
        </div>
      )}

      <div className="searchbar__actions">
        <Button
          variant="primary"
          size="lg"
          leftIcon={<MagnifyingGlassIcon className="h-5 w-5" />}
          type="submit"
          disabled={!!Object.keys(errors).length || esperandoUbicacion}
        >
          Buscar Hoteles
        </Button>
      </div>
    </form>
  );
}
