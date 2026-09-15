import { useState } from "react";
import {
  api,
  ApiError
} from "../../../services/api";
import { showToast } from "../../ui/toast";

// 개인/단일 멤버 카드번호 확정 — 관리자 직접 입력(PUT .../members/{memberId}/card-number).
export function CardNumberField({ appId, memberId, current, onSaved }: { appId: number; memberId: number; current?: string; onSaved: () => Promise<void> }) {
  const [value, setValue] = useState(current ?? "");
  const [busy, setBusy] = useState(false);
  const save = async () => {
    if (!value.trim()) { showToast("카드번호를 입력해 주세요."); return; }
    setBusy(true);
    try {
      await api.assignCardNumber(appId, memberId, value.trim());
      showToast("카드번호를 저장했습니다.");
      await onSaved();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "카드번호 저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="admin-naming__cardnum">
      <span className="field__label">카드번호</span>
      <div className="field__with-btn">
        <input className="field__input" value={value} onChange={(e) => setValue(e.target.value)} placeholder="ROK-00000-0000" maxLength={30} />
        <button type="button" className="postal-btn" disabled={busy} onClick={save}>저장</button>
      </div>
    </div>
  );
}

