// ZodiacDesignSelector/CardDesignSelector 공용 — 트리거 버튼 열기/닫기, 바깥 클릭·Escape 닫기,
// 그리고 패널을 document.body에 포탈로 띄우기 위한 위치 계산을 맡는다.
//
// 패널을 포탈로 렌더링하는 이유: 이 셀렉터들은 관리자 신청 상세(펼친 테이블 행) 안에 있고, 그
// 테이블은 가로 스크롤을 위해 `.admin__table-wrap { overflow-x: auto; }`로 감싸여 있다. CSS
// Overflow 스펙상 overflow-x/overflow-y 중 하나가 visible이고 다른 하나가 아니면 두 축 다
// visible일 수 없어 visible 쪽이 auto로 강제 변경된다 — 그래서 `overflow-y: visible`을 명시해도
// 소용없고, 일반 position:absolute 패널은 이 래퍼의 콘텐츠 기준 높이를 넘는 순간 그냥 잘려서
// 안 보인다(2026-09-23, Playwright로 실제 확인). 포탈로 document.body에 렌더링하면 이 클리핑을
// 구조적으로 피할 수 있다.
import { useEffect, useRef, useState, type KeyboardEvent as ReactKeyboardEvent } from "react";

export interface PopoverPosition {
  position: "fixed";
  top: number;
  left: number;
}

export function usePreviewPopover() {
  const [open, setOpen] = useState(false);
  const [style, setStyle] = useState<PopoverPosition | null>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  const reposition = () => {
    const rect = triggerRef.current?.getBoundingClientRect();
    if (!rect) return;
    setStyle({ position: "fixed", top: rect.bottom + 6, left: rect.left });
  };

  useEffect(() => {
    if (!open) { setStyle(null); return; }
    reposition();
    // 열려 있는 동안 스크롤·리사이즈가 일어나면 트리거와 어긋나므로 닫는다(포탈이라 트리거를
    // 따라 자동으로 움직이지 않는다) — 대부분의 드롭다운 라이브러리가 쓰는 것과 같은 처리다.
    const close = () => setOpen(false);
    window.addEventListener("scroll", close, { capture: true, passive: true });
    window.addEventListener("resize", close);
    const onDocPointer = (event: MouseEvent) => {
      const target = event.target as Node;
      if (triggerRef.current?.contains(target)) return;
      if (panelRef.current?.contains(target)) return;
      setOpen(false);
    };
    const onKey = (event: KeyboardEvent) => { if (event.key === "Escape") setOpen(false); };
    document.addEventListener("mousedown", onDocPointer);
    document.addEventListener("keydown", onKey);
    return () => {
      window.removeEventListener("scroll", close, { capture: true });
      window.removeEventListener("resize", close);
      document.removeEventListener("mousedown", onDocPointer);
      document.removeEventListener("keydown", onKey);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // 트리거를 클릭/Enter/Space로 열어도 DOM 포커스는 트리거 버튼에 남아있다 — 방향키가 목록에
  // 닿으려면 패널이 실제로 마운트된 뒤 옵션 하나로 포커스를 옮겨줘야 한다. 이미 선택된 항목이
  // 있으면 거기로, 없으면 첫 항목으로 이동한다.
  useEffect(() => {
    if (!open || !style) return;
    const id = requestAnimationFrame(() => {
      const panel = panelRef.current;
      if (!panel) return;
      const selected = panel.querySelector<HTMLElement>('[aria-selected="true"]');
      const first = panel.querySelector<HTMLElement>('[role="option"]');
      (selected ?? first)?.focus();
    });
    return () => cancelAnimationFrame(id);
  }, [open, style]);

  // 방향키로 옵션 사이를 이동한다(role=listbox 표준 패턴) — 이동 자체는 포커스만 옮기고
  // onFocus 핸들러가 알아서 그 항목의 미리보기를 띄운다. Enter/Space 선택은 각 옵션이
  // <button>이라 브라우저 기본 동작으로 이미 처리된다.
  const onListKeyDown = (event: ReactKeyboardEvent) => {
    if (!["ArrowDown", "ArrowUp", "Home", "End"].includes(event.key)) return;
    event.preventDefault();
    const options = Array.from(panelRef.current?.querySelectorAll<HTMLElement>('[role="option"]') ?? []);
    if (options.length === 0) return;
    const current = options.indexOf(document.activeElement as HTMLElement);
    let next: number;
    if (event.key === "Home") next = 0;
    else if (event.key === "End") next = options.length - 1;
    else if (event.key === "ArrowDown") next = current < 0 ? 0 : (current + 1) % options.length;
    else next = current < 0 ? options.length - 1 : (current - 1 + options.length) % options.length;
    options[next]?.focus();
  };

  return { open, setOpen, triggerRef, panelRef, style, onListKeyDown };
}
