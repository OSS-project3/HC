import { useState } from "react";
import { Button } from "../ui/Button";

export interface ManagedContent {
  id: string;
  title: string;
  content: string;
  meta?: string;
  attachment?: string;
}

export function loadManagedContent(key: string, fallback: ManagedContent[]) {
  try {
    return (JSON.parse(localStorage.getItem(`managed-content:${key}`) || "null") as ManagedContent[]) || fallback;
  } catch {
    return fallback;
  }
}

export function ContentAdminPanel({
  label,
  items,
  onChange,
  allowAttachment = false,
}: {
  label: string;
  items: ManagedContent[];
  onChange: (items: ManagedContent[]) => void;
  allowAttachment?: boolean;
}) {
  const [editing, setEditing] = useState<ManagedContent | null>(null);

  const save = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!editing) return;
    const exists = items.some((item) => item.id === editing.id);
    onChange(exists ? items.map((item) => (item.id === editing.id ? editing : item)) : [editing, ...items]);
    setEditing(null);
  };

  return (
    <section className="content-admin" style={{ margin: "24px 0", padding: 20, border: "1px solid #d8d1c4" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <strong>{label} 관리</strong>
        <Button type="button" onClick={() => setEditing({ id: crypto.randomUUID(), title: "", content: "", meta: new Date().toISOString().slice(0, 10) })}>
          글쓰기
        </Button>
      </div>

      {editing && (
        <form onSubmit={save} style={{ display: "grid", gap: 10, marginTop: 16 }}>
          <input className="field__input" aria-label="제목" placeholder="제목" value={editing.title} onChange={(e) => setEditing({ ...editing, title: e.target.value })} required />
          <textarea className="field__textarea" aria-label="내용" placeholder="내용" value={editing.content} onChange={(e) => setEditing({ ...editing, content: e.target.value })} required />
          <input className="field__input" aria-label="분류 또는 날짜" placeholder="분류 또는 날짜" value={editing.meta || ""} onChange={(e) => setEditing({ ...editing, meta: e.target.value })} />

          {allowAttachment && (
            <div style={{ display: "grid", gap: 8, padding: 12, border: "1px solid #e5ded2", background: "#fff" }}>
              <strong style={{ fontSize: 13, color: "#263d5b" }}>첨부파일</strong>
              <input className="field__input" aria-label="첨부파일명" placeholder="첨부파일명" value={editing.attachment || ""} onChange={(e) => setEditing({ ...editing, attachment: e.target.value || undefined })} />
              <input
                className="field__input"
                aria-label="첨부파일 선택"
                type="file"
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  if (file) setEditing({ ...editing, attachment: file.name });
                }}
              />
              {editing.attachment && (
                <Button type="button" variant="ghost" onClick={() => setEditing({ ...editing, attachment: undefined })}>
                  첨부파일 삭제
                </Button>
              )}
            </div>
          )}

          <div>
            <Button type="submit">저장</Button>{" "}
            <Button type="button" variant="ghost" onClick={() => setEditing(null)}>
              취소
            </Button>
          </div>
        </form>
      )}

      <div style={{ display: "grid", gap: 8, marginTop: 16 }}>
        {items.map((item) => (
          <div key={item.id} style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <span style={{ flex: 1 }}>{item.title}</span>
            <Button type="button" variant="outline" onClick={() => setEditing(item)}>
              수정
            </Button>
            <Button type="button" variant="ghost" onClick={() => { if (confirm("삭제하시겠습니까?")) onChange(items.filter((entry) => entry.id !== item.id)); }}>
              삭제
            </Button>
          </div>
        ))}
      </div>
    </section>
  );
}
