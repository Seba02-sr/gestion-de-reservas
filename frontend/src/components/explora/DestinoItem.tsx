"use client";

interface DestinoItemProps {
  nombre: string;
  hoteles: number;
  precioDesde: number;
  onClick?: () => void;
}

export default function DestinoItem({ nombre, hoteles, precioDesde, onClick }: DestinoItemProps) {
  return (
    <li className="explora__item" onClick={onClick} role="button" tabIndex={0}>
      <div className="explora__item-info">
        <h4 className="explora__item-name rarn-typography-h6">{nombre}</h4>
        <p className="explora__item-meta">{hoteles} hoteles disponibles</p>
      </div>
      <div className="explora__item-price">
        <p className="explora__price rarn-typography-h6">Desde ${precioDesde.toLocaleString("es-AR")}</p>
        <span className="explora__pernight">por noche</span>
      </div>
    </li>
  );
}
