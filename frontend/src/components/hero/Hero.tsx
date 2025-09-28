import { useEffect, useState } from "react";
import SearchBar from "./SearchBar";
import hotel1 from "../../assets/images/hotel1.webp";
import hotel2 from "../../assets/images/hotel2.webp";
import hotel3 from "../../assets/images/hotel3.webp";

const images = [hotel1, hotel2, hotel3];

export default function Hero() {
  const [i, setI] = useState(0);

  useEffect(() => {
    const id = setInterval(() => setI((p) => (p + 1) % images.length), 5000);
    return () => clearInterval(id);
  }, []);

  return (
    <section className="hero">
      <div className="hero__slides">
        {images.map((src, idx) => (
          <div key={idx} className={`hero__slide ${idx === i ? "is-active" : ""}`} aria-hidden={idx !== i}>
            <img src={src} alt="" className="hero__image" loading={idx === 0 ? "eager" : "lazy"} decoding="async" />
          </div>
        ))}
      </div>

      <div className="hero__overlay" />

      <div className="hero__container">
        <h1 className="hero__title rarn-typography-h1 rarn-font-weight-extrabold rarn-line-height-50">
          Encuentra tu hotel perfecto en Argentina
        </h1>
        <p className="hero__subtitle rarn-typography-p rarn-font-weight-medium rarn-line-height-24">
          Miles de hoteles disponibles al mejor precio
        </p>

        <div className="hero__card rarn-card">
          <SearchBar />
        </div>
      </div>
    </section>
  );
}
