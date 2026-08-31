// Application flow shell: apply page, step type/files/review/complete, upload UI.
export const applyFlow: Record<string, string> = {
  // ApplyPage
  "명예 한국인증": "Honorary Korean ID",
  "명예 시민증": "Honorary Citizen ID",
  "제작 신청은 로그인 후 이용할 수 있습니다.": "Please sign in to apply for a card.",
  "로그인이 필요합니다": "Sign In Required",
  "신청자 정보와 제작 진행 내역을 안전하게 관리하기 위해 로그인한 회원만 제작 신청을 진행할 수 있습니다.":
    "To keep applicant details and production history secure, only signed-in members can submit an application.",
  "제작 신청은 실제 로그인 후 이용할 수 있습니다.": "Please sign in to your account to submit an application.",
  "로고와 제출 ZIP 파일을 다시 선택해 주세요.": "Please reselect the logo and submission ZIP file.",
  "직인 파일을 다시 선택해 주세요.": "Please reselect the official seal file.",
  "본인 사진을 다시 선택해 주세요.": "Please reselect your photo.",
  "학교 로고 파일을 선택해 주세요.": "Please select a school logo file.",
  "신청 API 호출에 실패했습니다.": "Failed to submit the application. Please try again.",
  "입금자명 저장에 실패했습니다.": "Failed to save the depositor name.",

  // Stepper
  "신청 단계": "Application steps",
  "유형 선택": "Select Type",
  "정보 입력": "Enter Details",
  "사진 / 파일 등록": "Upload Files",
  "최종 확인": "Final Review",
  "신청 완료": "Application Complete",

  // StepType
  "신청 유형 선택": "Choose Your Application Type",
  "개인 또는 법인·단체를 선택하면 신청이 시작됩니다.": "Select individual or corporate/group to begin your application.",
  "개인 신청": "Individual Application",
  "법인 · 단체 신청": "Corporate / Group Application",
  "ⓘ 안내사항": "ⓘ Please Note",
  "신청 유형(개인 / 법인·단체)에 따라 신청 양식이 구분되어 제공됩니다.":
    "A different application form is provided depending on the applicant type (individual or corporate/group).",
  "원활한 제작 진행을 위해 제작 신청 전 사전 상담을 완료해 주시기 바랍니다.":
    "To ensure smooth production, please complete a consultation before applying.",
  "위 안내사항을 확인하였으며, 사전 상담을 완료하였습니다.":
    "I have read the notice above and completed the prior consultation.",
  "본 증서는 문화체험용 기념 증서로 제공되며, 법적 신분증으로 사용할 수 없습니다.":
    "This card is a commemorative certificate for cultural experience and cannot be used as legal identification.",

  // StepFiles
  "법인·단체 신청": "Corporate / Group Application",
  "사진 및 파일 등록": "Upload Photos & Files",
  "학교 로고와 직인, 그리고 학생별 프로필 사진 및 사주 정보 파일을 등록해 주세요.":
    "Please upload the school logo and seal, along with each student's profile photo and Saju (birth chart) information file.",
  "학생증에 사용할 본인 프로필 사진과 학교 로고 및 직인을 등록해 주세요.":
    "Please upload your profile photo for the student ID, along with the school logo and seal.",
  "법인·단체 로고와 직인, 그리고 개인별 프로필 사진 및 사주 정보 파일을 등록해 주세요.":
    "Please upload your organization's logo and seal, along with each member's profile photo and Saju (birth chart) information file.",
  "카드에 사용할 본인 프로필 사진을 등록해 주세요.": "Please upload the profile photo to be used on your card.",
  "학교 로고": "School Logo",
  "학교 직인 (선택)": "School Seal (Optional)",
  "PNG, JPG 이미지": "PNG or JPG image",
  "첨부파일": "Attachment",
  "ZIP (학생별 프로필 사진과 사주 정보 파일)": "ZIP (each student's profile photo and Saju information file)",
  "본인 프로필 사진": "Your Profile Photo",
  "PNG, JPG 이미지 1개": "One PNG or JPG image",
  "법인·단체 로고 이미지": "Organization Logo Image",
  "법인·단체 직인 이미지": "Organization Seal Image",
  "ZIP (개인별 프로필 사진과 사주 정보 파일)": "ZIP (each member's profile photo and Saju information file)",
  "프로필 사진": "Profile Photo",

  // StepReview
  "학교명": "School Name",
  "법인·단체명": "Organization Name",
  "신청 정보": "Application Details",
  "신청 유형": "Application Type",
  "카드 종류": "Card Type",
  "발급 유형": "Issuance Type",
  "엑셀 인원 수로 자동 산정": "Calculated automatically from the Excel roster",
  "신청인 정보": "Applicant Information",
  "부서": "Department",
  "영문 이름": "Name in English",
  "국적": "Nationality",
  "생년월일": "Date of Birth",
  "출생시간": "Time of Birth",
  "모름": "Unknown",
  "성별": "Gender",
  "남성": "Male",
  "여성": "Female",
  "학번": "Student Number",
  "학과": "Major",
  "한국입국일": "Date of Entry into Korea",
  "수령인 정보": "Recipient Information",
  "수령인": "Recipient",
  "주소": "Address",
  "등록한 이미지 / 파일": "Uploaded Images & Files",
  "학교 직인": "School Seal",
  "신청 제출": "Submit Application",

  // StepComplete
  "담당자가 신청 내용을 확인한 후, 사전 상담 시 확정된 금액 기준으로 제작이 진행됩니다.":
    "Once our team reviews your application, production will proceed based on the amount confirmed during your consultation.",
  "담당자가 단체 신청 내용과 제출 파일을 확인한 후, 사전 상담 시 확정된 금액 기준으로 제작이 진행됩니다.":
    "Once our team reviews your group application and submitted files, production will proceed based on the amount confirmed during your consultation.",
  "신청번호": "Application No.",
  "입금 안내": "Payment Information",
  "은행": "Bank",
  "계좌번호": "Account Number",
  "복사": "Copy",
  "예금주": "Account Holder",
  "농협은행": "NH Nonghyup Bank",
  "입금자명 입력": "Depositor Name",
  "입금자명을 입력해 주세요": "Enter the depositor name",
  "안내사항": "Please Note",
  "사전 상담을 통해 확정된 금액을 기준으로 입금해 주시기 바랍니다.":
    "Please transfer the exact amount confirmed during your consultation.",
  "신청 후 영업일 3일 이내에 입금해 주시기 바랍니다. 기간 내 입금이 확인되지 않을 경우 신청이 취소될 수 있습니다.":
    "Please complete your payment within 3 business days of applying. If payment is not confirmed within this period, your application may be cancelled.",
  "입금자명과 신청자명이 다를 경우 입금 확인이 지연될 수 있습니다.":
    "If the depositor name differs from the applicant name, payment confirmation may be delayed.",
  "입금 확인 후 제작이 진행됩니다.": "Production begins once payment is confirmed.",
  "신청 내역 확인하기": "View My Application",
  "계좌번호가 복사되었습니다.": "Account number copied.",
  "복사에 실패했습니다. 계좌번호를 직접 입력해 주세요.": "Copy failed. Please enter the account number manually.",

  // FileUploadBox
  "클릭하여 업로드": "Click to upload",
  "교체": "Replace",

  // CardPreviewPanel
  "예시 카드": "Sample card",
  "※ 특허출원에 의한 견본품": "※ Patent-pending sample",
  "이해를 돕기 위한 예시 이미지 입니다": "These images are examples provided for illustration only.",
  "실제 발급의 디자인과 구성은 변경될 수 있습니다": "The actual card design and layout may differ.",
  "앞면": "Front",
  "뒷면": "Back",

  // Modal
  "이전 문서": "Previous document",
  "다음 문서": "Next document",

  // FlipCard
  "모바일 카드 앞면": "Mobile card front",
  "모바일 카드 뒷면": "Mobile card back",
  "카드 앞면 보기": "Show card front",
  "카드 뒷면 보기": "Show card back",

  // cardDownload
  "모바일 신분증": "Mobile ID Card",
  "이미지를 불러오지 못했습니다:": "Failed to load image:",
  "캔버스를 사용할 수 없습니다.": "Canvas is not available in this browser.",
  "이미지 생성에 실패했습니다.": "Failed to generate the image.",
};
