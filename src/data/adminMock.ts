/** Mock application records for the admin page (demo only). */
export type AdminStatus =
  | "SUBMITTED"
  | "CONSULTING"
  | "PAYMENT_PENDING"
  | "IN_PRODUCTION"
  | "COMPLETED"
  | "CANCELLED";

export interface AdminApplication {
  applicationNumber: string;
  applicantType: "개인" | "법인·단체";
  cardType: string;
  applicantName: string;
  phone: string;
  quantity: number;
  status: AdminStatus;
  submittedAt: string;
}

export const adminStatusLabels: Record<AdminStatus, string> = {
  SUBMITTED: "접수 완료",
  CONSULTING: "상담 중",
  PAYMENT_PENDING: "입금 대기",
  IN_PRODUCTION: "제작 중",
  COMPLETED: "발급 완료",
  CANCELLED: "취소",
};

export const adminApplications: AdminApplication[] = [
  {
    applicationNumber: "APP-2026-000131",
    applicantType: "법인·단체",
    cardType: "명예한국인증",
    applicantName: "홍길동",
    phone: "010-1234-5678",
    quantity: 100,
    status: "IN_PRODUCTION",
    submittedAt: "2026-07-18",
  },
  {
    applicationNumber: "APP-2026-000130",
    applicantType: "개인",
    cardType: "학생증",
    applicantName: "이소연",
    phone: "010-2222-3333",
    quantity: 1,
    status: "PAYMENT_PENDING",
    submittedAt: "2026-07-17",
  },
  {
    applicationNumber: "APP-2026-000129",
    applicantType: "개인",
    cardType: "방문증",
    applicantName: "최인서",
    phone: "010-4444-5555",
    quantity: 2,
    status: "SUBMITTED",
    submittedAt: "2026-07-16",
  },
  {
    applicationNumber: "APP-2026-000128",
    applicantType: "법인·단체",
    cardType: "명예 시민증",
    applicantName: "정은성",
    phone: "010-6666-7777",
    quantity: 40,
    status: "COMPLETED",
    submittedAt: "2026-07-14",
  },
  {
    applicationNumber: "APP-2026-000127",
    applicantType: "개인",
    cardType: "명예한국인증",
    applicantName: "윤은재",
    phone: "010-8888-9999",
    quantity: 1,
    status: "CONSULTING",
    submittedAt: "2026-07-13",
  },
  {
    applicationNumber: "APP-2026-000126",
    applicantType: "개인",
    cardType: "학생증",
    applicantName: "박지호",
    phone: "010-1010-2020",
    quantity: 1,
    status: "CANCELLED",
    submittedAt: "2026-07-11",
  },
];

export const adminStats = [
  { label: "전체 신청", value: adminApplications.length },
  { label: "제작 중", value: adminApplications.filter((a) => a.status === "IN_PRODUCTION").length },
  { label: "입금 대기", value: adminApplications.filter((a) => a.status === "PAYMENT_PENDING").length },
  { label: "발급 완료", value: adminApplications.filter((a) => a.status === "COMPLETED").length },
];
