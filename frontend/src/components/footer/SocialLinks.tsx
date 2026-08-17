// Footer social-media icon links.
import { socialLinks } from "../../data/social";
import { InstagramIcon, InstagramColorIcon, FacebookIcon, YoutubeIcon } from "../ui/icons";

const outlineIcons = {
  instagram: InstagramIcon,
  facebook: FacebookIcon,
  youtube: YoutubeIcon,
} as const;

const colorIcons = {
  instagram: InstagramColorIcon,
  facebook: FacebookColorIcon,
  youtube: YoutubeColorIcon,
} as const;

/**
 * Enabled social links render as monochrome footer marks, then switch to each
 * service's brand-colour icon on hover/focus.
 */
export function SocialLinks() {
  return (
    <ul className="footer__social">
      {socialLinks
        .filter((s) => s.enabled)
        .map((s) => {
          const Outline = outlineIcons[s.type];
          const Color = colorIcons[s.type];
          return (
            <li key={s.type}>
              <a
                className={`social social--${s.type}`}
                href={s.href}
                target="_blank"
                rel="noreferrer noopener"
                aria-label={s.label}
              >
                <span className="social__default" aria-hidden="true">
                  <Outline width={22} height={22} />
                </span>
                <span className="social__hover" aria-hidden="true">
                  <Color />
                </span>
              </a>
            </li>
          );
        })}
    </ul>
  );
}

function FacebookColorIcon({ width = 24, height = 24 }: { width?: number; height?: number }) {
  return (
    <svg width={width} height={height} viewBox="0 0 24 24">
      <circle cx="12" cy="12" r="12" fill="#1877F2" />
      <path
        d="M14.7 12.6h2.1l.4-2.8h-2.5V8.2c0-.8.4-1.5 1.6-1.5h1V4.2c-.2 0-1-.2-2-.2-2.2 0-3.6 1.3-3.6 3.8v2H9.3v2.8h2.4V20h3v-7.4Z"
        fill="#fff"
      />
    </svg>
  );
}

function YoutubeColorIcon({ width = 24, height = 24 }: { width?: number; height?: number }) {
  return (
    <svg width={width} height={height} viewBox="0 0 24 24">
      <rect width="24" height="24" rx="12" fill="#FF0000" />
      <path d="m10 9 5.8 3L10 15V9Z" fill="#fff" />
    </svg>
  );
}
