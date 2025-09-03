import React from "react";

type Benefit = {
  id: string;
  title: string;
  description: string;
  Icon: React.FC<React.SVGProps<SVGSVGElement>>;
};

// Escala centrada: ajusta 0.78 si querés más/menos “aire”
const ICON_SCALE = 0.5;
const scaleG = `translate(256 256) scale(${ICON_SCALE}) translate(-256 -256)`;

const ICON_SCALE_CIRCLE = 0.8;
const scaleC = `translate(256 256) scale(${ICON_SCALE_CIRCLE}) translate(-256 -256)`;

/* ----------------- ICONOS ----------------- */
const ShieldIcon: React.FC<React.SVGProps<SVGSVGElement>> = (props) => (
  <svg viewBox="0 0 512 512" aria-hidden="true" {...props}>
    <circle transform={scaleC} cx="256" cy="256" r="256" fill="currentColor" />
    <g transform={scaleG}>
      <path
        fill="#fff"
        d="M256 0c4.6 0 9.2 1 13.4 2.9L457.7 82.8c22 9.3 38.4 31 38.3 57.2c-.5 99.2-41.3 280.7-213.6 363.2c-16.7 8-36.1 8-52.8 0C57.3 420.7 16.5 239.2 16 140c-.1-26.2 16.3-47.9 38.3-57.2L242.7 2.9C246.8 1 251.4 0 256 0zm0 66.8V444.8C394 378 431.1 230.1 432 141.4L256 66.8l0 0z"
      />
    </g>
  </svg>
);

const TagIcon: React.FC<React.SVGProps<SVGSVGElement>> = (props) => (
  <svg viewBox="0 0 512 512" aria-hidden="true" {...props}>
    <circle transform={scaleC} cx="256" cy="256" r="256" fill="currentColor" />
    <g transform={scaleG}>
      <path
        fill="#fff"
        d="M345 39.1L472.8 168.4c52.4 53 52.4 138.2 0 191.2L360.8 472.9c-9.3 9.4-24.5 9.5-33.9 .2s-9.5-24.5-.2-33.9L438.6 325.9c33.9-34.3 33.9-89.4 0-123.7L310.9 72.9c-9.3-9.4-9.2-24.6 .2-33.9s24.6-9.2 33.9 .2zM0 229.5V80C0 53.5 21.5 32 48 32H197.5c17 0 33.3 6.7 45.3 18.7l168 168c25 25 25 65.5 0 90.5L277.3 442.7c-25 25-65.5 25-90.5 0l-168-168C6.7 262.7 0 246.5 0 229.5zM144 144a32 32 0 1 0 -64 0 32 32 0 1 0 64 0z"
      />
    </g>
  </svg>
);

const HeadsetIcon: React.FC<React.SVGProps<SVGSVGElement>> = (props) => (
  <svg viewBox="0 0 512 512" aria-hidden="true" {...props}>
    <circle transform={scaleC} cx="256" cy="256" r="256" fill="currentColor" />
    <g transform={scaleG}>
      <path
        fill="#fff"
        d="M256 48C141.1 48 48 141.1 48 256v40c0 13.3-10.7 24-24 24s-24-10.7-24-24V256C0 114.6 114.6 0 256 0S512 114.6 512 256V400.1c0 48.6-39.4 88-88.1 88L313.6 488c-8.3 14.3-23.8 24-41.6 24H240c-26.5 0-48-21.5-48-48s21.5-48 48-48h32c17.8 0 33.3 9.7 41.6 24l110.4 .1c22.1 0 40-17.9 40-40V256c0-114.9-93.1-208-208-208zM144 208h16c17.7 0 32 14.3 32 32V352c0 17.7-14.3 32-32 32H144c-35.3 0-64-28.7-64-64V272c0-35.3 28.7-64 64-64zm224 0c35.3 0 64 28.7 64 64v48c0 35.3-28.7 64-64 64H352c-17.7 0-32-14.3-32-32V240c0-17.7 14.3-32 32-32h16z"
      />
    </g>
  </svg>
);

/* --------------- DATA + COMPONENTE --------------- */
const BENEFITS: Benefit[] = [
  {
    id: "secure",
    title: "Reserva Segura",
    description: "Tus datos y pagos están protegidos con la máxima seguridad.",
    Icon: ShieldIcon,
  },
  {
    id: "prices",
    title: "Mejores Precios",
    description: "Garantizamos los mejores precios del mercado.",
    Icon: TagIcon,
  },
  {
    id: "support",
    title: "Soporte 24/7",
    description: "Atención al cliente disponible las 24 horas.",
    Icon: HeadsetIcon,
  },
];

export default function Benefits() {
  return (
    <section className="rarn-benefits" aria-labelledby="benefits-heading">
      <div className="rarn-benefits__container">
        <h2 id="benefits-heading" className="rarn-benefits__title rarn-typography-h2">
          ¿Por qué elegir Reservas ARN?
        </h2>

        <ul className="rarn-benefits__grid" role="list">
          {BENEFITS.map(({ id, title, description, Icon }) => (
            <li key={id} className="rarn-benefits__item">
              <span className="rarn-benefits__icon" aria-hidden="true">
                <Icon className="rarn-benefits__icon-svg" />
              </span>
              <h3 className="rarn-benefits__item-title">{title}</h3>
              <p className="rarn-benefits__item-desc">{description}</p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
