import { useState } from "react";
import { Button } from "../ui/Button";
import { showToast } from "../ui/toast";
import { api, type BoardAttachment, type BoardListItem, type BoardType } from "../../services/api";

interface EditingState {
  id: number | null;
  title: string;
  content: string;
  existingAttachments: BoardAttachment[];
  keepIds: number[];
  newFiles: File[];
}

/**
 * Admin CRUD panel backed by the real Board API (`/api/admin/boards`).
 * Used for NOTICE (with attachments) and FAQ (no attachments). On edit it
 * fetches the board detail to recover existing attachment ids, since the list
 * item does not carry them and PATCH uses full re-submit semantics
 * (omitting `keepAttachmentIds` deletes all existing attachments).
 */
export function BoardAdminPanel({ boardType, items, onChanged }: { boardType: BoardType; items: BoardListItem[]; onChanged: () => void }) {
  const [editing, setEditing] = useState<EditingState | null>(null);
  const [busy, setBusy] = useState(false);
  const allowAttachment = boardType === "NOTICE";

  const beginCreate = () => setEditing({ id: null, title: "", content: "", existingAttachments: [], keepIds: [], newFiles: [] });

  const beginEdit = async (id: number) => {
    try {
      const detail = await api.getBoard(id);
      setEditing({ id, title: detail.title, content: detail.content, existingAttachments: detail.attachments, keepIds: detail.attachments.map((a) => a.id), newFiles: [] });
    } catch (error) {
      showToast(error instanceof Error ? error.message : "글을 불러오지 못했습니다.");
    }
  };

  const save = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!editing || busy) return;
    setBusy(true);
    try {
      const body = { boardType, title: editing.title.trim(), content: editing.content.trim() };
      if (editing.id === null) {
        await api.createBoard(body, allowAttachment ? editing.newFiles : []);
        showToast("등록되었습니다.");
      } else {
        await api.updateBoard(editing.id, { ...body, keepAttachmentIds: allowAttachment ? editing.keepIds : [] }, allowAttachment ? editing.newFiles : []);
        showToast("수정되었습니다.");
      }
      setEditing(null);
      onChanged();
    } catch (error) {
      showToast(error instanceof Error ? error.message : "저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: number) => {
    if (!confirm("삭제하시겠습니까?")) return;
    try {
      await api.deleteBoard(id);
      showToast("삭제되었습니다.");
      onChanged();
    } catch (error) {
      showToast(error instanceof Error ? error.message : "삭제에 실패했습니다.");
    }
  };

  const label = boardType === "NOTICE" ? "공지사항" : "FAQ";

  return (
    <section className="content-admin" style={{ margin: "24px 0", padding: 20, border: "1px solid #d8d1c4" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <strong>{label} 관리</strong>
        <Button type="button" onClick={beginCreate}>글쓰기</Button>
      </div>

      {editing && (
        <form onSubmit={save} style={{ display: "grid", gap: 10, marginTop: 16 }}>
          <input className="field__input" aria-label="제목" placeholder={boardType === "FAQ" ? "질문" : "제목"} value={editing.title} onChange={(e) => setEditing({ ...editing, title: e.target.value })} required />
          <textarea className="field__textarea" aria-label="내용" placeholder={boardType === "FAQ" ? "답변" : "내용"} value={editing.content} onChange={(e) => setEditing({ ...editing, content: e.target.value })} required />

          {allowAttachment && (
            <div style={{ display: "grid", gap: 8, padding: 12, border: "1px solid #e5ded2", background: "#fff" }}>
              <strong style={{ fontSize: 13, color: "#263d5b" }}>첨부파일</strong>
              {editing.existingAttachments.map((att) => (
                <label key={att.id} style={{ display: "flex", gap: 8, alignItems: "center" }}>
                  <input type="checkbox" checked={editing.keepIds.includes(att.id)} onChange={(e) => setEditing({ ...editing, keepIds: e.target.checked ? [...editing.keepIds, att.id] : editing.keepIds.filter((k) => k !== att.id) })} />
                  <span>{att.originalFileName}</span>
                </label>
              ))}
              <input
                className="field__input"
                aria-label="첨부파일 추가"
                type="file"
                multiple
                onChange={(e) => setEditing({ ...editing, newFiles: Array.from(e.target.files ?? []) })}
              />
              <small style={{ color: "#6b7280" }}>체크 해제한 기존 첨부파일은 저장 시 삭제됩니다. (개당 10MB, 최대 10개)</small>
            </div>
          )}

          <div>
            <Button type="submit" disabled={busy}>저장</Button>{" "}
            <Button type="button" variant="ghost" onClick={() => setEditing(null)}>취소</Button>
          </div>
        </form>
      )}

      <div style={{ display: "grid", gap: 8, marginTop: 16 }}>
        {items.map((item) => (
          <div key={item.id} style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <span style={{ flex: 1 }}>{item.title}</span>
            <Button type="button" variant="outline" onClick={() => beginEdit(item.id)}>수정</Button>
            <Button type="button" variant="ghost" onClick={() => remove(item.id)}>삭제</Button>
          </div>
        ))}
      </div>
    </section>
  );
}
