import React from "react";

const ICON_SCALE = 0.5;
const ICON_SCALE_CIRCLE = 0.8;
const scaleG = `translate(256 256) scale(${ICON_SCALE}) translate(-256 -256)`;
const scaleC = `translate(256 256) scale(${ICON_SCALE_CIRCLE}) translate(-256 -256)`;

export default function ShieldIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
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
}

