// ⬅️ Sin react-router-dom. Solo <a>
import arnLogo from "@/assets/svg/ARN-logo.svg";

type FooterLink = { label: string; to: string; external?: boolean };

const DESTINOS: FooterLink[] = [
  { label: "Buenos Aires", to: "/destinos/buenos-aires" },
  { label: "Bariloche", to: "/destinos/bariloche" },
  { label: "Mendoza", to: "/destinos/mendoza" },
  { label: "Córdoba", to: "/destinos/cordoba" },
];

const SOPORTE: FooterLink[] = [
  { label: "Centro de Ayuda", to: "/ayuda" },
  { label: "Contacto", to: "/contacto" },
  { label: "Términos y Condiciones", to: "/terminos" },
  { label: "Política de Privacidad", to: "/privacidad" },
];

const EMPRESA: FooterLink[] = [
  { label: "Sobre nosotros", to: "/sobre-nosotros" },
  { label: "Contacto", to: "/contacto" },
];

function Column({ title, items }: { title: string; items: FooterLink[] }) {
  return (
    <nav aria-label={title} className="rarn-footer__col">
      <h3 className="rarn-footer__colTitle">{title}</h3>
      <ul className="rarn-footer__list">
        {items.map((item) => (
          <li key={item.label}>
            <a
              href={item.to}
              {...(item.external ? { target: "_blank", rel: "noopener noreferrer" } : {})}
              className="rarn-footer__link"
            >
              {item.label}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}

export default function Footer() {
  const year = new Date().getFullYear();

  return (
    <footer className="rarn-footer">
      <div className="rarn-footer__inner container">
        {/* Brand + tagline */}
        <div className="rarn-footer__brand">
          <a href="/" className="rarn-footer__logoLink" aria-label="Volver al inicio">
            <img
              src={arnLogo}
              alt="HotelReservas"
              className="rarn-footer__logo"
              width={120}
              height={32}
              loading="lazy"
            />
          </a>
          <p className="rarn-footer__tagline">
            La plataforma líder en reservas de hoteles en Argentina.
          </p>
        </div>

        {/* Columns */}
        <div className="rarn-footer__grid">
          <Column title="Destinos" items={DESTINOS} />
          <Column title="Soporte" items={SOPORTE} />
          <Column title="Empresa" items={EMPRESA} />
        </div>
      </div>

      <hr className="rarn-footer__divider" />

      <div className="rarn-footer__bottom container">
        <p className="rarn-footer__copy">
          © {year} Reservas ARN. Todos los derechos reservados.
        </p>
      </div>
    </footer>
  );
}
