// Apply step: submission-complete confirmation screen.
import { useNavigate } from "react-router-dom";
import type { ApplicationDraft } from "../../../features/apply/types";
import { bankInfo } from "../../../config/company";
import { Button } from "../../ui/Button";
import { ChevronRight } from "../../ui/icons";
import { showToast } from "../../ui/toast";

interface StepCompleteProps {
  draft: ApplicationDraft;
  applicationNumber: string;
  onDone: () => void;
}

/** Completion copy differs by applicant type (personal vs organization). */
const completionMessage: Record<ApplicationDraft["applicantType"], string> = {
  personal: "담당자가 신청 내용을 확인한 후, 사전 상담 시 확정된 금액 기준으로 제작이 진행됩니다.",
  organization:
    "담당자가 단체 신청 내용과 제출 파일을 확인한 후, 사전 상담 시 확정된 금액 기준으로 제작이 진행됩니다.",
};

export function StepComplete({ draft, applicationNumber, onDone }: StepCompleteProps) {
  const navigate = useNavigate();
  const isOrg = draft.applicantType === "organization";

  const copyAccount = async () => {
    try {
      await navigator.clipboard.writeText(bankInfo.accountNumber);
      showToast("계좌번호가 복사되었습니다.");
    } catch {
      showToast("복사에 실패했습니다. 계좌번호를 직접 입력해 주세요.");
    }
  };

  return (
    <div className="step">
      <h2 className="step__heading">신청 완료</h2>
      <p className="step__desc">{completionMessage[draft.applicantType]}</p>
      <p className="complete__number">
        신청번호 <strong>{applicationNumber}</strong>
      </p>

      <section className="deposit">
        <h3 className="deposit__title">입금 안내</h3>
        <div className="deposit__table">
          <div className="deposit__row">
            <span className="deposit__key">은행</span>
            <span className="deposit__val">{bankInfo.bankName}</span>
          </div>
          <div className="deposit__row">
            <span className="deposit__key">계좌번호</span>
            <span className="deposit__val deposit__val--account">
              {bankInfo.accountNumber}
              <button type="button" className="deposit__copy" onClick={copyAccount}>
                복사
              </button>
            </span>
          </div>
          <div className="deposit__row">
            <span className="deposit__key">예금주</span>
            <span className="deposit__val">{bankInfo.accountHolder}</span>
          </div>
        </div>
      </section>

      <div className="field complete__depositor">
        <span className="field__label">입금자명 입력</span>
        <input className="field__input" placeholder="입금자명을 입력해 주세요" />
      </div>

      <div className="notice">
        <p className="notice__title">안내사항</p>
        <ul className="notice__list">
          <li>사전 상담을 통해 확정된 금액을 기준으로 입금해 주시기 바랍니다.</li>
          {/* The deposit-deadline line applies to individual applications only. */}
          {!isOrg && (
            <li>
              신청 후 영업일 3일 이내에 입금해 주시기 바랍니다. 기간 내 입금이 확인되지 않을 경우 신청이 취소될 수
              있습니다.
            </li>
          )}
          <li>입금자명과 신청자명이 다를 경우 입금 확인이 지연될 수 있습니다.</li>
          <li>입금 확인 후 제작이 진행됩니다.</li>
        </ul>
      </div>

      <div className="step__actions step__actions--end">
        <Button
          onClick={() => {
            onDone();
            navigate(`/lookup`);
          }}
        >
          신청 내역 확인하기 <ChevronRight width={16} height={16} />
        </Button>
      </div>
    </div>
  );
}
