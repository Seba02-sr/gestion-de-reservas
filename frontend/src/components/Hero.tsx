import { MapPinIcon, CalendarIcon, UserGroupIcon, MagnifyingGlassIcon } from "@heroicons/react/24/outline";
import { useEffect, useState } from "react";

// Lista de imágenes (ajusta las rutas según tu estructura)
import hotel1 from "../assets/images/hotel1.jpg";
import hotel2 from "../assets/images/hotel2.jpg";
import hotel3 from "../assets/images/hotel3.jpg";

const images = [hotel1, hotel2, hotel3];
export default function Hero() {
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  // Cambiar imagen cada 5 segundos
  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentImageIndex((prev) => (prev + 1) % images.length);
    }, 5000); // 5 segundos

    return () => clearInterval(interval); // Limpieza al desmontar
  }, []);

  return (
    <section className="hero relative w-full py-16 px-4 overflow-hidden">
      {/* Carrusel de fondo */}
      <div className="hero__carousel absolute inset-0 z-0">
        {images.map((img, index) => (
          <div
            key={index}
            className={`hero__slide absolute inset-0 transition-opacity duration-1000 ${
              index === currentImageIndex ? "opacity-100" : "opacity-0"
            }`}
          >
            <img
              src={img}
              alt={`Fondo ${index + 1}`}
              className="w-full h-full object-cover"
            />
          </div>
        ))}
      </div>

      {/* Overlay para suavizar contraste */}
      <div className="absolute inset-0 bg-gradient-to-br from-white/10 to-slate-900/20 z-10"></div>

      {/* Contenido */}
      <div className="hero__container relative z-20 max-w-6xl mx-auto text-center">
        <h1 className="hero__title rarn-font-weight-extrabold">Encuentra tu hotel perfecto</h1>
        <p className="hero__subtitle rarn-font-weight-bold">Miles de hoteles disponibles al mejor precio</p>

        <div className="hero__form">
          <div className="hero__grid">
            {/* Destino */}
            <div className="hero__field">
              <label className="hero__label">Destino</label>
              <div className="hero__input-container">
                <MapPinIcon className="hero__icon" />
                <input
                  type="text"
                  placeholder="¿A dónde viajas?"
                  className="hero__input"
                />
              </div>
            </div>

            {/* Check-in */}
            <div className="hero__field">
              <label className="hero__label">Check-in</label>
              <div className="hero__input-container">
                <CalendarIcon className="hero__icon" />
                <input type="date" className="hero__input" />
              </div>
            </div>

            {/* Check-out */}
            <div className="hero__field">
              <label className="hero__label">Check-out</label>
              <div className="hero__input-container">
                <CalendarIcon className="hero__icon" />
                <input type="date" className="hero__input" />
              </div>
            </div>

            {/* Huéspedes */}
            <div className="hero__field">
              <label className="hero__label">Huéspedes</label>
              <div className="hero__input-container">
                <UserGroupIcon className="hero__icon" />
                <select className="hero__select">
                  <option>1 huésped</option>
                  <option>2 huéspedes</option>
                  <option>3 huéspedes</option>
                  <option>4 huéspedes</option>
                  <option>5+ huéspedes</option>
                </select>
              </div>
            </div>
          </div>

          <button className="hero__button">
            <MagnifyingGlassIcon className="h-5 w-5" />
            Buscar Hoteles
          </button>
        </div>
      </div>
    </section>
  );
}