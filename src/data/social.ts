/**
 * Social links. Only Instagram is enabled for now; Facebook and YouTube stay
 * in the data (disabled) so they can be switched on later without code edits.
 */
export type SocialType = "instagram" | "facebook" | "youtube";

export interface SocialLink {
  type: SocialType;
  label: string;
  href: string;
  enabled: boolean;
}

export const socialLinks: SocialLink[] = [
  {
    type: "instagram",
    label: "Instagram",
    href: "https://www.instagram.com/",
    enabled: true,
  },
  { type: "facebook", label: "Facebook", href: "#", enabled: false },
  { type: "youtube", label: "YouTube", href: "#", enabled: false },
];
