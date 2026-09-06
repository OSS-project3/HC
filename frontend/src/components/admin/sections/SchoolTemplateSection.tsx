import { useEffect, useMemo, useState } from "react";
import { api, ApiError, type CardDesignOrientation, type SchoolCardTemplate, type SchoolOption } from "../../../services/api";
import { SearchableSelectField, type SearchableOption } from "../../ui/SearchableSelectField";
import { showToast } from "../../ui/toast";

export function SchoolTemplateSection() {
  const [schoolQuery, setSchoolQuery] = useState("");
  const [schools, setSchools] = useState<SchoolOption[]>([]);
  const [schoolId, setSchoolId] = useState("");
  const [orientation, setOrientation] = useState<CardDesignOrientation>("LANDSCAPE");
  const [template, setTemplate] = useState<SchoolCardTemplate | undefined>();
  const [front, setFront] = useState<File | null>(null);
  const [back, setBack] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const query = schoolQuery.trim();
    if (query.length < 2) {
      setSchools([]);
      return;
    }
    const handle = window.setTimeout(() => {
      api.searchSchools(query).then(setSchools).catch(() => setSchools([]));
    }, 250);
    return () => window.clearTimeout(handle);
  }, [schoolQuery]);

  const options: SearchableOption[] = useMemo(() => schools.map((school) => ({
    value: String(school.id),
    label: `${school.name} (${school.schoolType === "UNIVERSITY" ? "대학교" : "고등학교"})`,
    keywords: school.schoolType,
  })), [schools]);

  const loadTemplate = async () => {
    const id = Number(schoolId);
    if (!id) {
      setTemplate(undefined);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setTemplate(await api.getSchoolCardTemplate(id, orientation));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "템플릿을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void loadTemplate(); }, [schoolId, orientation]);

  const upload = async () => {
    const id = Number(schoolId);
    if (!id) { showToast("학교를 선택해 주세요."); return; }
    if (!front || !back) { showToast("앞면과 뒷면 PNG를 모두 선택해 주세요."); return; }
    setLoading(true);
    setError(null);
    try {
      const next = await api.uploadSchoolCardTemplate(id, orientation, front, back);
      setTemplate(next);
      setFront(null);
      setBack(null);
      showToast("학생증 템플릿을 등록했습니다.");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "템플릿 업로드에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="admin-panel">
      <div className="admin-panel__head">
        <div><p className="eyebrow">학생증</p><h2 className="admin-panel__title">학교별 카드 템플릿</h2></div>
      </div>

      <div className="admin-template__controls">
        <SearchableSelectField
          value={schoolId}
          onChange={setSchoolId}
          options={options}
          placeholder="학교 선택"
          searchPlaceholder="학교명을 입력해 주세요"
          ariaLabel="학교"
          onQueryChange={setSchoolQuery}
        />
        <select className="field__select" value={orientation} onChange={(e) => setOrientation(e.target.value as CardDesignOrientation)}>
          <option value="LANDSCAPE">가로형</option>
          <option value="PORTRAIT">세로형</option>
        </select>
      </div>

      {loading && <p className="admin-panel__note">처리 중...</p>}
      {error && <p className="admin-panel__note admin-panel__note--error">{error}</p>}

      <div className="admin-template__preview">
        {template ? (
          <>
            <img src={template.frontPreviewUrl} alt="학생증 앞면 템플릿" />
            <img src={template.backPreviewUrl} alt="학생증 뒷면 템플릿" />
          </>
        ) : (
          <p className="admin__empty">등록된 템플릿이 없습니다.</p>
        )}
      </div>

      <div className="admin-template__upload">
        <label className="field">
          <span className="field__label">앞면 PNG</span>
          <input className="field__input" type="file" accept="image/png" onChange={(e) => setFront(e.target.files?.[0] ?? null)} />
        </label>
        <label className="field">
          <span className="field__label">뒷면 PNG</span>
          <input className="field__input" type="file" accept="image/png" onChange={(e) => setBack(e.target.files?.[0] ?? null)} />
        </label>
        <button type="button" className="admin__btn admin__btn--primary" disabled={loading} onClick={upload}>등록/교체</button>
      </div>
    </div>
  );
}
