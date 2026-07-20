/**
 * Policy documents shown in the footer modal.
 * Stored as data (not JSX) so copy can be edited in one place. Content is
 * abbreviated placeholder text — replace with the final legal copy.
 */
export type PolicyType = "privacy" | "terms" | "email" | "sitemap";

export interface PolicyDocument {
  type: PolicyType;
  title: string;
  paragraphs: string[];
}

export const policyDocuments: Record<Exclude<PolicyType, "sitemap">, PolicyDocument> = {
  privacy: {
    type: "privacy",
    title: "개인정보처리방침",
    paragraphs: [
      "(사)한글과 세종(이하 '회사')은 이용자의 개인정보를 중요시하며, 「개인정보 보호법」 등 관련 법령을 준수합니다.",
      "제1조 (수집하는 개인정보 항목) 회사는 제작 신청 및 문의 처리를 위하여 이름, 연락처, 이메일, 주소, 제출 파일 등의 개인정보를 수집합니다.",
      "제2조 (개인정보의 수집 및 이용 목적) 수집한 개인정보는 카드 제작·발급, 배송, 상담 및 고객 문의 응대의 목적으로만 이용됩니다.",
      "제3조 (개인정보의 보유 및 이용 기간) 개인정보는 수집·이용 목적이 달성된 후 지체 없이 파기하며, 관계 법령에 따라 보존할 필요가 있는 경우 해당 기간 동안 보관합니다.",
      "제4조 (개인정보의 제3자 제공) 회사는 이용자의 동의 없이 개인정보를 외부에 제공하지 않습니다.",
      "본 방침은 예시 문구이며 실제 배포 전 최종 검토가 필요합니다.",
    ],
  },
  terms: {
    type: "terms",
    title: "이용약관",
    paragraphs: [
      "제1조 (목적) 본 약관은 (사)한글과 세종이 제공하는 서비스의 이용 조건 및 절차에 관한 사항을 규정함을 목적으로 합니다.",
      "제2조 (용어의 정의) '서비스'라 함은 회사가 제공하는 한국 이름 작명 및 각종 증(證) 제작·발급 관련 일체의 서비스를 말합니다.",
      "제3조 (약관의 효력 및 변경) 본 약관은 서비스를 이용하고자 하는 모든 이용자에게 효력이 발생합니다.",
      "제4조 (서비스의 제공) 회사는 안정적인 서비스 제공을 위하여 노력하며, 필요한 경우 서비스의 내용을 변경할 수 있습니다.",
      "본 약관은 예시 문구이며 실제 배포 전 최종 검토가 필요합니다.",
    ],
  },
  email: {
    type: "email",
    title: "이메일무단수집거부",
    paragraphs: [
      "본 웹사이트에 게시된 이메일 주소가 전자우편 수집 프로그램이나 그 밖의 기술적 장치를 이용하여 무단으로 수집되는 것을 거부합니다.",
      "이를 위반 시 「정보통신망 이용촉진 및 정보보호 등에 관한 법률」에 의해 형사 처벌됨을 유의하시기 바랍니다.",
      "게시일: 2026년",
    ],
  },
};
