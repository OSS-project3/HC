// Apply step: review all entries before submitting.
import type { ApplicationDraft } from "../../../features/apply/types";
import type { CardDesign } from "../../../data/cards";
import { cardTypeLabels } from "../../../data/cards";
import { birthCitiesFor, formatUtcOffset } from "../../../data/birthCities";
import { countries } from "../../../data/countries";
import { Button } from "../../ui/Button";
import { ChevronLeft, ChevronRight } from "../../ui/icons";
import { useLanguage } from "../../../features/i18n/LanguageContext";

interface StepReviewProps {
  draft: ApplicationDraft;
  design?: CardDesign;
  onSubmit: () => void;
  onPrev: () => void;
  onEdit: (step: number) => void;
}

const dash = "—";

export function StepReview({ draft, design, onSubmit, onPrev, onEdit }: StepReviewProps) {
  const { t, language } = useLanguage();
  const isPhysical = draft.issuanceMethod === "mobile_and_physical";
  const isOrg = draft.applicantType === "organization";
  const isStudent = draft.cardType === "student";
  const orgLabel = t(isStudent ? "학교명" : "법인·단체명");
  const issuanceLabel = t(isPhysical ? "모바일 + 실물 발급" : "모바일 발급");
  const typeLabel = t(isOrg ? "법인·단체 신청" : "개인 신청");
  const cardLabel = design ? t(cardTypeLabels[design.cardType]) : draft.cardType ? t(cardTypeLabels[draft.cardType]) : dash;
  // 영어 모드에서는 국적(한국어 국가명)을 영문 국가명으로 바꿔 보여준다.
  const nationalityLabel = draft.applicant.nationality
    ? language === "en"
      ? countries.find((c) => c.ko === draft.applicant.nationality)?.en ?? draft.applicant.nationality
      : draft.applicant.nationality
    : dash;

  // 출생지역: 저장값(도시명)에 국적 기준 시차를 붙여 입력 화면과 동일하게 "서울 (UTC +9:00)"으로 표기.
  // 시차 데이터가 없는 국적(자유 입력 폴백)은 도시명만 노출한다.
  const birthCity = birthCitiesFor(draft.applicant.nationality ?? "").find(
    (city) => city.ko === draft.applicant.birthPlace,
  );
  const birthCityName = birthCity && language === "en" ? birthCity.en : draft.applicant.birthPlace;
  const birthPlaceLabel = draft.applicant.birthPlace
    ? birthCity
      ? `${birthCityName} (UTC ${formatUtcOffset(birthCity.offset)})`
      : draft.applicant.birthPlace
    : dash;

  return (
    <div className="step">
      <p className="step__eyebrow">{typeLabel}</p>
      <h2 className="step__heading">{t("최종 확인")}</h2>

      {/* Built from the same draft data — no re-typed values, so nothing is lost. */}
      {/* 디자인은 랜덤 배정이므로 최종 확인에 노출하지 않는다. */}
      <ReviewSection title="신청 정보" onEdit={() => onEdit(0)}>
        <Item label="신청 유형" value={typeLabel} />
        <Item label="카드 종류" value={cardLabel} />
        <Item label="발급 유형" value={issuanceLabel} />
        {isOrg && <Item label="수량" value={t("엑셀 인원 수로 자동 산정")} />}
      </ReviewSection>

      <ReviewSection title="신청인 정보" onEdit={() => onEdit(1)}>
        {isOrg ? (
          <>
            <Item label="이름" value={draft.applicant.name || dash} />
            <Item label={orgLabel} value={draft.applicant.organizationName || dash} />
            <Item label="부서" value={draft.applicant.department || dash} />
            <Item label="연락처" value={draft.applicant.phone || dash} />
            <Item label="이메일" value={draft.applicant.email || dash} />
          </>
        ) : (
          <>
            <Item label="영문 이름" value={draft.applicant.englishName || dash} />
            <Item label="국적" value={nationalityLabel} />
            <Item label="출생지역" value={birthPlaceLabel} />
            <Item label="생년월일" value={draft.applicant.birthDate || dash} />
            <Item
              label="출생시간"
              value={draft.applicant.birthTimeUnknown ? t("모름") : draft.applicant.birthTime || dash}
            />
            <Item
              label="성별"
              value={draft.applicant.gender === "male" ? t("남성") : draft.applicant.gender === "female" ? t("여성") : dash}
            />
            {isStudent &&
              ((draft.applicant.schoolLevel ?? "university") === "highschool" ? (
                <Item label="학교명" value={draft.applicant.schoolName || dash} />
              ) : (
                <>
                  <Item label="대학교명" value={draft.applicant.schoolName || dash} />
                  <Item label="학번" value={draft.applicant.studentNumber || dash} />
                  <Item label="학과" value={draft.applicant.department || dash} />
                </>
              ))}
            <Item label="한국입국일" value={draft.applicant.koreaEntryDate || dash} />
            <Item label="전화번호" value={draft.applicant.phone || dash} />
            <Item label="이메일" value={draft.applicant.email || dash} />
          </>
        )}
      </ReviewSection>

      {isPhysical && (
        <ReviewSection title="수령인 정보" onEdit={() => onEdit(1)}>
          <Item label="수령인" value={draft.recipient.name || dash} />
          {isOrg && <Item label={orgLabel} value={draft.recipient.organizationName || dash} />}
          {isOrg && <Item label="부서" value={draft.recipient.department || dash} />}
          <Item label="연락처" value={draft.recipient.phone || dash} />
          <Item
            label="주소"
            value={
              [
                draft.recipient.postalCode &&
                  (language === "en" ? `(ZIP ${draft.recipient.postalCode})` : `(우 ${draft.recipient.postalCode})`),
                draft.recipient.address,
                draft.recipient.addressDetail,
              ]
                .filter(Boolean)
                .join(" ") || dash
            }
          />
          <Item label="배송 요청사항" value={draft.recipient.deliveryRequest || dash} />
        </ReviewSection>
      )}

      <ReviewSection title="등록한 이미지 / 파일" onEdit={() => onEdit(2)}>
        {isStudent ? (
          isOrg ? (
            <>
              <FileItem label="학교 로고" name={draft.logoFile?.name} preview={draft.logoFile?.previewUrl} />
              <FileItem label="학교 직인" name={draft.sealFile?.name} preview={draft.sealFile?.previewUrl} />
              <FileItem label="첨부파일" name={draft.archiveFile?.name} />
            </>
          ) : (
            <>
              <FileItem label="본인 프로필 사진" name={draft.faceFile?.name} preview={draft.faceFile?.previewUrl} />
              <FileItem label="학교 로고" name={draft.logoFile?.name} preview={draft.logoFile?.previewUrl} />
              <FileItem label="학교 직인" name={draft.sealFile?.name} preview={draft.sealFile?.previewUrl} />
            </>
          )
        ) : isOrg ? (
          <>
            <FileItem label="법인·단체 로고 이미지" name={draft.logoFile?.name} preview={draft.logoFile?.previewUrl} />
            <FileItem label="법인·단체 직인 이미지" name={draft.sealFile?.name} preview={draft.sealFile?.previewUrl} />
            <FileItem label="첨부파일" name={draft.archiveFile?.name} />
          </>
        ) : (
          <FileItem label="프로필 사진" name={draft.faceFile?.name} preview={draft.faceFile?.previewUrl} />
        )}
      </ReviewSection>

      <div className="step__actions">
        <Button variant="soft" onClick={onPrev}>
          <ChevronLeft width={16} height={16} /> {t("이전")}
        </Button>
        <Button onClick={onSubmit}>
          {t("신청 제출")} <ChevronRight width={16} height={16} />
        </Button>
      </div>
    </div>
  );
}

function ReviewSection({
  title,
  onEdit,
  children,
}: {
  title: string;
  onEdit: () => void;
  children: React.ReactNode;
}) {
  const { t } = useLanguage();
  return (
    <section className="review">
      <div className="review__head">
        <h3 className="review__title">{t(title)}</h3>
        <button type="button" className="review__edit" onClick={onEdit}>
          {t("수정")} <span aria-hidden="true">›</span>
        </button>
      </div>
      <dl className="review__grid">{children}</dl>
    </section>
  );
}

function Item({ label, value }: { label: string; value: string }) {
  const { t } = useLanguage();
  return (
    <div className="review__item">
      <dt>{t(label)}</dt>
      <dd>{value}</dd>
    </div>
  );
}

function FileItem({ label, name, preview }: { label: string; name?: string; preview?: string }) {
  const { t } = useLanguage();
  return (
    <div className="review__item review__item--file">
      <dt>{t(label)}</dt>
      <dd>
        {name ? (
          <span className="review__file">
            {preview && <img src={preview} alt="" className="review__thumb" />}
            {name}
          </span>
        ) : (
          "—"
        )}
      </dd>
    </div>
  );
}
