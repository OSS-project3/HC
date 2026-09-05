// Custom searchable select (combobox): a trigger that opens a panel with a search
// box and a filtered, scrollable option list. Reuses the .select-field* styling
// so it matches the plain SelectField, plus .select-field__search / __empty.
import { useEffect, useMemo, useRef, useState } from "react";
import { useLanguage } from "../../features/i18n/LanguageContext";

export interface SearchableOption {
  value: string;
  label: string;
  /** Extra text matched by the search box (e.g. an English name). */
  keywords?: string;
}

interface SearchableSelectFieldProps {
  value: string;
  onChange: (value: string) => void;
  options: SearchableOption[];
  placeholder?: string;
  searchPlaceholder?: string;
  ariaLabel?: string;
  className?: string;
  /** Trigger box styling. Defaults to the shared form field look (.field__select). */
  triggerClassName?: string;
  /**
   * Fires on every keystroke in the search box (including "" when the panel opens/resets).
   * Lets a parent that fetches `options` from a server (e.g. debounced search) know what
   * the user is typing. Client-side filtering below still runs on whatever `options` it
   * gets, so this is additive — no behavior change for callers that don't pass it.
   */
  onQueryChange?: (query: string) => void;
}

export function SearchableSelectField({
  value,
  onChange,
  options,
  placeholder,
  searchPlaceholder = "검색어를 입력해 주세요",
  ariaLabel,
  className,
  triggerClassName = "field__select",
  onQueryChange,
}: SearchableSelectFieldProps) {
  // Option labels and placeholders arrive as Korean source strings; translate at
  // render (t() is a no-op for strings that are already translated or unknown).
  const { t, language } = useLanguage();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [activeIndex, setActiveIndex] = useState(0);
  const ref = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);
  const selected = options.find((option) => option.value === value);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return options;
    return options.filter(
      (option) =>
        option.label.toLowerCase().includes(q) ||
        t(option.label).toLowerCase().includes(q) ||
        (option.keywords ?? "").toLowerCase().includes(q),
    );
  }, [options, query, t]);

  // Close on outside click / Escape.
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

  // When opening, focus the search box and reset the query/highlight.
  useEffect(() => {
    if (open) {
      setQuery("");
      setActiveIndex(0);
      requestAnimationFrame(() => inputRef.current?.focus());
    }
  }, [open]);

  // Keep the highlighted option in view as it moves.
  useEffect(() => {
    if (!open) return;
    listRef.current?.querySelector<HTMLElement>(".is-active")?.scrollIntoView({ block: "nearest" });
  }, [activeIndex, open]);

  const commit = (next: string) => {
    onChange(next);
    setOpen(false);
  };

  const onSearchKeyDown = (event: React.KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex((i) => Math.min(i + 1, filtered.length - 1));
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex((i) => Math.max(i - 1, 0));
    } else if (event.key === "Enter") {
      event.preventDefault();
      const option = filtered[activeIndex];
      if (option) commit(option.value);
    }
  };

  return (
    <div className={`select-field${open ? " is-open" : ""}${className ? ` ${className}` : ""}`} ref={ref}>
      <button
        type="button"
        className={`${triggerClassName} select-field__trigger${selected ? "" : " select-field__trigger--placeholder"}`}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label={ariaLabel ? t(ariaLabel) : undefined}
        onClick={() => setOpen((prev) => !prev)}
      >
        <span>{selected ? t(selected.label) : placeholder ? t(placeholder) : undefined}</span>
        <b className="select-field__caret" aria-hidden="true">⌄</b>
      </button>
      {open && (
        <div className="select-field__panel">
          <input
            ref={inputRef}
            type="text"
            className="select-field__search"
            placeholder={t(searchPlaceholder)}
            value={query}
            aria-label={
              ariaLabel
                ? language === "en"
                  ? `Search: ${t(ariaLabel)}`
                  : `${ariaLabel} 검색`
                : t("검색")
            }
            onChange={(e) => {
              setQuery(e.target.value);
              setActiveIndex(0);
              onQueryChange?.(e.target.value);
            }}
            onKeyDown={onSearchKeyDown}
          />
          {filtered.length === 0 ? (
            <p className="select-field__empty">{t("검색 결과가 없습니다.")}</p>
          ) : (
            <ul className="select-field__options select-field__options--scroll" role="listbox" aria-label={ariaLabel ? t(ariaLabel) : undefined} ref={listRef}>
              {filtered.map((option, index) => (
                <li key={option.value}>
                  <button
                    type="button"
                    role="option"
                    aria-selected={option.value === value}
                    className={`select-field__option${option.value === value ? " is-selected" : ""}${index === activeIndex ? " is-active" : ""}`}
                    onMouseEnter={() => setActiveIndex(index)}
                    onClick={() => commit(option.value)}
                  >
                    <span>{t(option.label)}</span>
                    {option.value === value && <b aria-hidden="true">✓</b>}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
