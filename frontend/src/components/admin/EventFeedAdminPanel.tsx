import { useState } from "react";
import { emptyFeedPost, type FeedPost } from "../../data/eventFeedPosts";
import "./EventFeedAdminPanel.css";

export function EventFeedAdminPanel({
  label,
  items,
  onChange,
  showCompanyFields = false,
  compact = false,
}: {
  label: string;
  items: FeedPost[];
  onChange: (items: FeedPost[]) => void;
  showCompanyFields?: boolean;
  compact?: boolean;
}) {
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [editing, setEditing] = useState<FeedPost | null>(null);
  const beginCreate = () => { setEditingIndex(null); setEditing(emptyFeedPost()); };
  const beginEdit = (index: number) => { setEditingIndex(index); setEditing(items[index]); };
  const save = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!editing) return;
    const normalized = { ...editing, image: editing.image || undefined, company: editing.company || undefined, logoUrl: editing.logoUrl || undefined };
    onChange(editingIndex === null ? [normalized, ...items] : items.map((item, index) => index === editingIndex ? normalized : item));
    setEditing(null);
    setEditingIndex(null);
  };

  return (
    <section className={`event-feed-admin${compact ? " event-feed-admin--compact" : " page-container"}`}>
      <div className="event-feed-admin__head">
        <strong>{label} 관리</strong>
        <button type="button" onClick={beginCreate}>글쓰기</button>
      </div>
      {editing && (
        <form className="event-feed-admin__form" onSubmit={save}>
          <input className="field__input" placeholder="제목" value={editing.title} onChange={(event) => setEditing({ ...editing, title: event.target.value })} required />
          <input className="field__input" placeholder="일자 예: 2027.01.15" value={editing.date} onChange={(event) => setEditing({ ...editing, date: event.target.value })} required />
          <input className="field__input" placeholder="장소" value={editing.place} onChange={(event) => setEditing({ ...editing, place: event.target.value })} required />
          <input className="field__input" placeholder="주최" value={editing.host} onChange={(event) => setEditing({ ...editing, host: event.target.value })} required />
          <input className="field__input" placeholder="발급 카드" value={editing.cardLabel} onChange={(event) => setEditing({ ...editing, cardLabel: event.target.value })} required />
          {showCompanyFields && (
            <>
              <input className="field__input" placeholder="회사명 또는 단체명" value={editing.company || ""} onChange={(event) => setEditing({ ...editing, company: event.target.value })} />
              <input className="field__input" placeholder="로고 이미지 경로" value={editing.logoUrl || ""} onChange={(event) => setEditing({ ...editing, logoUrl: event.target.value })} />
            </>
          )}
          <input className="field__input" placeholder="대표 이미지 경로" value={editing.image || ""} onChange={(event) => setEditing({ ...editing, image: event.target.value })} />
          <textarea className="field__textarea" placeholder="내용" value={editing.text} onChange={(event) => setEditing({ ...editing, text: event.target.value })} required />
          <div className="event-feed-admin__actions">
            <button type="submit">저장</button>
            <button type="button" onClick={() => setEditing(null)}>취소</button>
          </div>
        </form>
      )}
      <div className="event-feed-admin__list">
        {items.map((item, index) => (
          <div key={`${item.title}-${item.date}`}>
            <span>{item.date}</span>
            <b>{item.title}</b>
            <button type="button" onClick={() => beginEdit(index)}>수정</button>
            <button type="button" onClick={() => { if (confirm("삭제하시겠습니까?")) onChange(items.filter((_, itemIndex) => itemIndex !== index)); }}>삭제</button>
          </div>
        ))}
      </div>
    </section>
  );
}
