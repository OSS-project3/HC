import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import "./toast.css";

type Listener = (message: string) => void;
const listeners = new Set<Listener>();

/** Fire a toast from anywhere (e.g. after copying the account number). */
export function showToast(message: string) {
  listeners.forEach((l) => l(message));
}

interface ToastItem {
  id: number;
  message: string;
}

let counter = 0;

/** Mount once near the app root. */
export function Toaster() {
  const [items, setItems] = useState<ToastItem[]>([]);

  useEffect(() => {
    const listener: Listener = (message) => {
      const id = ++counter;
      setItems((prev) => [...prev, { id, message }]);
      window.setTimeout(() => {
        setItems((prev) => prev.filter((t) => t.id !== id));
      }, 2400);
    };
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  }, []);

  if (items.length === 0) return null;

  return createPortal(
    <div className="toaster" role="status" aria-live="polite">
      {items.map((t) => (
        <div key={t.id} className="toast">
          {t.message}
        </div>
      ))}
    </div>,
    document.body,
  );
}
