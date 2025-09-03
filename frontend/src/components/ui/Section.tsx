import type { ReactNode } from "react";


interface SectionProps {
id?: string;
className?: string;
children: ReactNode;
}


export default function Section({ id, className = "", children }: SectionProps) {
return (
<section id={id} className={`py-8 md:py-12 lg:py-16 ${className}`}>
<div className="container mx-auto">{children}</div>
</section>
);
}