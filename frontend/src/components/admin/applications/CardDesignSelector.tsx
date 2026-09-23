// 카드 디자인 선택 — 옵션에 hover/focus하면 그 디자인의 앞·뒷면 원본 템플릿을 조회 전용으로
// 미리 보여준다(2026-09-22 확정 정책). 기존 native <select>를 대체하되 value/onChange 계약은
// 그대로 유지해 카드 미리보기·생성 흐름(CardProductionPanel)은 손대지 않는다.
import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { api, type CardDesignOption, type CardDesignTemplatePreview } from "../../../services/api";
import { asDataUrl } from "./applicationUtils";
import { usePreviewPopover } from "./usePreviewPopover";

const CLOSE_DELAY_MS = 150;

type CacheEntry = "loading" | "error" | CardDesignTemplatePreview;

export function CardDesignSelector({
  designs, value, onChange, disabled, locked,
}: {
  designs: CardDesignOption[];
  value: string;
  onChange: (value: string) => void;
  /** 완전 비활성 — 열기 자체를 막는다(디자인 목록을 아직 안 불러왔거나 없을 때). */
  disabled?: boolean;
  /** 선택 변경만 막고 조회(hover/focus 미리보기)는 계속 허용한다(카드 생성 후 확정 잠금). */
  locked?: boolean;
}) {
  const { open, setOpen, triggerRef, panelRef, style, onListKeyDown } = usePreviewPopover();
  const [activeId, setActiveId] = useState<number | null>(null);
  const [cache, setCache] = useState<Record<number, CacheEntry>>({});
  const leaveTimer = useRef<number | null>(null);
  const selected = designs.find((d) => String(d.id) === value);

  const cancelLeave = () => {
    if (leaveTimer.current != null) { window.clearTimeout(leaveTimer.current); leaveTimer.current = null; }
  };
  const enter = (id: number) => { cancelLeave(); setActiveId(id); };
  const scheduleLeave = (id: number) => {
    cancelLeave();
    leaveTimer.current = window.setTimeout(() => {
      setActiveId((current) => (current === id ? null : current));
      leaveTimer.current = null;
    }, CLOSE_DELAY_MS);
  };

  useEffect(() => {
    if (!open) { cancelLeave(); setActiveId(null); }
  }, [open]);

  // requested는 state가 아니라 ref다 — effect의 의존성에 cache(state)를 넣으면 fetchDesign 안의
  // setCache("loading")이 그 즉시 같은 effect를 재실행시켜 방금 시작한 요청 자신을 취소해 버린다
  // (cleanup의 cancelled=true가 나중에 도착하는 성공 응답까지 막는 자기 취소 버그).
  const mounted = useRef(true);
  const requested = useRef<Set<number>>(new Set());
  useEffect(() => () => { mounted.current = false; }, []);

  // 디자인 목록을 다시 불러오면(예: 학생증 템플릿 교체) 캐시가 새 목록 기준으로 다시 구성되도록
  // designs 배열이 바뀔 때 초기화한다 — 교체된 학생증 템플릿을 영구적으로 고정하지 않기 위함.
  useEffect(() => { setCache({}); requested.current.clear(); }, [designs]);

  const fetchDesign = (id: number) => {
    requested.current.add(id);
    setCache((prev) => ({ ...prev, [id]: "loading" }));
    api.getCardDesignTemplatePreview(id)
      .then((data) => { if (mounted.current) setCache((prev) => ({ ...prev, [id]: data })); })
      .catch(() => { if (mounted.current) setCache((prev) => ({ ...prev, [id]: "error" })); });
  };

  useEffect(() => {
    if (activeId == null || requested.current.has(activeId)) return;
    fetchDesign(activeId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeId]);

  const retry = (id: number) => fetchDesign(id);

  const select = (id: number) => {
    if (locked) return;
    onChange(String(id));
    setOpen(false);
  };

  const entry = activeId != null ? cache[activeId] : undefined;

  return (
    <div className={`select-field preview-listbox${open ? " is-open" : ""}`}>
      <button
        ref={triggerRef}
        type="button"
        className={`field__select select-field__trigger preview-listbox__trigger${selected ? "" : " select-field__trigger--placeholder"}`}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label="카드 디자인"
        disabled={disabled}
        onClick={() => setOpen((prev) => !prev)}
      >
        <span>{selected ? `${selected.name} #${selected.designNumber}` : "디자인 선택"}</span>
        <b className="select-field__caret" aria-hidden="true">⌄</b>
      </button>
      {open && style && createPortal(
        <div className="preview-listbox__panel" ref={panelRef} style={style}>
          <ul className="select-field__options preview-listbox__options" role="listbox" aria-label="카드 디자인" onKeyDown={onListKeyDown}>
            {designs.map((d) => (
              <li key={d.id}>
                <button
                  type="button"
                  role="option"
                  aria-selected={String(d.id) === value}
                  className={`select-field__option${String(d.id) === value ? " is-selected" : ""}`}
                  onMouseEnter={() => enter(d.id)}
                  onMouseLeave={() => scheduleLeave(d.id)}
                  onFocus={() => enter(d.id)}
                  onBlur={() => scheduleLeave(d.id)}
                  onClick={() => select(d.id)}
                >
                  <span>{d.name} #{d.designNumber}</span>
                  {String(d.id) === value && <b aria-hidden="true">✓</b>}
                </button>
              </li>
            ))}
          </ul>
          {activeId != null && (
            <div
              className="preview-listbox__preview"
              onMouseEnter={cancelLeave}
              onMouseLeave={() => scheduleLeave(activeId)}
            >
              <p className="preview-listbox__preview-title">
                {designs.find((d) => d.id === activeId)?.name ?? ""} 미리보기
              </p>
              {entry === "loading" && (
                <div className="card-design-preview">
                  <div className="card-design-preview__img card-design-preview__img--skeleton" />
                  <div className="card-design-preview__img card-design-preview__img--skeleton" />
                </div>
              )}
              {entry === "error" && (
                <p className="admin-panel__note admin-panel__note--error">
                  이미지를 불러오지 못했습니다.{" "}
                  <button type="button" className="admin__btn" onClick={() => retry(activeId)}>다시 시도</button>
                </p>
              )}
              {entry && entry !== "loading" && entry !== "error" && (
                <div className="card-design-preview">
                  <img className="card-design-preview__img" src={asDataUrl(entry.frontImageBase64)} alt="카드 앞면 미리보기" />
                  <img className="card-design-preview__img" src={asDataUrl(entry.backImageBase64)} alt="카드 뒷면 미리보기" />
                </div>
              )}
            </div>
          )}
        </div>,
        document.body
      )}
    </div>
  );
}
