// 공지사항 / FAQ 관리 — 실제 API(/api/admin/boards) 연결. 기존 BoardAdminPanel을 재사용한다.
// 목록은 서버 페이지네이션(page/totalPages)과 연결한다(§1.20) — 고정 size 단일 호출 금지.
import { useCallback, useEffect, useState } from "react";
import { api, ApiError, type BoardListItem, type BoardType } from "../../../services/api";
import { BoardAdminPanel } from "../BoardAdminPanel";
import { AdminPager } from "../AdminPager";

const PAGE_SIZE = 20;

export function BoardsSection({ boardType }: { boardType: BoardType }) {
  const [items, setItems] = useState<BoardListItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => { setPage(0); }, [boardType]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await api.listBoards({ type: boardType, page, size: PAGE_SIZE });
      setItems(result.content);
      setTotalPages(Math.max(1, result.totalPages));
      setTotal(result.totalElements);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [boardType, page]);

  useEffect(() => { void load(); }, [load]);

  const title = boardType === "NOTICE" ? "공지사항" : "자주 묻는 질문(FAQ)";
  const eyebrow = boardType === "NOTICE" ? "공지사항" : "FAQ";

  return (
    <div className="admin-panel">
      <div className="admin-panel__head">
        <div><p className="eyebrow">{eyebrow}</p><h2 className="admin-panel__title">{title}</h2></div>
        <strong className="admin-panel__count">총 {total}건</strong>
      </div>
      {loading && <p className="admin-panel__note">불러오는 중…</p>}
      {error && <p className="admin-panel__note admin-panel__note--error">{error}</p>}
      {!loading && !error && <BoardAdminPanel boardType={boardType} items={items} onChanged={load} />}
      {!error && <AdminPager page={page} totalPages={totalPages} disabled={loading} onChange={setPage} />}
    </div>
  );
}
