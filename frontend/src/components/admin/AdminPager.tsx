// 관리자 목록 공용 페이지 이동 UI(§1.20) — 서버 Pageable 응답(totalPages)과 연결한다.
// totalPages가 1 이하면 아무것도 그리지 않는다.
export function AdminPager({ page, totalPages, onChange, disabled }: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
  disabled?: boolean;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="admin-pager">
      <button type="button" className="admin__btn" disabled={disabled || page <= 0} onClick={() => onChange(page - 1)}>
        ‹ 이전
      </button>
      <span className="admin-pager__label">{page + 1} / {totalPages}</span>
      <button type="button" className="admin__btn" disabled={disabled || page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        다음 ›
      </button>
    </div>
  );
}
