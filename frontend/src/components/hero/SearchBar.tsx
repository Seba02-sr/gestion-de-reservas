import { useEffect, useMemo, useState } from "react";
import { MapPinIcon, CalendarIcon, UserGroupIcon, MagnifyingGlassIcon } from "@heroicons/react/24/outline";
import Button from "../ui/Button";
import { Field, InputIcon, SelectIcon } from "../ui/Field";
import { todayISO, isPastDate, isInvalidCheckOut } from "../../util/date";

export default function SearchBar() {
  const [destino, setDestino] = useState("");
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
    setErrors(nextErrors);
  }, [checkIn, checkOut]);

  const onSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (Object.keys(errors).length) return;
    // TODO: ejecutar búsqueda con { destino, checkIn, checkOut, huespedes }
  };

  return (
    <form className="hero__form searchbar" onSubmit={onSubmit} noValidate>
      <div className="searchbar__grid">
        <Field label="Destino" htmlFor="destino">
          <InputIcon
            id="destino"
            icon={<MapPinIcon className="rarn-input-icon__svg h-5 w-5" />}
            placeholder="¿A dónde viajas?"
            value={destino}
            onChange={(e) => setDestino(e.target.value)}
          />
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
            <option value="5+">5+ huéspedes</option>
          </SelectIcon>
        </Field>
      </div>

      <div className="searchbar__actions">
        <Button
          variant="primary"
          size="lg"
          leftIcon={<MagnifyingGlassIcon className="h-5 w-5" />}
          type="submit"
          disabled={!!Object.keys(errors).length}
        >
          Buscar Hoteles
        </Button>
      </div>
    </form>
  );
}
