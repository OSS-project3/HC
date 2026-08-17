// Custom select: replaces the native <select> so the opened dropdown options
// render in the site's own design instead of the browser/OS default.
import { useEffect, useRef, useState } from "react";

export interface SelectOption {
  value: string;
  label: string;
}

interface SelectFieldProps {
  value: string;
  onChange: (value: string) => void;
  options: SelectOption[];
  placeholder?: string;
  ariaLabel?: string;
  className?: string;
  /** Trigger box styling. Defaults to the shared form field look (.field__select). */
  triggerClassName?: string;
}

export function SelectField({ value, onChange, options, placeholder, ariaLabel, className, triggerClassName = "field__select" }: SelectFieldProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);
  const selected = options.find((option) => option.value === value);

  useEffect(() => {
    if (!open) return;
    const onDocPointer = (event: MouseEvent) => {
      if (ref.current && !ref.current.contains(event.target as Node)) setOpen(false);
    };
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onDocPointer);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDocPointer);
      document.removeEventListener("keydown", onKey);
    };
  }, [open]);

  return (
    <div className={`select-field${open ? " is-open" : ""}${className ? ` ${className}` : ""}`} ref={ref}>
      <button
        type="button"
        className={`${triggerClassName} select-field__trigger${selected ? "" : " select-field__trigger--placeholder"}`}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label={ariaLabel}
        onClick={() => setOpen((prev) => !prev)}
      >
        <span>{selected ? selected.label : placeholder}</span>
        <b className="select-field__caret" aria-hidden="true">⌄</b>
      </button>
      {open && (
        <ul className="select-field__options" role="listbox" aria-label={ariaLabel}>
          {options.map((option) => (
            <li key={option.value}>
              <button
                type="button"
                role="option"
                aria-selected={option.value === value}
                className={`select-field__option${option.value === value ? " is-selected" : ""}`}
                onClick={() => {
                  onChange(option.value);
                  setOpen(false);
                }}
              >
                <span>{option.label}</span>
                {option.value === value && <b aria-hidden="true">✓</b>}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
