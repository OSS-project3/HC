// 고객지원(1:1 문의) 관리 — 실제 API(/api/admin/inquiries) 연결.
import { useCallback, useEffect, useState } from "react";
import { api, ApiError, type InquiryDetail, type InquiryListItem, type InquiryStatus } from "../../../services/api";
import { showToast } from "../../ui/toast";

// category는 백엔드가 한글 값 그대로 준다(예: "카드 발급") — 별도 라벨 매핑 없이 그대로 표시한다.
const statusLabels: Record<InquiryStatus, string> = { PENDING: "답변 대기", COMPLETED: "문의 완료" };

export function InquiriesSection() {
  const [items, setItems] = useState<InquiryListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<number | null>(null);
  const [detail, setDetail] = useState<InquiryDetail | null>(null);
  const [answerDraft, setAnswerDraft] = useState("");
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await api.listAdminInquiries());
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "문의 목록을 불러오지 못했습니다. 관리자 권한(서버 인증)이 필요합니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const openDetail = async (id: number) => {
    if (openId === id) { setOpenId(null); setDetail(null); return; }
    setOpenId(id);
    setDetail(null);
    try {
      const d = await api.getAdminInquiry(id);
      setDetail(d);
      setAnswerDraft(d.answer ?? "");
    } catch {
      showToast("문의 상세를 불러오지 못했습니다.");
    }
  };

  const submitAnswer = async (id: number) => {
    const text = answerDraft.trim();
    if (!text) { showToast("답변 내용을 입력해 주세요."); return; }
    setSaving(true);
    try {
      await api.answerInquiry(id, text);
      showToast("답변이 저장되었습니다. 문의 상태가 완료로 변경됩니다.");
      await load();
      const d = await api.getAdminInquiry(id);
      setDetail(d);
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "답변 저장에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  const toggleStatus = async (id: number, next: InquiryStatus) => {
    try {
      await api.updateInquiryStatus(id, next);
      await load();
      if (openId === id) setDetail((prev) => (prev ? { ...prev, status: next } : prev));
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "상태 변경에 실패했습니다.");
    }
  };

  return (
    <div className="admin-panel">
      <div className="admin-panel__head">
        <div><p className="eyebrow">고객지원</p><h2 className="admin-panel__title">1:1 문의 내역</h2></div>
        <strong className="admin-panel__count">총 {items.length}건</strong>
      </div>

      {loading && <p className="admin-panel__note">불러오는 중…</p>}
      {error && <p className="admin-panel__note admin-panel__note--error">{error}</p>}

      {!loading && !error && (
        <div className="admin__table-wrap">
          <table className="admin__table">
            <thead>
              <tr><th>유형</th><th>제목</th><th>문의자</th><th>이메일</th><th>연락처</th><th>접수일</th><th>상태</th></tr>
            </thead>
            <tbody>
              {items.map((it) => (
                <tr key={it.id} className={openId === it.id ? "is-open" : undefined}>
                  <td><span className="admin__badge is-inquiry">{it.category}</span></td>
                  <td>
                    <button className="admin__linklike" onClick={() => openDetail(it.id)} aria-expanded={openId === it.id}>{it.title}</button>
                    {openId === it.id && (
                      <div className="admin__inquiry-content">
                        {!detail ? <p className="admin-panel__note">불러오는 중…</p> : (
                          <>
                            <b>문의 내용</b>
                            <p>{detail.content}</p>
                            <b>답변 작성</b>
                            <textarea className="field__textarea" rows={4} placeholder="답변 내용을 입력하세요." value={answerDraft} onChange={(e) => setAnswerDraft(e.target.value)} />
                            <div className="admin__inquiry-answer-foot">
                              <button type="button" className="admin__btn admin__btn--primary" disabled={saving} onClick={() => submitAnswer(it.id)}>{detail.answeredAt ? "답변 수정" : "답변 저장"}</button>
                              {detail.answeredAt && <span className="admin__muted">답변 완료 · {new Date(detail.answeredAt).toLocaleString("ko-KR")}</span>}
                            </div>
                          </>
                        )}
                      </div>
                    )}
                  </td>
                  <td>{it.name}</td>
                  <td>{it.email}</td>
                  <td className="admin__mono">{it.phone}</td>
                  <td>{new Date(it.createdAt).toLocaleDateString("ko-KR")}</td>
                  <td>
                    <button
                      type="button"
                      className={`admin__status-pill ${it.status === "COMPLETED" ? "is-completed" : "is-waiting"}`}
                      onClick={() => toggleStatus(it.id, it.status === "COMPLETED" ? "PENDING" : "COMPLETED")}
                      title="클릭하여 상태 전환"
                    >
                      {statusLabels[it.status]}
                    </button>
                  </td>
                </tr>
              ))}
              {items.length === 0 && <tr><td className="admin__empty" colSpan={7}>접수된 문의가 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
