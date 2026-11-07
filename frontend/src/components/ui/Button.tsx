import React from "react";

type Props = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "ghost";
  size?: "sm" | "md" | "lg";
  leftIcon?: React.ReactNode;
};

export default function Button({ variant = "primary", size = "md", leftIcon, className = "", children, ...rest }: Props) {
  const variantClass = variant === "primary" ? "rarn-btn--primary" : "rarn-btn--ghost";
  const sizeClass = size === "lg" ? "rarn-btn--lg" : size === "sm" ? "rarn-btn--sm" : "";
  return (
    <button className={`rarn-btn ${variantClass} ${sizeClass} ${className}`} {...rest}>
      {leftIcon} {children}
    </button>
  );
}
