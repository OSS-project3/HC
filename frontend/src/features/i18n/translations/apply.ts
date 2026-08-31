// Application flow (제작 신청) strings: steps, validation, file upload.
export const apply: Record<string, string> = {
  // Step headings and issuance options (StepInfo)
  "개인 신청": "Individual Application",
  "발급 유형 선택": "Issuance Type",
  "모바일 발급": "Mobile card only",
  "모바일 + 실물 발급": "Mobile + physical card",
  "카드 방향": "Card Orientation",
  "가로형": "Landscape",
  "세로형": "Portrait",

  // Applicant section
  "이름": "Name",
  "담당자 이름": "Contact person's name",
  "영문 이름": "Name in English",
  "법인·단체명": "Organization Name",
  "법인·단체명을 입력해 주세요": "Enter the organization name",
  "부서": "Department",
  "부서 (선택)": "Department (optional)",
  "연락처": "Phone Number",
  "전화번호": "Phone Number",
  "이메일": "Email",
  "올바른 연락처 형식으로 입력해 주세요. (예: 010-1234-5678)":
    "Please enter a valid phone number (e.g. 010-1234-5678).",
  "올바른 전화번호 형식으로 입력해 주세요. (예: 010-1234-5678)":
    "Please enter a valid phone number (e.g. 010-1234-5678).",
  "올바른 이메일 형식으로 입력해 주세요.": "Please enter a valid email address.",
  "입력하신 연락처와 이메일로 발급된 모바일 카드를 조회할 수 있습니다.":
    "You can look up your issued mobile card using the phone number and email you provide.",

  // Nationality / birth details (used for the Saju-based Korean naming)
  "국적": "Nationality",
  "국적 선택": "Select nationality",
  "국적을 선택해 주세요": "Select your nationality",
  "국가명을 입력해 주세요": "Type a country name",
  "출생지역": "Place of Birth",
  "국적을 먼저 선택해 주세요": "Select your nationality first",
  "출생지역 선택": "Select place of birth",
  "출생 도시를 선택해 주세요": "Select your city of birth",
  "도시명을 입력해 주세요": "Type a city name",
  "출생 도시를 입력해 주세요": "Enter your city of birth",
  "생년월일": "Date of Birth",
  "출생시간": "Time of Birth",
  "출생시간을 모릅니다": "I don't know my time of birth",
  "성별": "Gender",
  "성별 선택": "Select gender",
  "성별을 선택해 주세요": "Select your gender",
  "남성": "Male",
  "여성": "Female",
  "한국입국일": "Date of Entry into Korea",

  // Student ID school fields
  "학교": "School",
  "학교명": "School Name",
  "학교 선택": "Select school",
  "학교 구분": "School Type",
  "대학교": "University",
  "고등학교": "High school",
  "대학교명": "University Name",
  "대학교명을 입력해 주세요": "Enter the university name",
  "학교명을 검색해 주세요": "Search for your school",
  "학교명을 입력해 주세요": "Enter the school name",
  "찾는 학교가 없나요? 직접 입력": "Can't find your school? Enter it manually",
  "목록에서 학교 찾기": "Choose from the school list instead",
  "학번": "Student Number",
  "학과": "Major",
  "학과를 입력해 주세요": "Enter your major",

  // Recipient / shipping section
  "신청인과 동일합니다": "Same as applicant",
  "학교명 (선택)": "School name (optional)",
  "법인·단체명 (선택)": "Organization name (optional)",
  "배송지 주소": "Shipping Address",
  "우편번호": "Postal code",
  "우편번호 찾기": "Find Postal Code",
  "기본 주소": "Street address",
  "상세 주소를 입력해 주세요": "Enter the rest of your address (unit, floor, etc.)",
  "배송 요청사항": "Delivery Instructions",
  "* 필수 입력 항목": "* Required fields",
  "주소 검색 서비스를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.":
    "We couldn't load the address search service. Please try again in a moment.",
  "한글과 세종 우편번호 검색": "Hangul & Sejong Postal Code Search",

  // Shared select components
  "검색어를 입력해 주세요": "Type to search",
  "검색 결과가 없습니다.": "No results found.",
};
