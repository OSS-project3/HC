export interface InquiryRecord {
  id: string;
  category: string;
  name: string;
  email: string;
  phone: string;
  title: string;
  content: string;
  createdAt: string;
  status: "PENDING" | "COMPLETED";
}

const STORAGE_KEY = "customer-inquiries";
const demoInquiries: InquiryRecord[] = [
  { id: "inquiry-demo-1", category: "카드 발급", name: "홍길동", email: "user@demo.com", phone: "010-1234-5678", title: "모바일 카드 발급 일정을 알고 싶습니다.", content: "제작 신청을 완료했습니다. 모바일 카드는 언제부터 확인할 수 있는지 안내 부탁드립니다.", createdAt: "2026-08-04T09:30:00.000Z", status: "PENDING" },
];

export function loadInquiries(): InquiryRecord[] {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (!saved) return demoInquiries;
    const parsed = JSON.parse(saved) as Array<Omit<InquiryRecord, "status"> & { status?: InquiryRecord["status"] }>;
    const normalized = parsed.map((inquiry) => ({ ...inquiry, status: inquiry.status ?? "PENDING" as const }));
    localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized));
    return normalized;
  } catch { return demoInquiries; }
}

export function saveInquiries(inquiries: InquiryRecord[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(inquiries));
}
