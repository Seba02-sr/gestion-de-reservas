import React from "react";
import Section from "../ui/Section";
import ShieldIcon from "../icons/ShieldIcon";
import TagIcon from "../icons/TagIcon";
import HeadsetIcon from "../icons/HeadsetIcon";

type Benefit = {
  id: string;
  title: string;
  description: string;
  Icon: React.FC<React.SVGProps<SVGSVGElement>>;
};

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
    <Section className="rarn-benefits" containerClassName="rarn-benefits__container" aria-labelledby="benefits-heading">
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
    </Section>
  );
}
