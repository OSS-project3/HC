// Apply step: choose card type and issuance method.
import clsx from "clsx";
import type { ApplicantType, ApplicationDraft } from "../../../features/apply/types";
import { PersonIcon, GroupIcon, CheckIcon, ChevronRight } from "../../ui/icons";
import { Button } from "../../ui/Button";

interface StepTypeProps {
  draft: ApplicationDraft;
  update: (patch: Partial<ApplicationDraft>) => void;
  onNext: () => void;
}

const options: { type: ApplicantType; label: string; icon: React.ReactNode }[] = [
  { type: "personal", label: "개인 신청", icon: <PersonIcon width={26} height={26} /> },
  { type: "organization", label: "법인 · 단체 신청", icon: <GroupIcon width={26} height={26} /> },
];

export function StepType({ draft, update, onNext }: StepTypeProps) {
  return (
    <div className="step">
      {/* NOTE: the "사주 명리학 기반 이름 정보 입력" subtitle was removed per the
          client's request — it does not apply here. */}
      <h2 className="step__heading">신청 유형 선택</h2>
      <p className="step__desc">개인 또는 법인·단체를 선택하면 신청이 시작됩니다.</p>

      <div className="type-cards">
        {options.map((opt) => {
          const selected = draft.applicantType === opt.type;
          return (
            <button
              key={opt.type}
              type="button"
              className={clsx("type-card", selected && "type-card--selected")}
              aria-pressed={selected}
              onClick={() => update({ applicantType: opt.type })}
            >
              {selected && (
                <span className="type-card__check" aria-hidden="true">
                  <CheckIcon width={15} height={15} />
                </span>
              )}
              <span className="type-card__icon">{opt.icon}</span>
              <span className="type-card__label">{opt.label}</span>
              <ChevronRight className="type-card__arrow" width={20} height={20} />
            </button>
          );
        })}
      </div>

      <div className="notice">
        <p className="notice__title">ⓘ 안내사항</p>
        <ul className="notice__list">
          <li>신청 유형(개인 / 법인 단체)에 따라 신청 양식이 구분되어 제공됩니다.</li>
          <li>원활한 제작 진행을 위해 제작 신청 전 사전 상담을 완료해 주시기 바랍니다.</li>
        </ul>
        <label className="check notice__agree">
          <input
            type="checkbox"
            checked={draft.consultationConfirmed}
            onChange={(e) => update({ consultationConfirmed: e.target.checked })}
          />
          <span>위 안내사항을 확인하였으며, 사전 상담을 완료하였습니다.</span>
        </label>
      </div>

      <div className="step__actions step__actions--end">
        <Button onClick={onNext} disabled={!draft.consultationConfirmed}>
          다음 <ChevronRight width={16} height={16} />
        </Button>
      </div>
    </div>
  );
}
