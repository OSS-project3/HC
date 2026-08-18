import { useState } from "react";
import { showToast } from "../ui/toast";
import { api, type EventType } from "../../services/api";
import type { FeedPost } from "../../data/eventFeedPosts";
import "./EventFeedAdminPanel.css";

interface Draft {
  id: number | null;
  title: string;
  eventDateText: string;
  eventDate: string;
  place: string;
  host: string;
  cardLabel: string;
  content: string;
  displayOrder: string;
  visible: boolean;
}

const emptyDraft = (): Draft => ({ id: null, title: "", eventDateText: "", eventDate: "", place: "", host: "", cardLabel: "", content: "", displayOrder: "", visible: true });

/**
 * Admin CRUD panel backed by the real Event API (`/api/admin/events`).
 * Handles BOOTH and COLLABORATION events. Limitations from the backend contract:
 * - No `company`/`logoUrl` fields (collaboration logos not supported by the API).
 * - Gallery `images` can only be set on create, not edited via PATCH.
 * - `visible`/`displayOrder` are not returned by the detail API, so editing an
 *   existing event resets `visible=true` and clears `displayOrder`.
 */
export function EventAdminPanel({ label, eventType, items, onChanged }: { label: string; eventType: EventType; items: FeedPost[]; onChanged: () => void }) {
  const [draft, setDraft] = useState<Draft | null>(null);
  const [thumbnail, setThumbnail] = useState<File | null>(null);
  const [gallery, setGallery] = useState<File[]>([]);
  const [busy, setBusy] = useState(false);

  const beginCreate = () => { setDraft(emptyDraft()); setThumbnail(null); setGallery([]); };
  const beginEdit = (post: FeedPost) => {
    if (post.id == null) return;
    setDraft({ id: post.id, title: post.title, eventDateText: post.date, eventDate: "", place: post.place, host: post.host, cardLabel: post.cardLabel, content: post.text, displayOrder: "", visible: true });
    setThumbnail(null);
    setGallery([]);
  };

  const save = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!draft || busy) return;
    setBusy(true);
    try {
      const body = {
        eventType,
        title: draft.title.trim(),
        eventDateText: draft.eventDateText.trim(),
        eventDate: draft.eventDate || undefined,
        place: draft.place.trim(),
        host: draft.host.trim(),
        cardLabel: draft.cardLabel.trim(),
        content: draft.content.trim(),
        visible: draft.visible,
        displayOrder: draft.displayOrder ? Number(draft.displayOrder) : undefined,
      };
      if (draft.id === null) {
        await api.createEvent(body, thumbnail ?? undefined, gallery);
        showToast("등록되었습니다.");
      } else {
        await api.updateEvent(draft.id, body, thumbnail ?? undefined);
        showToast("수정되었습니다.");
      }
      setDraft(null);
      onChanged();
    } catch (error) {
      showToast(error instanceof Error ? error.message : "저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: number | undefined) => {
    if (id == null || !confirm("삭제하시겠습니까?")) return;
    try {
      await api.deleteEvent(id);
      showToast("삭제되었습니다.");
      onChanged();
    } catch (error) {
      showToast(error instanceof Error ? error.message : "삭제에 실패했습니다.");
    }
  };

  return (
    <section className="event-feed-admin page-container">
      <div className="event-feed-admin__head">
        <strong>{label} 관리</strong>
        <button type="button" onClick={beginCreate}>글쓰기</button>
      </div>
      {draft && (
        <form className="event-feed-admin__form" onSubmit={save}>
          <input className="field__input" placeholder="제목" value={draft.title} onChange={(e) => setDraft({ ...draft, title: e.target.value })} required />
          <input className="field__input" placeholder="일자 표시 예: 2027.01.15" value={draft.eventDateText} onChange={(e) => setDraft({ ...draft, eventDateText: e.target.value })} required />
          <input className="field__input" type="date" aria-label="정렬용 날짜(선택)" value={draft.eventDate} onChange={(e) => setDraft({ ...draft, eventDate: e.target.value })} />
          <input className="field__input" placeholder="장소" value={draft.place} onChange={(e) => setDraft({ ...draft, place: e.target.value })} required />
          <input className="field__input" placeholder="주최" value={draft.host} onChange={(e) => setDraft({ ...draft, host: e.target.value })} required />
          <input className="field__input" placeholder="발급 카드" value={draft.cardLabel} onChange={(e) => setDraft({ ...draft, cardLabel: e.target.value })} required />
          <input className="field__input" type="number" placeholder="정렬 순서(선택)" value={draft.displayOrder} onChange={(e) => setDraft({ ...draft, displayOrder: e.target.value })} />
          <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <input type="checkbox" checked={draft.visible} onChange={(e) => setDraft({ ...draft, visible: e.target.checked })} />
            <span>공개</span>
          </label>
          <label className="field__label">대표 이미지(썸네일){draft.id !== null ? " — 교체 시에만 선택" : ""}</label>
          <input className="field__input" type="file" accept="image/png,image/jpeg,image/webp" onChange={(e) => setThumbnail(e.target.files?.[0] ?? null)} />
          {draft.id === null && (
            <>
              <label className="field__label">갤러리 이미지(최대 10장, 등록 시에만 설정)</label>
              <input className="field__input" type="file" accept="image/png,image/jpeg,image/webp" multiple onChange={(e) => setGallery(Array.from(e.target.files ?? []))} />
            </>
          )}
          <textarea className="field__textarea" placeholder="내용" value={draft.content} onChange={(e) => setDraft({ ...draft, content: e.target.value })} required />
          <div className="event-feed-admin__actions">
            <button type="submit" disabled={busy}>저장</button>
            <button type="button" onClick={() => setDraft(null)}>취소</button>
          </div>
        </form>
      )}
      <div className="event-feed-admin__list">
        {items.map((item) => (
          <div key={item.id ?? `${item.title}-${item.date}`}>
            <span>{item.date}</span>
            <b>{item.title}</b>
            <button type="button" onClick={() => beginEdit(item)}>수정</button>
            <button type="button" onClick={() => remove(item.id)}>삭제</button>
          </div>
        ))}
      </div>
    </section>
  );
}
