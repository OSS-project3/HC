// Apply step: review all entries before submitting.
import type { ApplicationDraft } from "../../../features/apply/types";
import type { CardDesign } from "../../../data/cards";
import { cardTypeLabels } from "../../../data/cards";
import { Button } from "../../ui/Button";
import { ChevronLeft, ChevronRight } from "../../ui/icons";

interface StepReviewProps {
  draft: ApplicationDraft;
  design?: CardDesign;
  onSubmit: () => void;
  onPrev: () => void;
  onEdit: (step: number) => void;
}

const dash = "—";

export function StepReview({ draft, design, onSubmit, onPrev, onEdit }: StepReviewProps) {
  const isPhysical = draft.issuanceMethod === "mobile_and_physical";
  const isVisitor = draft.cardType === "visitor";
  const isStudent = draft.cardType === "student";
  const issuanceLabel = isPhysical ? "모바일 + 실물 발급" : "모바일 발급";
  const typeLabel = draft.applicantType === "organization" ? "법인 단체 신청" : "개인 신청";
  const cardLabel = design ? cardTypeLabels[design.cardType] : draft.cardType ? cardTypeLabels[draft.cardType] : dash;

  return (
    <div className="step">
      <h2 className="step__heading">최종 확인</h2>

      {/* Built from the same draft data — no re-typed values, so nothing is lost. */}
      <ReviewSection title="신청 정보" onEdit={() => onEdit(0)}>
        <Item label="신청 유형" value={typeLabel} />
        <Item label="카드 종류" value={cardLabel} />
        {design && <Item label="디자인" value={design.name} />}
        <Item label="발급 유형" value={issuanceLabel} />
        <Item label="수량" value={`${draft.quantity}매`} />
      </ReviewSection>

      <ReviewSection title="신청인 정보" onEdit={() => onEdit(1)}>
        {isVisitor ? (
          <>
            <Item label="영문 이름" value={draft.applicant.englishName || dash} />
            <Item label="국적" value={draft.applicant.nationality || dash} />
            <Item label="출생지역" value={draft.applicant.birthPlace || dash} />
            <Item label="생년월일" value={draft.applicant.birthDate || dash} />
            <Item
              label="출생시간"
              value={draft.applicant.birthTimeUnknown ? "모름" : draft.applicant.birthTime || dash}
            />
            <Item
              label="성별"
              value={draft.applicant.gender === "male" ? "남성" : draft.applicant.gender === "female" ? "여성" : dash}
            />
            <Item label="전화번호" value={draft.applicant.phone || dash} />
          </>
        ) : (
          <>
            <Item label="이름" value={draft.applicant.name || dash} />
            {isStudent && <Item label="학번" value={draft.applicant.studentNumber || dash} />}
            {isStudent && <Item label="학과" value={draft.applicant.department || dash} />}
            <Item label="연락처" value={draft.applicant.phone || dash} />
            <Item label="이메일" value={draft.applicant.email || dash} />
          </>
        )}
      </ReviewSection>

      {isPhysical && (
        <ReviewSection title="수령인 정보" onEdit={() => onEdit(1)}>
          <Item label="수령인" value={draft.recipient.name || dash} />
          <Item label="연락처" value={draft.recipient.phone || dash} />
          <Item
            label="주소"
            value={
              [draft.recipient.postalCode && `(우 ${draft.recipient.postalCode})`, draft.recipient.address, draft.recipient.addressDetail]
                .filter(Boolean)
                .join(" ") || dash
            }
          />
          <Item label="배송 요청사항" value={draft.recipient.deliveryRequest || dash} />
        </ReviewSection>
      )}

      <ReviewSection title="등록한 이미지 / 파일" onEdit={() => onEdit(2)}>
        {isVisitor ? (
          <FileItem label="본인 얼굴 사진" name={draft.faceFile?.name} preview={draft.faceFile?.previewUrl} />
        ) : isStudent ? (
          <>
            <FileItem label="본인 프로필 사진" name={draft.faceFile?.name} preview={draft.faceFile?.previewUrl} />
            <FileItem label="학교 로고" name={draft.logoFile?.name} preview={draft.logoFile?.previewUrl} />
            <FileItem label="학교 직인" name={draft.sealFile?.name} preview={draft.sealFile?.previewUrl} />
          </>
        ) : (
          <>
            <FileItem label="로고 이미지" name={draft.logoFile?.name} preview={draft.logoFile?.previewUrl} />
            <FileItem label="직인 이미지" name={draft.sealFile?.name} preview={draft.sealFile?.previewUrl} />
            <FileItem label="제출 파일" name={draft.archiveFile?.name} />
          </>
        )}
      </ReviewSection>

      <div className="step__actions">
        <Button variant="soft" onClick={onPrev}>
          <ChevronLeft width={16} height={16} /> 이전
        </Button>
        <Button onClick={onSubmit}>
          신청 제출 <ChevronRight width={16} height={16} />
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
  return (
    <section className="review">
      <div className="review__head">
        <h3 className="review__title">{title}</h3>
        <button type="button" className="review__edit" onClick={onEdit}>
          수정
        </button>
      </div>
      <dl className="review__grid">{children}</dl>
    </section>
  );
}

function Item({ label, value }: { label: string; value: string }) {
  return (
    <div className="review__item">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}

function FileItem({ label, name, preview }: { label: string; name?: string; preview?: string }) {
  return (
    <div className="review__item review__item--file">
      <dt>{label}</dt>
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
