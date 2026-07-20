import clsx from "clsx";
import { STEP_LABELS } from "../../features/apply/types";

interface StepperProps {
  current: number; // 0-based
}

export function Stepper({ current }: StepperProps) {
  return (
    <ol className="stepper" aria-label="신청 단계">
      {STEP_LABELS.map((label, i) => (
        <li
          key={label}
          className={clsx("stepper__item", i === current && "stepper__item--active", i < current && "stepper__item--done")}
          aria-current={i === current ? "step" : undefined}
        >
          <span className="stepper__dot">{i + 1}</span>
          <span className="stepper__label">{label}</span>
        </li>
      ))}
    </ol>
  );
}
