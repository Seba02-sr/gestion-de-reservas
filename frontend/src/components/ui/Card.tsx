import React from "react";

type Props = {
  title?: React.ReactNode;
  footer?: React.ReactNode;
  hoverable?: boolean;
  className?: string;
  children: React.ReactNode;
};

export default function Card({ title, footer, hoverable, className = "", children }: Props) {
  return (
    <div className={`rarn-card ${hoverable ? "rarn-card--hoverable" : ""} ${className}`}>
      {title && (
        <div className="rarn-card__header">
          <h3 className="rarn-card__title">{title}</h3>
        </div>
      )}
      <div className="rarn-card__body">{children}</div>
      {footer && <div className="rarn-card__footer">{footer}</div>}
    </div>
  );
}
