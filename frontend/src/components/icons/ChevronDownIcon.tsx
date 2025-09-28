import React from "react";

export default function ChevronDownIcon({ className = "rarn-select__chev" }: { className?: string }) {
  return (
    <svg aria-hidden viewBox="0 0 20 20" className={className}>
      <path d="M6 8l4 4 4-4" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

