// 공지사항 / FAQ 관리 — 실제 API(/api/admin/boards) 연결. 기존 BoardAdminPanel을 재사용한다.
import { useCallback, useEffect, useState } from "react";
import { api, ApiError, type BoardListItem, type BoardType } from "../../../services/api";
import { BoardAdminPanel } from "../BoardAdminPanel";

export function BoardsSection({ boardType }: { boardType: BoardType }) {
  const [items, setItems] = useState<BoardListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const page = await api.listBoards({ type: boardType, size: 100 });
      setItems(page.content);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [boardType]);

  useEffect(() => { void load(); }, [load]);

  const title = boardType === "NOTICE" ? "공지사항" : "자주 묻는 질문(FAQ)";
  const eyebrow = boardType === "NOTICE" ? "공지사항" : "FAQ";

  return (
    <div className="admin-panel">
      <div className="admin-panel__head">
        <div><p className="eyebrow">{eyebrow}</p><h2 className="admin-panel__title">{title}</h2></div>
        <strong className="admin-panel__count">총 {items.length}건</strong>
      </div>
      {loading && <p className="admin-panel__note">불러오는 중…</p>}
      {error && <p className="admin-panel__note admin-panel__note--error">{error}</p>}
      {!loading && !error && <BoardAdminPanel boardType={boardType} items={items} onChanged={load} />}
    </div>
  );
}
