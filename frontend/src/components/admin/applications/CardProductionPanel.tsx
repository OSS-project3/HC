import { useEffect, useState } from "react";
import { cardTypeById } from "../../../data/cards";
import {
  api,
  ApiError,
  type CardDesignOption,
  type StudentTextColor
} from "../../../services/api";
import { showToast } from "../../ui/toast";

import { asDataUrl, todayIso } from "./applicationUtils";

const STUDENT_TEXT_COLOR_LABEL: Record<StudentTextColor, string> = { DARK_GRAY: "진회색", WHITE: "흰색" };

export function CardProductionPanel({ appId, memberId, cardTypeId, confirmedCardDesignId, confirmedCardIssueDate, confirmedFrontTextColor, confirmedBackTextColor, onGenerated }: {
  appId: number;
  memberId: number;
  cardTypeId: number;
  // 카드 생성 성공 시 신청 단위로 확정되어 이후 다른 값이면 재생성이 거절된다(백엔드가 거절).
  confirmedCardDesignId?: number;
  confirmedCardIssueDate?: string;
  // 학생증(STUDENT) 전용 — 신청 단위로 이미 확정된 값이면 다른 색으로 바꿀 수 없다(백엔드가 거절).
  confirmedFrontTextColor?: StudentTextColor;
  confirmedBackTextColor?: StudentTextColor;
  onGenerated: () => Promise<void>;
}) {
  const isStudentCard = cardTypeById[cardTypeId] === "student";
  const [designs, setDesigns] = useState<CardDesignOption[]>([]);
  const [designId, setDesignId] = useState(confirmedCardDesignId ? String(confirmedCardDesignId) : "");
  const [issueDate, setIssueDate] = useState(confirmedCardIssueDate ?? todayIso());
  const [frontTextColor, setFrontTextColor] = useState<StudentTextColor>(confirmedFrontTextColor ?? "DARK_GRAY");
  const [backTextColor, setBackTextColor] = useState<StudentTextColor>(confirmedBackTextColor ?? "DARK_GRAY");
  const [preview, setPreview] = useState<{ front: string; back: string } | null>(null);
  const [busy, setBusy] = useState(false);

  // 다른 멤버의 카드 생성으로 신청 단위 색상이 막 확정된 경우에도 선택값을 확정값으로 맞춘다.
  useEffect(() => { if (confirmedFrontTextColor) setFrontTextColor(confirmedFrontTextColor); }, [confirmedFrontTextColor]);
  useEffect(() => { if (confirmedBackTextColor) setBackTextColor(confirmedBackTextColor); }, [confirmedBackTextColor]);
  useEffect(() => { if (confirmedCardDesignId) setDesignId(String(confirmedCardDesignId)); }, [confirmedCardDesignId]);
  useEffect(() => { if (confirmedCardIssueDate) setIssueDate(confirmedCardIssueDate); }, [confirmedCardIssueDate]);

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

  // 이미 카드가 생성된 신청은 확정 디자인의 이름·번호를 화면에 보여줘야 하므로 자동으로 목록을 불러온다.
  // (매번 "디자인 불러오기"를 눌러야 확정값이 드롭다운에 정상 표시되는 문제 방지)
  useEffect(() => {
    if (confirmedCardDesignId) void loadDesigns();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [confirmedCardDesignId]);

  const requestBody = () => {
    const id = Number(designId);
    if (!id) {
      showToast("카드 디자인을 선택해 주세요.");
      return null;
    }
    // 비학생증 카드는 색상 필드를 아예 보내면 안 된다(서버가 INVALID_INPUT으로 거절).
    if (!isStudentCard) return { cardDesignId: id, issueDate };
    return { cardDesignId: id, issueDate, studentFrontTextColor: frontTextColor, studentBackTextColor: backTextColor };
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
        <select className="field__select" value={designId} onChange={(e) => setDesignId(e.target.value)} disabled={busy || designs.length === 0 || Boolean(confirmedCardDesignId)}>
          <option value="">디자인 선택</option>
          {designs.map((d) => <option key={d.id} value={d.id}>{d.name} #{d.designNumber}</option>)}
        </select>
        <input className="field__input admin-card-tools__date" type="date" value={issueDate} onChange={(e) => setIssueDate(e.target.value)} disabled={busy || Boolean(confirmedCardIssueDate)} />
        {isStudentCard && (
          <>
            <select
              className="field__select"
              value={frontTextColor}
              onChange={(e) => setFrontTextColor(e.target.value as StudentTextColor)}
              disabled={busy || Boolean(confirmedFrontTextColor)}
              title="학생증 앞면 글씨색"
            >
              {(Object.keys(STUDENT_TEXT_COLOR_LABEL) as StudentTextColor[]).map((color) => (
                <option key={color} value={color}>앞면 글씨색: {STUDENT_TEXT_COLOR_LABEL[color]}</option>
              ))}
            </select>
            <select
              className="field__select"
              value={backTextColor}
              onChange={(e) => setBackTextColor(e.target.value as StudentTextColor)}
              disabled={busy || Boolean(confirmedBackTextColor)}
              title="학생증 뒷면 글씨색"
            >
              {(Object.keys(STUDENT_TEXT_COLOR_LABEL) as StudentTextColor[]).map((color) => (
                <option key={color} value={color}>뒷면 글씨색: {STUDENT_TEXT_COLOR_LABEL[color]}</option>
              ))}
            </select>
          </>
        )}
        <button type="button" className="admin__btn" disabled={busy} onClick={previewCard}>미리보기</button>
        <button type="button" className="admin__btn admin__btn--primary" disabled={busy} onClick={generate}>카드 생성</button>
        <button type="button" className="admin__btn" disabled={busy} onClick={downloadMember}>다운로드</button>
      </div>
      {(confirmedCardDesignId || confirmedCardIssueDate) && (
        <p className="admin__muted">이미 카드가 생성되어 디자인·발급일자가 확정됐습니다 — 같은 값으로만 재생성할 수 있습니다.</p>
      )}
      {isStudentCard && (confirmedFrontTextColor || confirmedBackTextColor) && (
        <p className="admin__muted">이미 카드가 생성되어 글씨색이 확정됐습니다 — 같은 색으로만 재생성할 수 있습니다.</p>
      )}
      {preview && (
        <div className="admin-card-tools__preview">
          <img src={preview.front} alt="카드 앞면 미리보기" />
          <img src={preview.back} alt="카드 뒷면 미리보기" />
        </div>
      )}
    </div>
  );
}

