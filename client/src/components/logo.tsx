import * as React from "react"
import { cn } from "cn"

interface LogoProps extends React.SVGProps<SVGSVGElement> {
  size?: number
  className?: string
}

export function Logo({ size = 36, className, ...props }: LogoProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 128 128"
      fill="none"
      width={size}
      height={size}
      className={cn(
        "shrink-0 transition-transform duration-300 hover:scale-105",
        className
      )}
      {...props}
    >
      <defs>
        <linearGradient
          id="logo-layer1-top"
          x1="24"
          y1="16"
          x2="104"
          y2="56"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0%" stopColor="#38bdf8" />
          <stop offset="100%" stopColor="#818cf8" />
        </linearGradient>
        <linearGradient
          id="logo-layer1-side"
          x1="24"
          y1="40"
          x2="104"
          y2="56"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0%" stopColor="#0284c7" />
          <stop offset="100%" stopColor="#4f46e5" />
        </linearGradient>

        <linearGradient
          id="logo-layer2-top"
          x1="24"
          y1="40"
          x2="104"
          y2="80"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0%" stopColor="#818cf8" />
          <stop offset="100%" stopColor="#c084fc" />
        </linearGradient>
        <linearGradient
          id="logo-layer2-side"
          x1="24"
          y1="64"
          x2="104"
          y2="80"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0%" stopColor="#4f46e5" />
          <stop offset="100%" stopColor="#9333ea" />
        </linearGradient>

        <linearGradient
          id="logo-layer3-top"
          x1="24"
          y1="64"
          x2="104"
          y2="104"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0%" stopColor="#c084fc" />
          <stop offset="100%" stopColor="#2dd4bf" />
        </linearGradient>
        <linearGradient
          id="logo-layer3-side"
          x1="24"
          y1="88"
          x2="104"
          y2="104"
          gradientUnits="userSpaceOnUse"
        >
          <stop offset="0%" stopColor="#7e22ce" />
          <stop offset="100%" stopColor="#0f766e" />
        </linearGradient>
      </defs>

      {/* Layer 3 - Base */}
      <g>
        <path
          d="M24 76 L64 96 L64 104 L24 84 Z"
          fill="url(#logo-layer3-side)"
          fillOpacity="0.8"
        />
        <path
          d="M64 96 L104 76 L104 84 L64 104 Z"
          fill="url(#logo-layer3-side)"
          fillOpacity="0.95"
        />
        <path
          d="M64 56 L104 76 L64 96 L24 76 Z"
          fill="url(#logo-layer3-top)"
          fillOpacity="0.85"
          stroke="#5eead4"
          strokeWidth="1.5"
          strokeLinejoin="round"
        />
      </g>

      {/* Layer 2 - Middle */}
      <g>
        <path
          d="M24 52 L64 72 L64 80 L24 60 Z"
          fill="url(#logo-layer2-side)"
          fillOpacity="0.8"
        />
        <path
          d="M64 72 L104 52 L104 60 L64 80 Z"
          fill="url(#logo-layer2-side)"
          fillOpacity="0.95"
        />
        <path
          d="M64 32 L104 52 L64 72 L24 52 Z"
          fill="url(#logo-layer2-top)"
          fillOpacity="0.85"
          stroke="#e879f9"
          strokeWidth="1.5"
          strokeLinejoin="round"
        />
      </g>

      {/* Layer 1 - Top */}
      <g>
        <path
          d="M24 28 L64 48 L64 56 L24 36 Z"
          fill="url(#logo-layer1-side)"
          fillOpacity="0.85"
        />
        <path
          d="M64 48 L104 28 L104 36 L64 56 Z"
          fill="url(#logo-layer1-side)"
          fillOpacity="1"
        />
        <path
          d="M64 8 L104 28 L64 48 L24 28 Z"
          fill="url(#logo-layer1-top)"
          fillOpacity="0.9"
          stroke="#7dd3fc"
          strokeWidth="2"
          strokeLinejoin="round"
        />
        <path
          d="M64 16 L90 29 L64 42 L38 29 Z"
          fill="#ffffff"
          fillOpacity="0.3"
          stroke="#ffffff"
          strokeWidth="1"
          strokeLinejoin="round"
        />
      </g>
    </svg>
  )
}
