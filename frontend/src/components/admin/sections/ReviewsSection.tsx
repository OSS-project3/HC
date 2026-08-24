// 후기 관리 — 실제 API(/api/reviews) 연결. 전용 admin 목록 API는 없어 공개 목록을 사용하고,
// 관리자는 임의 후기를 삭제할 수 있다(백엔드 ReviewService가 ADMIN 삭제를 허용).
import { useCallback, useEffect, useState } from "react";
import { api, ApiError, type ReviewListItem } from "../../../services/api";
import { showToast } from "../../ui/toast";

export function ReviewsSection() {
  const [items, setItems] = useState<ReviewListItem[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await api.listReviews({ size: 50 });
      setItems(page.content);
      setTotal(page.totalElements);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "후기를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const remove = async (id: number, title: string) => {
    if (!window.confirm(`후기 "${title}"를 삭제하시겠습니까?`)) return;
    try {
      await api.deleteReview(id);
      showToast("후기가 삭제되었습니다.");
      await load();
    } catch (e) {
      showToast(e instanceof ApiError ? e.message : "삭제에 실패했습니다. 관리자 권한(서버 인증)이 필요합니다.");
    }
  };

  return (
    <div className="admin-panel">
      <div className="admin-panel__head">
        <div><p className="eyebrow">후기</p><h2 className="admin-panel__title">후기 관리</h2></div>
        <strong className="admin-panel__count">총 {total}건</strong>
      </div>
      {loading && <p className="admin-panel__note">불러오는 중…</p>}
      {error && <p className="admin-panel__note admin-panel__note--error">{error}</p>}
      {!loading && !error && (
        <div className="admin__table-wrap">
          <table className="admin__table">
            <thead><tr><th>카드 종류</th><th>제목</th><th>작성자</th><th>구분</th><th>작성일</th><th>관리</th></tr></thead>
            <tbody>
              {items.map((r) => (
                <tr key={r.id}>
                  <td>{r.cardType.name}</td>
                  <td>{r.title}</td>
                  <td>{r.authorName}</td>
                  <td>{r.applicationType === "GROUP" ? "단체" : "개인"}</td>
                  <td>{new Date(r.createdAt).toLocaleDateString("ko-KR")}</td>
                  <td><button type="button" className="admin__btn admin__btn--danger" onClick={() => remove(r.id, r.title)}>삭제</button></td>
                </tr>
              ))}
              {items.length === 0 && <tr><td className="admin__empty" colSpan={6}>등록된 후기가 없습니다.</td></tr>}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
