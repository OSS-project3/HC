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

// Display order: Facebook → Instagram → YouTube.
export const socialLinks: SocialLink[] = [
  { type: "facebook", label: "Facebook", href: "https://www.facebook.com/", enabled: true },
  { type: "instagram", label: "Instagram", href: "https://www.instagram.com/", enabled: true },
  { type: "youtube", label: "YouTube", href: "https://www.youtube.com/", enabled: true },
];
