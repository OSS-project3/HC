import { useEffect, useMemo, useState } from "react";
import { api, type SchoolOption } from "../../services/api";
import type { ApplicantInfo, ApplicationDraft } from "./types";
export function useSchoolSearch(draft: ApplicationDraft, setApplicant: (patch: Partial<ApplicantInfo>) => void) {
 const isStudent = draft.cardType === "student";
  // 학교 검색select — 대학교+고등학교를 합치면 학교 수가 약 2,800개라 더 이상 전체 목록을 한 번에
  // 받지 않는다(백엔드 SchoolService 참고). 대신 검색어를 200~300ms debounce해 서버에 넘기고, 결과를
  // 그대로 옵션으로 쓴다(서버가 이미 최대 20건으로 제한 + 관련도순 정렬해 반환). 직접입력으로 채워진
  // 기존 draft(schoolName은 있고 schoolId는 없음)를 복원한 경우 직접입력 모드로 시작한다.
  //
  // 이미 학교가 선택된 draft를 복원할 때(schoolId 있음)는 그 학교를 initial 옵션으로 미리 채워둔다 —
  // 그래야 사용자가 아무것도 타이핑하지 않아도 선택select 트리거에 기존 학교명이 그대로 보인다(별도
  // API 호출 없이 draft가 이미 들고 있는 값으로 구성).
  const [schoolOptions, setSchoolOptions] = useState<SchoolOption[]>(() =>
    draft.applicant.schoolId != null && draft.applicant.schoolName
      ? [
          {
            id: draft.applicant.schoolId,
            name: draft.applicant.schoolName,
            schoolType: draft.applicant.schoolLevel === "highschool" ? "HIGH_SCHOOL" : "UNIVERSITY",
          },
        ]
      : [],
  );
  const [schoolQuery, setSchoolQuery] = useState("");
  const [manualSchoolInput, setManualSchoolInput] = useState(
    () => !!draft.applicant.schoolName && draft.applicant.schoolId == null,
  );

  useEffect(() => {
    if (!isStudent) return;
    const query = schoolQuery.trim();
    // 검색어가 비었으면(패널을 처음 열었을 때 등) 서버를 부르지 않는다 — 기존 옵션(복원된 선택 포함)을
    // 그대로 둔다. 서버도 빈 검색어에는 빈 목록을 돌려주므로 어차피 호출할 이유가 없다.
    if (!query) return;
    let cancelled = false;
    const timer = setTimeout(() => {
      api.searchSchools(query).then((options) => {
        if (!cancelled) setSchoolOptions(options);
      }).catch(() => {
        // 검색 실패해도 직접입력으로 계속 진행할 수 있으므로 별도 에러 처리는 하지 않는다.
      });
    }, 250);
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [isStudent, schoolQuery]);

  const schoolSelectOptions = useMemo(
    () =>
      schoolOptions.map((school) => ({
        value: String(school.id),
        label: school.name,
        keywords: school.schoolType === "UNIVERSITY" ? "대학교 University" : "고등학교 High School",
      })),
    [schoolOptions],
  );

  const selectSchool = (value: string) => {
    const school = schoolOptions.find((option) => String(option.id) === value);
    if (!school) return;
    const isUniversity = school.schoolType === "UNIVERSITY";
    setApplicant({
      schoolId: school.id,
      schoolName: school.name,
      // 단체 신청 화면의 "학교명"(orgLabel)은 organizationName을 그대로 쓰는 기존 필드라, 검색select로
      // 골라도 같은 값을 여기에도 채운다(개인 신청에서는 이 필드를 아무도 읽지 않아 영향 없음).
      organizationName: school.name,
      schoolLevel: isUniversity ? "university" : "highschool",
      ...(isUniversity ? {} : { studentNumber: "", department: "" }),
    });
  };

 return { manualSchoolInput, setManualSchoolInput, selectSchool, setSchoolQuery, schoolSelectOptions };
}
