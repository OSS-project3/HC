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
  maxHeight: number;
}

const VIEWPORT_MARGIN = 12;

export function usePreviewPopover() {
  const [open, setOpen] = useState(false);
  const [style, setStyle] = useState<PopoverPosition | null>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  // 패널 높이는 옵션 수·미리보기 크기에 따라 뷰포트보다 커질 수 있다(특히 모바일) — 트리거
  // 아래 남은 세로 공간으로 max-height를 제한하고 패널 자체를 내부 스크롤시킨다. 그러지 않으면
  // position:fixed 패널이 뷰포트 아래로 넘치는 부분은 페이지 스크롤로도 닿을 수 없다(fixed는
  // 페이지 스크롤을 따라가지 않으므로) — 2026-09-23 모바일 Playwright 검증 중 실제로 발견.
  const reposition = () => {
    const rect = triggerRef.current?.getBoundingClientRect();
    if (!rect) return;
    const top = rect.bottom + 6;
    setStyle({
      position: "fixed",
      top,
      left: rect.left,
      maxHeight: Math.max(120, window.innerHeight - top - VIEWPORT_MARGIN),
    });
  };

  useEffect(() => {
    if (!open) { setStyle(null); return; }
    reposition();
    // 열려 있는 동안 페이지가 스크롤·리사이즈되면 트리거와 어긋나므로 다시 계산한다(포탈이라
    // 트리거를 따라 자동으로 움직이지 않는다). 닫지 않고 재계산하는 이유: 모바일 브라우저는
    // 스크롤 중 주소창이 접히고 펴지면서 window resize를 계속 쏘고(실제 기기 회전이 아니어도),
    // 옵션 목록 자체의 내부 스크롤(overflow-y:auto, 방향키·자동 포커스의 scrollIntoView 포함)도
    // scroll 이벤트를 낸다 — 이 둘을 "닫기"로 처리하면 열자마자 다시 닫혀버린다(실제로 겪은
    // 버그: 모바일 tap으로 열고 다음 옵션을 tap하려는 사이에 팝업이 사라짐). 재계산은 이런
    // 이벤트가 실제 트리거 위치 변화든 아니든 항상 안전하다.
    window.addEventListener("scroll", reposition, { capture: true, passive: true });
    window.addEventListener("resize", reposition);
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
      window.removeEventListener("scroll", reposition, { capture: true });
      window.removeEventListener("resize", reposition);
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
