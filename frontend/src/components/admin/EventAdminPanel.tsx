import { useCallback, useEffect, useState } from "react";
import { showToast } from "../ui/toast";
import { api, type EventAdminListItem, type EventImage, type EventType } from "../../services/api";
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
  company: string;
  displayOrder: string;
  visible: boolean;
}

const emptyDraft = (): Draft => ({
  id: null, title: "", eventDateText: "", eventDate: "", place: "", host: "",
  cardLabel: "", content: "", company: "", displayOrder: "", visible: true,
});

const MAX_GALLERY = 10;

/**
 * Admin CRUD panel backed by the real Event API (`/api/admin/events`).
 * BOOTH / COLLABORATION 공통. 협업(COLLABORATION)일 때만 회사명·로고를 다룬다.
 * 썸네일(1)·로고(1)·갤러리(다중)를 업로드하고, 수정 시 기존 갤러리 이미지는
 * keepImageIds로 유지/삭제하며 새 파일을 뒤에 추가한다.
 */
export function EventAdminPanel({ label, eventType, onChanged }: { label: string; eventType: EventType; onChanged?: () => void }) {
  const isCollab = eventType === "COLLABORATION";

  const [items, setItems] = useState<EventAdminListItem[]>([]);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [thumbnail, setThumbnail] = useState<File | null>(null);
  const [logo, setLogo] = useState<File | null>(null);
  const [newImages, setNewImages] = useState<File[]>([]);
  // 수정 시 기존 자료(미리보기/유지 판단)
  const [existingThumbnailUrl, setExistingThumbnailUrl] = useState<string | undefined>();
  const [existingLogoUrl, setExistingLogoUrl] = useState<string | undefined>();
  const [keptImages, setKeptImages] = useState<EventImage[]>([]);
  const [removeThumbnail, setRemoveThumbnail] = useState(false);
  const [removeLogo, setRemoveLogo] = useState(false);
  const [busy, setBusy] = useState(false);

  const reload = useCallback(() => {
    api.listAdminEvents({ type: eventType, size: 100 })
      .then((d) => setItems(d.content))
      .catch(() => showToast(`${label}을(를) 불러오지 못했습니다.`));
    onChanged?.();
  }, [eventType, label, onChanged]);

  useEffect(() => { reload(); }, [reload]);

  const resetFiles = () => {
    setThumbnail(null); setLogo(null); setNewImages([]);
    setExistingThumbnailUrl(undefined); setExistingLogoUrl(undefined);
    setKeptImages([]); setRemoveThumbnail(false); setRemoveLogo(false);
  };

  const beginCreate = () => { resetFiles(); setDraft(emptyDraft()); };

  const beginEdit = async (id: number) => {
    try {
      const d = await api.getAdminEvent(id);
      resetFiles();
      setDraft({
        id: d.id, title: d.title, eventDateText: d.eventDateText, eventDate: d.eventDate ?? "",
        place: d.place, host: d.host, cardLabel: d.cardLabel, content: d.content,
        company: d.company ?? "", displayOrder: d.displayOrder != null ? String(d.displayOrder) : "",
        visible: d.visible,
      });
      setExistingThumbnailUrl(d.thumbnailImageUrl);
      setExistingLogoUrl(d.logoUrl);
      setKeptImages(d.images);
    } catch (error) {
      showToast(error instanceof Error ? error.message : "불러오지 못했습니다.");
    }
  };

  const removeKept = (id: number) => setKeptImages((cur) => cur.filter((img) => img.id !== id));

  const onPickGallery = (files: File[]) => {
    const room = MAX_GALLERY - keptImages.length - newImages.length;
    if (room <= 0) { showToast(`갤러리는 최대 ${MAX_GALLERY}장까지 가능합니다.`); return; }
    setNewImages((cur) => [...cur, ...files.slice(0, room)]);
  };

  const save = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!draft || busy) return;
    setBusy(true);
    try {
      const base = {
        eventType,
        title: draft.title.trim(),
        eventDateText: draft.eventDateText.trim(),
        eventDate: draft.eventDate || undefined,
        place: draft.place.trim(),
        host: draft.host.trim(),
        cardLabel: draft.cardLabel.trim(),
        content: draft.content.trim(),
        company: isCollab ? draft.company.trim() || undefined : undefined,
        visible: draft.visible,
        displayOrder: draft.displayOrder ? Number(draft.displayOrder) : undefined,
      };
      const files = {
        thumbnail: thumbnail ?? undefined,
        logo: isCollab ? logo ?? undefined : undefined,
        images: newImages,
      };
      if (draft.id === null) {
        await api.createEvent(base, files);
        showToast("등록되었습니다.");
      } else {
        await api.updateEvent(draft.id, {
          ...base,
          keepImageIds: keptImages.map((img) => img.id),
          removeThumbnail: removeThumbnail && !thumbnail,
          removeLogo: isCollab ? removeLogo && !logo : undefined,
        }, files);
        showToast("수정되었습니다.");
      }
      setDraft(null);
      reload();
    } catch (error) {
      showToast(error instanceof Error ? error.message : "저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: number) => {
    if (!confirm("삭제하시겠습니까?")) return;
    try {
      await api.deleteEvent(id);
      showToast("삭제되었습니다.");
      reload();
    } catch (error) {
      showToast(error instanceof Error ? error.message : "삭제에 실패했습니다.");
    }
  };

  const galleryCount = keptImages.length + newImages.length;

  return (
    <section className="event-feed-admin page-container">
      <div className="event-feed-admin__head">
        <strong>{label} 관리 <span className="event-feed-admin__count">({items.length})</span></strong>
        <button type="button" onClick={beginCreate}>글쓰기</button>
      </div>

      {draft && (
        <form className="event-feed-admin__form" onSubmit={save}>
          <input className="field__input" placeholder="제목" value={draft.title} onChange={(e) => setDraft({ ...draft, title: e.target.value })} required />
          {isCollab && (
            <input className="field__input" placeholder="회사·단체명" value={draft.company} onChange={(e) => setDraft({ ...draft, company: e.target.value })} />
          )}
          <input className="field__input" placeholder="일자 표시 예: 2027.01.15" value={draft.eventDateText} onChange={(e) => setDraft({ ...draft, eventDateText: e.target.value })} required />
          <input className="field__input" type="date" aria-label="정렬용 날짜(선택)" value={draft.eventDate} onChange={(e) => setDraft({ ...draft, eventDate: e.target.value })} />
          <input className="field__input" placeholder="장소" value={draft.place} onChange={(e) => setDraft({ ...draft, place: e.target.value })} required />
          <input className="field__input" placeholder="주최" value={draft.host} onChange={(e) => setDraft({ ...draft, host: e.target.value })} required />
          <input className="field__input" placeholder="발급 카드" value={draft.cardLabel} onChange={(e) => setDraft({ ...draft, cardLabel: e.target.value })} required />
          <input className="field__input" type="number" placeholder="정렬 순서(선택)" value={draft.displayOrder} onChange={(e) => setDraft({ ...draft, displayOrder: e.target.value })} />
          <label className="event-feed-admin__check">
            <input type="checkbox" checked={draft.visible} onChange={(e) => setDraft({ ...draft, visible: e.target.checked })} />
            <span>공개</span>
          </label>

          {/* 대표 썸네일 */}
          <label className="field__label">대표 이미지(썸네일)</label>
          {existingThumbnailUrl && !thumbnail && !removeThumbnail && (
            <div className="event-feed-admin__thumb">
              <img src={existingThumbnailUrl} alt="현재 썸네일" />
              <button type="button" onClick={() => setRemoveThumbnail(true)}>삭제</button>
            </div>
          )}
          <input className="field__input" type="file" accept="image/png,image/jpeg,image/webp" onChange={(e) => { setThumbnail(e.target.files?.[0] ?? null); setRemoveThumbnail(false); }} />

          {/* 협업 로고 */}
          {isCollab && (
            <>
              <label className="field__label">로고 이미지(협업)</label>
              {existingLogoUrl && !logo && !removeLogo && (
                <div className="event-feed-admin__thumb">
                  <img src={existingLogoUrl} alt="현재 로고" />
                  <button type="button" onClick={() => setRemoveLogo(true)}>삭제</button>
                </div>
              )}
              <input className="field__input" type="file" accept="image/png,image/jpeg,image/webp,image/svg+xml" onChange={(e) => { setLogo(e.target.files?.[0] ?? null); setRemoveLogo(false); }} />
            </>
          )}

          {/* 갤러리(다중) */}
          <label className="field__label">행사 이미지 갤러리 (최대 {MAX_GALLERY}장, 현재 {galleryCount}장)</label>
          {(keptImages.length > 0 || newImages.length > 0) && (
            <div className="event-feed-admin__gallery">
              {keptImages.map((img) => (
                <figure key={`kept-${img.id}`}><img src={img.url} alt={img.originalFileName} /><button type="button" onClick={() => removeKept(img.id)}>삭제</button></figure>
              ))}
              {newImages.map((file, i) => (
                <figure key={`new-${i}`}><img src={URL.createObjectURL(file)} alt={file.name} /><button type="button" onClick={() => setNewImages((cur) => cur.filter((_, idx) => idx !== i))}>삭제</button></figure>
              ))}
            </div>
          )}
          {galleryCount < MAX_GALLERY && (
            <input className="field__input" type="file" accept="image/png,image/jpeg,image/webp" multiple onChange={(e) => { onPickGallery(Array.from(e.target.files ?? [])); e.target.value = ""; }} />
          )}

          <textarea className="field__textarea" placeholder="내용" value={draft.content} onChange={(e) => setDraft({ ...draft, content: e.target.value })} required />
          <div className="event-feed-admin__actions">
            <button type="submit" disabled={busy}>저장</button>
            <button type="button" onClick={() => setDraft(null)}>취소</button>
          </div>
        </form>
      )}

      <div className="event-feed-admin__list">
        {items.length === 0 && <p className="event-feed-admin__empty">등록된 게시글이 없습니다.</p>}
        {items.map((item) => (
          <div className="event-feed-admin__row" key={item.id}>
            <span className="event-feed-admin__row-date">{item.eventDateText}</span>
            <b className="event-feed-admin__row-title">{item.title}</b>
            <span className="event-feed-admin__row-meta">{item.place}{item.host ? ` · ${item.host}` : ""}{!item.visible ? " · 비공개" : ""}</span>
            <button type="button" onClick={() => beginEdit(item.id)}>수정</button>
            <button type="button" onClick={() => remove(item.id)}>삭제</button>
          </div>
        ))}
      </div>
    </section>
  );
}
