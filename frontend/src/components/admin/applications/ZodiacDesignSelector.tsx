// 십이간지 디자인 세트 선택 — 옵션에 hover/focus하면 그 세트의 대표 4종(쥐/호랑이/용/돼지)
// 이미지를 조회 전용으로 미리 보여준다(2026-09-22 확정 정책). 기존 native <select>를 대체하되
// value/onChange 계약은 그대로 유지해 저장 흐름(saveZodiacDesignSet)은 손대지 않는다.
import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { api, type ZodiacDesignPreview } from "../../../services/api";
import { asDataUrl } from "./applicationUtils";
import { usePreviewPopover } from "./usePreviewPopover";

const ZODIAC_SETS = [1, 2, 3, 4, 5];
// 흰색 이미지인 세트(2026-09-13 확장분) — 체크무늬 배경 없이는 타일 안에서 형체가 안 보인다.
const WHITE_SETS = new Set([4, 5]);
const CLOSE_DELAY_MS = 150;

type CacheEntry = "loading" | "error" | ZodiacDesignPreview;

export function ZodiacDesignSelector({
  value, onChange, disabled, locked,
}: {
  value: string;
  onChange: (value: string) => void;
  /** 완전 비활성 — 열기 자체를 막는다(로딩 전·목록 없음 등). */
  disabled?: boolean;
  /** 선택 변경만 막고 조회(hover/focus 미리보기)는 계속 허용한다. */
  locked?: boolean;
}) {
  const { open, setOpen, triggerRef, panelRef, style } = usePreviewPopover();
  const [activeSet, setActiveSet] = useState<number | null>(null);
  const [cache, setCache] = useState<Record<number, CacheEntry>>({});
  const leaveTimer = useRef<number | null>(null);

  const cancelLeave = () => {
    if (leaveTimer.current != null) { window.clearTimeout(leaveTimer.current); leaveTimer.current = null; }
  };
  const enter = (set: number) => { cancelLeave(); setActiveSet(set); };
  const scheduleLeave = (set: number) => {
    cancelLeave();
    leaveTimer.current = window.setTimeout(() => {
      setActiveSet((current) => (current === set ? null : current));
      leaveTimer.current = null;
    }, CLOSE_DELAY_MS);
  };

  useEffect(() => {
    if (!open) { cancelLeave(); setActiveSet(null); }
  }, [open]);

  // 항목이 처음 활성화될 때만 요청하고 컴포넌트 생명주기 동안 세트별로 캐시한다.
  // requested는 state가 아니라 ref다 — effect의 의존성에 cache(state)를 넣으면 fetchSet 안의
  // setCache("loading")이 그 즉시 같은 effect를 재실행시켜 방금 시작한 요청 자신을 취소해 버린다
  // (cleanup의 cancelled=true가 나중에 도착하는 성공 응답까지 막는 자기 취소 버그).
  const mounted = useRef(true);
  const requested = useRef<Set<number>>(new Set());
  useEffect(() => () => { mounted.current = false; }, []);

  const fetchSet = (set: number) => {
    requested.current.add(set);
    setCache((prev) => ({ ...prev, [set]: "loading" }));
    api.getZodiacDesignPreview(set)
      .then((data) => { if (mounted.current) setCache((prev) => ({ ...prev, [set]: data })); })
      .catch(() => { if (mounted.current) setCache((prev) => ({ ...prev, [set]: "error" })); });
  };

  useEffect(() => {
    if (activeSet == null || requested.current.has(activeSet)) return;
    fetchSet(activeSet);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeSet]);

  const retry = (set: number) => fetchSet(set);

  const select = (set: number) => {
    if (locked) return;
    onChange(String(set));
    setOpen(false);
  };

  const entry = activeSet != null ? cache[activeSet] : undefined;

  return (
    <div className={`select-field preview-listbox${open ? " is-open" : ""}`}>
      <button
        ref={triggerRef}
        type="button"
        className={`field__select select-field__trigger preview-listbox__trigger${value ? "" : " select-field__trigger--placeholder"}`}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label="십이간지 디자인 세트"
        disabled={disabled}
        onClick={() => setOpen((prev) => !prev)}
      >
        <span>{value ? `디자인 세트 ${value}` : "디자인 세트 선택"}</span>
        <b className="select-field__caret" aria-hidden="true">⌄</b>
      </button>
      {open && style && createPortal(
        <div className="preview-listbox__panel" ref={panelRef} style={style}>
          <ul className="select-field__options preview-listbox__options" role="listbox" aria-label="십이간지 디자인 세트">
            {ZODIAC_SETS.map((set) => (
              <li key={set}>
                <button
                  type="button"
                  role="option"
                  aria-selected={String(set) === value}
                  className={`select-field__option${String(set) === value ? " is-selected" : ""}`}
                  onMouseEnter={() => enter(set)}
                  onMouseLeave={() => scheduleLeave(set)}
                  onFocus={() => enter(set)}
                  onBlur={() => scheduleLeave(set)}
                  onClick={() => select(set)}
                >
                  <span>디자인 세트 {set}</span>
                  {String(set) === value && <b aria-hidden="true">✓</b>}
                </button>
              </li>
            ))}
          </ul>
          {activeSet != null && (
            <div
              className="preview-listbox__preview"
              onMouseEnter={cancelLeave}
              onMouseLeave={() => scheduleLeave(activeSet)}
            >
              <p className="preview-listbox__preview-title">세트 {activeSet} 미리보기</p>
              {entry === "loading" && (
                <div className={`zodiac-preview-grid${WHITE_SETS.has(activeSet) ? " zodiac-preview-grid--checkered" : ""}`}>
                  {Array.from({ length: 4 }).map((_, i) => (
                    <div key={i} className="zodiac-preview-tile">
                      <div className="zodiac-preview-tile__img-wrap zodiac-preview-tile__img-wrap--skeleton" />
                    </div>
                  ))}
                </div>
              )}
              {entry === "error" && (
                <p className="admin-panel__note admin-panel__note--error">
                  이미지를 불러오지 못했습니다.{" "}
                  <button type="button" className="admin__btn" onClick={() => retry(activeSet)}>다시 시도</button>
                </p>
              )}
              {entry && entry !== "loading" && entry !== "error" && (
                <div className={`zodiac-preview-grid${WHITE_SETS.has(activeSet) ? " zodiac-preview-grid--checkered" : ""}`}>
                  {entry.animals.map((animal) => (
                    <div key={animal.code} className="zodiac-preview-tile">
                      <div className="zodiac-preview-tile__img-wrap">
                        <img src={asDataUrl(animal.imageBase64)} alt={animal.name} />
                      </div>
                      <span>{animal.name}</span>
                    </div>
                  ))}
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
