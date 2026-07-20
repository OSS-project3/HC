import { socialLinks } from "../../data/social";
import { InstagramIcon, FacebookIcon, YoutubeIcon } from "../ui/icons";

const outlineIcons = {
  instagram: InstagramIcon,
  facebook: FacebookIcon,
  youtube: YoutubeIcon,
} as const;

/**
 * Only enabled social links render (Instagram for now; Facebook/YouTube stay in
 * the data, disabled). A monochrome outline glyph is used as the UI mark — the
 * real Instagram colour logo asset should be dropped in on hover once provided.
 * A translucent dark layer appears on press (:active) for tap feedback.
 */
export function SocialLinks() {
  return (
    <ul className="footer__social">
      {socialLinks
        .filter((s) => s.enabled)
        .map((s) => {
          const Outline = outlineIcons[s.type];
          return (
            <li key={s.type}>
              <a
                className={`social social--${s.type}`}
                href={s.href}
                target="_blank"
                rel="noreferrer noopener"
                aria-label={s.label}
              >
                <Outline width={22} height={22} />
              </a>
            </li>
          );
        })}
    </ul>
  );
}
