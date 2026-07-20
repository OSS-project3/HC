import type { SVGProps } from "react";

type IconProps = SVGProps<SVGSVGElement>;

const base = (props: IconProps) => ({
  width: 24,
  height: 24,
  viewBox: "0 0 24 24",
  fill: "none",
  stroke: "currentColor",
  strokeWidth: 1.6,
  strokeLinecap: "round" as const,
  strokeLinejoin: "round" as const,
  ...props,
});

export const PhoneIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L20 13l1 4v3a1 1 0 0 1-1 1A16 16 0 0 1 4 5a1 1 0 0 1 1-1Z" />
  </svg>
);

export const MailIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <rect x="3" y="5" width="18" height="14" rx="2" />
    <path d="m4 7 8 6 8-6" />
  </svg>
);

export const DocIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M6 3h8l4 4v14H6Z" />
    <path d="M14 3v4h4M9 12h6M9 16h6" />
  </svg>
);

export const ChatIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M4 5h16v10H9l-4 4v-4H4Z" />
    <path d="M8 10h.01M12 10h.01M16 10h.01" />
  </svg>
);

export const CheckIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="m5 12 4.5 4.5L19 7" />
  </svg>
);

export const CheckCircleIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <circle cx="12" cy="12" r="9" />
    <path d="m8.5 12 2.5 2.5 4.5-5" />
  </svg>
);

export const GlobeIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <circle cx="12" cy="12" r="9" />
    <path d="M3 12h18M12 3a15 15 0 0 1 0 18M12 3a15 15 0 0 0 0 18" />
  </svg>
);

export const PersonIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <circle cx="12" cy="8" r="4" />
    <path d="M4 20a8 8 0 0 1 16 0" />
  </svg>
);

export const GroupIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <circle cx="9" cy="8" r="3.2" />
    <circle cx="17" cy="9" r="2.6" />
    <path d="M3 19a6 6 0 0 1 12 0M15 19a5 5 0 0 1 6-1.5" />
  </svg>
);

export const ImageIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <rect x="3" y="4" width="18" height="16" rx="2" />
    <circle cx="9" cy="9.5" r="1.8" />
    <path d="m4 18 5-5 4 4 3-3 4 4" />
  </svg>
);

export const ChevronRight = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="m9 6 6 6-6 6" />
  </svg>
);

export const ChevronLeft = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="m15 6-6 6 6 6" />
  </svg>
);

export const ArrowUpRight = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M7 17 17 7M8 7h9v9" />
  </svg>
);

export const InstagramIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <rect x="3" y="3" width="18" height="18" rx="5" />
    <circle cx="12" cy="12" r="4" />
    <circle cx="17" cy="7" r="1" fill="currentColor" stroke="none" />
  </svg>
);

/** Instagram logo in its real gradient colours (shown on hover). */
export const InstagramColorIcon = (p: IconProps) => (
  <svg width={24} height={24} viewBox="0 0 24 24" {...p}>
    <defs>
      <radialGradient id="ig-grad" cx="30%" cy="107%" r="150%">
        <stop offset="0%" stopColor="#fdf497" />
        <stop offset="5%" stopColor="#fdf497" />
        <stop offset="45%" stopColor="#fd5949" />
        <stop offset="60%" stopColor="#d6249f" />
        <stop offset="90%" stopColor="#285AEB" />
      </radialGradient>
    </defs>
    <rect x="2" y="2" width="20" height="20" rx="5.5" fill="url(#ig-grad)" />
    <rect x="6" y="6" width="12" height="12" rx="4" fill="none" stroke="#fff" strokeWidth="1.7" />
    <circle cx="12" cy="12" r="3.2" fill="none" stroke="#fff" strokeWidth="1.7" />
    <circle cx="16.4" cy="7.6" r="1.1" fill="#fff" />
  </svg>
);

export const FacebookIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <path d="M14 8h2V5h-2a3 3 0 0 0-3 3v2H9v3h2v6h3v-6h2l1-3h-3V8a1 1 0 0 1 1-1Z" />
  </svg>
);

export const YoutubeIcon = (p: IconProps) => (
  <svg {...base(p)}>
    <rect x="3" y="6" width="18" height="12" rx="3" />
    <path d="m10 9 5 3-5 3Z" fill="currentColor" />
  </svg>
);
