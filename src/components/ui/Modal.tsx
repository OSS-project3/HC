import { useCallback, useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import "./Modal.css";

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  /** Optional prev/next navigation arrows (used by the policy modal). */
  onPrev?: () => void;
  onNext?: () => void;
}

const FOCUSABLE =
  'a[href], button:not([disabled]), textarea, input, select, [tabindex]:not([tabindex="-1"])';

/**
 * Accessible modal dialog: white surface, scrollable body, closes on backdrop
 * click / ESC / close button, traps focus and restores it on close.
 */
export function Modal({ open, onClose, title, children, onPrev, onNext }: ModalProps) {
  const dialogRef = useRef<HTMLDivElement>(null);
  const previouslyFocused = useRef<HTMLElement | null>(null);

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        onClose();
        return;
      }
      if (e.key === "Tab" && dialogRef.current) {
        const nodes = Array.from(
          dialogRef.current.querySelectorAll<HTMLElement>(FOCUSABLE),
        ).filter((el) => el.offsetParent !== null);
        if (nodes.length === 0) return;
        const first = nodes[0];
        const last = nodes[nodes.length - 1];
        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    },
    [onClose],
  );

  useEffect(() => {
    if (!open) return;
    previouslyFocused.current = document.activeElement as HTMLElement;
    document.addEventListener("keydown", handleKeyDown);
    const { overflow } = document.body.style;
    document.body.style.overflow = "hidden";

    // Focus the dialog once mounted.
    requestAnimationFrame(() => {
      const first = dialogRef.current?.querySelector<HTMLElement>(FOCUSABLE);
      (first ?? dialogRef.current)?.focus();
    });

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = overflow;
      previouslyFocused.current?.focus?.();
    };
  }, [open, handleKeyDown]);

  if (!open) return null;

  return createPortal(
    <div className="modal" onMouseDown={onClose}>
      <div
        className="modal__dialog"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        ref={dialogRef}
        tabIndex={-1}
        onMouseDown={(e) => e.stopPropagation()}
      >
        {onPrev && (
          <button className="modal__nav modal__nav--prev" onClick={onPrev} aria-label="이전 문서">
            ‹
          </button>
        )}
        {onNext && (
          <button className="modal__nav modal__nav--next" onClick={onNext} aria-label="다음 문서">
            ›
          </button>
        )}
        <header className="modal__header">
          <h2 className="modal__title">{title}</h2>
          <button className="modal__close" onClick={onClose} aria-label="닫기">
            ✕
          </button>
        </header>
        <div className="modal__body">{children}</div>
      </div>
    </div>,
    document.body,
  );
}
