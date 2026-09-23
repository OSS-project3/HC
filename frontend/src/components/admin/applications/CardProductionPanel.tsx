import { useEffect, useRef, useState } from "react";
import { cardTypeById } from "../../../data/cards";
import {
  api,
  ApiError,
  type ApplicationStatus,
  type CardDesignOption,
  type StudentTextColor
} from "../../../services/api";
import { showToast } from "../../ui/toast";

import { asDataUrl, todayIso } from "./applicationUtils";
import { CardDesignSelector } from "./CardDesignSelector";

const STUDENT_TEXT_COLOR_LABEL: Record<StudentTextColor, string> = { DARK_GRAY: "진회색", WHITE: "흰색" };

// 카드 미리보기 자동 갱신(2026-09-22 확정 정책) — 이름·카드번호 저장 성공 또는 디자인·발급일·
// 글씨색 변경 후 이 지연만큼 기다렸다가 자동으로 미리보기를 다시 부른다.
const AUTO_PREVIEW_DEBOUNCE_MS = 800;

export function CardProductionPanel({
  appId, memberId, cardTypeId, applicationStatus, nameConfirmed, cardNumber,
  confirmedCardDesignId, confirmedCardIssueDate, confirmedFrontTextColor, confirmedBackTextColor, onGenerated,
}: {
  appId: number;
  memberId: number;
  cardTypeId: number;
  applicationStatus: ApplicationStatus;
  // 이름 확정 여부(member.surname && member.assignedName) — 자동 미리보기 필수 조건.
  nameConfirmed: boolean;
  // 저장된 카드번호 — 자동 미리보기 필수 조건이자, 값이 바뀌면(=방금 저장 성공) 갱신 트리거가 된다.
  cardNumber?: string;
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
  const [autoPreviewLoading, setAutoPreviewLoading] = useState(false);
  const [autoPreviewError, setAutoPreviewError] = useState<string | null>(null);
  // 마지막으로 시작한 자동 미리보기 요청의 순번 — 늦게 도착한 이전 응답이 최신 이미지를 덮어쓰지 않도록 막는다.
  const autoPreviewSeq = useRef(0);
  // 최초 마운트(=신청 상세 진입) 시점의 effect 실행은 건너뛴다 — "최초 진입 시 전체 구성원 일괄 호출 금지" 정책.
  const skippedFirstRun = useRef(false);

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

  // PRODUCING 이후에는 백엔드 미리보기 API 자체가 막히므로(2-C는 NAME_EDITING/PRODUCTION_READY 전용),
  // 실시간 재렌더링 대신 이미 생성·저장된 카드 이미지를 그대로 보여준다.
  const usesGeneratedImage = applicationStatus === "PRODUCING" || applicationStatus === "COMPLETED";

  useEffect(() => {
    if (!usesGeneratedImage) return;
    let cancelled = false;
    void (async () => {
      try {
        const data = await api.getAdminMemberCardDownload(appId, memberId);
        if (!cancelled) setPreview({ front: data.cardFrontUrl, back: data.cardBackUrl });
      } catch {
        // 카드 파일이 아직 없는 경우(드묾) — 조용히 넘어간다. "다운로드" 버튼을 누르면 같은 오류가 토스트로 뜬다.
      }
    })();
    return () => { cancelled = true; };
  }, [usesGeneratedImage, appId, memberId]);

  // 카드 미리보기 자동 갱신 — 이름·카드번호 저장 성공(prop 값 변경) 또는 디자인·발급일·글씨색 변경 시
  // 800ms 뒤 자동으로 다시 부른다. 입력 중인 값이 아니라 이미 저장된 값(nameConfirmed/cardNumber)과
  // 이 패널 자체의 확정 요청 파라미터만 쓴다 — DB 저장이나 관리자 활동 로그를 남기지 않는 읽기 전용 호출.
  useEffect(() => {
    if (usesGeneratedImage) return;
    if (!skippedFirstRun.current) { skippedFirstRun.current = true; return; }
    if (!nameConfirmed || !cardNumber || !designId || !issueDate) {
      setAutoPreviewError(null);
      return;
    }
    const seq = ++autoPreviewSeq.current;
    const timer = setTimeout(() => {
      void (async () => {
        const body = requestBody();
        if (!body) return;
        setAutoPreviewLoading(true);
        try {
          const data = await api.getCardPreview(appId, memberId, body);
          if (seq !== autoPreviewSeq.current) return; // 늦게 도착한 이전 요청 — 무시
          setPreview({ front: asDataUrl(data.front), back: asDataUrl(data.back) });
          setAutoPreviewError(null);
        } catch (e) {
          if (seq !== autoPreviewSeq.current) return;
          setAutoPreviewError(e instanceof ApiError ? e.message : "미리보기 갱신에 실패했습니다.");
        } finally {
          if (seq === autoPreviewSeq.current) setAutoPreviewLoading(false);
        }
      })();
    }, AUTO_PREVIEW_DEBOUNCE_MS);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nameConfirmed, cardNumber, designId, issueDate, frontTextColor, backTextColor, usesGeneratedImage]);

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
        <CardDesignSelector
          designs={designs}
          value={designId}
          onChange={setDesignId}
          disabled={busy || designs.length === 0}
          locked={Boolean(confirmedCardDesignId)}
        />
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
      {!usesGeneratedImage && !nameConfirmed && (
        <p className="admin__muted">이름이 확정되면 미리보기가 자동으로 갱신됩니다.</p>
      )}
      {!usesGeneratedImage && nameConfirmed && !cardNumber && (
        <p className="admin__muted">카드번호를 저장하면 미리보기가 자동으로 갱신됩니다.</p>
      )}
      {!usesGeneratedImage && autoPreviewLoading && (
        <p className="admin__muted">미리보기 자동 갱신 중…</p>
      )}
      {!usesGeneratedImage && autoPreviewError && (
        <p className="admin-panel__note admin-panel__note--error">
          미리보기 자동 갱신 실패: {autoPreviewError}{" "}
          <button type="button" className="admin__btn" disabled={busy} onClick={() => void previewCard()}>다시 시도</button>
        </p>
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

