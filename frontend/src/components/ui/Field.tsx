import React from "react";
import ChevronDownIcon from "../icons/ChevronDownIcon";

export function Field({
  label,
  htmlFor,
  children,
  className = "",
}: {
  label: string;
  htmlFor?: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div className={`rarn-field ${className}`}>
      <label className="rarn-label" {...(htmlFor ? { htmlFor } : {})}>
        {label}
      </label>
      {children}
    </div>
  );
}

export function InputIcon({
  icon,
  id,
  className = "",
  ...rest
}: { icon: React.ReactNode } & React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <div className={`rarn-input-icon ${className}`}>
      <span className="rarn-input-icon__icon" aria-hidden>
        {icon}
      </span>
      <input id={id} className="rarn-input rarn-input-icon__control" {...rest} />
    </div>
  );
}

export function SelectIcon({
  icon,
  id,
  className = "",
  children,
  useCustomChevron = true,
  ...rest
}: {
  icon: React.ReactNode;
  children: React.ReactNode;
  useCustomChevron?: boolean;
} & React.SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <div className={`rarn-input-icon ${className}`}>
      <span className="rarn-input-icon__icon" aria-hidden>
        {icon}
      </span>
      <select id={id} className="rarn-select rarn-input-icon__control" {...rest}>
        {children}
      </select>
      {useCustomChevron && <ChevronDownIcon />}
    </div>
  );
}
