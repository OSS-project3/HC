import { useState } from "react";
import {
  api,
  ApiError,
  type CardDesignOption
} from "../../../services/api";
import { showToast } from "../../ui/toast";

import { asDataUrl, todayIso } from "./applicationUtils";
export function CardProductionPanel({ appId, memberId, cardTypeId, onGenerated }: {
  appId: number;
  memberId: number;
  cardTypeId: number;
  onGenerated: () => Promise<void>;
}) {
  const [designs, setDesigns] = useState<CardDesignOption[]>([]);
  const [designId, setDesignId] = useState("");
  const [issueDate, setIssueDate] = useState(todayIso());
  const [preview, setPreview] = useState<{ front: string; back: string } | null>(null);
  const [busy, setBusy] = useState(false);

  const loadDesigns = async () => {
    setBusy(true);
    try {
      const rows = await api.listCardDesigns({ cardTypeId, active: true, applicationId: appId });
      setDesigns(rows);
      setDesignId((current) => current || String(rows.find((d) => d.isDefault)?.id ?? rows[0]?.id ?? ""));
      if (rows.length === 0) showToast("사용 가능한 카드 디자인이 없습니다.");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "카드 디자인을 불러오지 못했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const requestBody = () => {
    const id = Number(designId);
    if (!id) {
      showToast("카드 디자인을 선택해 주세요.");
      return null;
    }
    return { cardDesignId: id, issueDate };
  };

  const previewCard = async () => {
    const body = requestBody();
    if (!body) return;
    setBusy(true);
    try {
      const data = await api.getCardPreview(appId, memberId, body);
      setPreview({ front: asDataUrl(data.front), back: asDataUrl(data.back) });
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "카드 미리보기에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const generate = async () => {
    const body = requestBody();
    if (!body) return;
    setBusy(true);
    try {
      await api.generateCard(appId, memberId, body);
      showToast("카드 이미지를 생성해 저장했습니다.");
      await onGenerated();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "카드 생성에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const downloadMember = async () => {
    setBusy(true);
    try {
      const data = await api.getAdminMemberCardDownload(appId, memberId);
      window.open(data.cardFrontUrl, "_blank", "noopener,noreferrer");
      window.open(data.cardBackUrl, "_blank", "noopener,noreferrer");
      showToast("카드 다운로드 링크를 열었습니다.");
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "카드 다운로드에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="admin-card-tools">
      <div className="admin-card-tools__row">
        <button type="button" className="admin__btn" disabled={busy} onClick={loadDesigns}>디자인 불러오기</button>
        <select className="field__select" value={designId} onChange={(e) => setDesignId(e.target.value)} disabled={busy || designs.length === 0}>
          <option value="">디자인 선택</option>
          {designs.map((d) => <option key={d.id} value={d.id}>{d.name} #{d.designNumber}</option>)}
        </select>
        <input className="field__input admin-card-tools__date" type="date" value={issueDate} onChange={(e) => setIssueDate(e.target.value)} />
        <button type="button" className="admin__btn" disabled={busy} onClick={previewCard}>미리보기</button>
        <button type="button" className="admin__btn admin__btn--primary" disabled={busy} onClick={generate}>카드 생성</button>
        <button type="button" className="admin__btn" disabled={busy} onClick={downloadMember}>다운로드</button>
      </div>
      {preview && (
        <div className="admin-card-tools__preview">
          <img src={preview.front} alt="카드 앞면 미리보기" />
          <img src={preview.back} alt="카드 뒷면 미리보기" />
        </div>
      )}
    </div>
  );
}

