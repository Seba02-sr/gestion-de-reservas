import type { ReactNode } from "react";

interface SectionProps {
  id?: string;
  className?: string;
  containerClassName?: string;
  children: ReactNode;
}

export default function Section({ id, className = "", containerClassName = "", children }: SectionProps) {
  return (
    <section id={id} className={`rarn-section ${className}`}>
      <div className={`rarn-section__container ${containerClassName}`}>{children}</div>
    </section>
  );
}
