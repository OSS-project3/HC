// Reusable button component with style variants.
import { forwardRef } from "react";
import type { ButtonHTMLAttributes } from "react";
import { Link } from "react-router-dom";
import clsx from "clsx";
import "./Button.css";

type Variant = "primary" | "outline" | "ghost" | "soft";

interface CommonProps {
  variant?: Variant;
  block?: boolean;
}

type ButtonAsButton = CommonProps &
  ButtonHTMLAttributes<HTMLButtonElement> & { to?: undefined; href?: undefined };

type ButtonAsLink = CommonProps & {
  to: string;
  children?: React.ReactNode;
  className?: string;
};

type ButtonAsAnchor = CommonProps & {
  href: string;
  children?: React.ReactNode;
  className?: string;
  target?: string;
  rel?: string;
};

export type ButtonProps = ButtonAsButton | ButtonAsLink | ButtonAsAnchor;

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(props, ref) {
  const { variant = "primary", block, className, children, ...rest } = props as ButtonProps & {
    className?: string;
    children?: React.ReactNode;
  };
  const cls = clsx("btn", `btn--${variant}`, block && "btn--block", className);

  if ("to" in props && props.to !== undefined) {
    const { to } = props;
    return (
      <Link to={to} className={cls}>
        {children}
      </Link>
    );
  }
  if ("href" in props && props.href !== undefined) {
    const { href, target, rel } = props as ButtonAsAnchor;
    return (
      <a className={cls} href={href} target={target} rel={rel}>
        {children}
      </a>
    );
  }

  return (
    <button ref={ref} className={cls} {...(rest as ButtonHTMLAttributes<HTMLButtonElement>)}>
      {children}
    </button>
  );
});
