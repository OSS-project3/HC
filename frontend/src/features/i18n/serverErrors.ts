// English messages for backend ErrorCode values (see backend ErrorCode.java).
// Keyed by errorCode from the API envelope so translations survive backend
// message wording changes; unknown codes fall back to the server message.
export const serverErrorMessages: Record<string, string> = {
  // Common
  INVALID_INPUT: "The input failed validation.",
  UNAUTHORIZED: "Sign-in required.",
  FORBIDDEN: "You don't have permission to do this.",
  NOT_FOUND: "The requested data could not be found.",
  INTERNAL_ERROR: "An internal server error occurred.",
  // Auth
  TERMS_NOT_AGREED: "Please agree to the terms first.",
  INVALID_REFRESH_TOKEN: "Your session is invalid. Please sign in again.",
  REFRESH_TOKEN_REUSE_DETECTED: "Suspicious session activity was detected, so all sessions were signed out.",
  TERMS_ALREADY_AGREED: "You have already agreed to the terms.",
  USER_NOT_FOUND: "No such user exists.",
  EMAIL_ALREADY_EXISTS: "This email is already registered.",
  EMAIL_DELIVERY_FAILED: "Failed to send the email.",
  TOO_MANY_REQUESTS: "Too many requests. Please try again shortly.",
  INVALID_VERIFICATION_CODE: "The verification code is incorrect or has expired.",
  INVALID_SIGNUP_TOKEN: "Your signup verification has expired or is invalid. Please verify your email again.",
  ACCOUNT_LOCKED: "Too many sign-in attempts — the account is temporarily locked. Please try again later.",
  INVALID_CREDENTIALS: "The email or password is incorrect.",
  CURRENT_PASSWORD_MISMATCH: "The current password does not match.",
  PASSWORD_CHANGE_NOT_ALLOWED: "OAuth accounts cannot change their password.",
  AUTH_SESSION_VALIDATION_UNAVAILABLE: "Session verification is temporarily unavailable. Please try again shortly.",
  // Application
  APPLICATION_NOT_FOUND: "No such application exists.",
  INVALID_STATUS_TRANSITION: "This status change is not allowed.",
  APPLICATION_LIMIT_EXCEEDED: "You have exceeded the daily application limit (3 per day).",
  NAMING_INCOMPLETE: "Naming has not been completed for all members.",
  CARD_NUMBER_LOCKED: "The card has already been produced, so its number can no longer be changed.",
  CARD_NUMBER_VALIDATION_FAILED: "The card number failed validation.",
  CARD_NUMBER_ALREADY_USED: "This card number is already in use by another card.",
  APPLICATION_VERSION_CONFLICT: "Another admin has modified this first. Please refresh and try again.",
  GEOCODING_NOT_CONFIGURED: "Birthplace lookup is not configured yet.",
  GEOCODING_PROVIDER_ERROR: "An error occurred while looking up the birthplace. Please try again shortly.",
  REGION_NOT_FOUND: "No matching birthplace was found.",
  // Upload
  FILE_TOO_LARGE: "Files cannot exceed 10MB.",
  UNSUPPORTED_FILE_TYPE: "This file type is not allowed.",
  INVALID_IMAGE: "A face could not be detected in the photo.",
  INVALID_IMAGE_FILE: "The image file is corrupted or in an unsupported format.",
  // Bulk application
  INVALID_ZIP: "The ZIP file format is invalid.",
  BULK_APPLICATION_VALIDATION_FAILED: "The group application failed validation.",
  // Card
  CARD_NOT_READY: "The card has not been issued yet.",
  CARD_TYPE_NOT_FOUND: "No such card type exists.",
  UNSUPPORTED_CARD_TYPE: "Student IDs are not supported by this feature.",
  CARD_DESIGN_NOT_FOUND: "No such card design exists.",
  CARD_DESIGN_MISMATCH: "This design does not match the application's card type or is disabled.",
  CARD_ISSUE_DATE_OUT_OF_RANGE: "The issue date must be after the application date and within 3 months of it.",
  MANSERYEOK_NOT_CONFIRMED: "The Saju chart has not been confirmed, so the zodiac image cannot be determined.",
  CARD_ISSUER_ASSETS_MISSING: "Group applications require both a logo and a seal image.",
  // Review
  REVIEW_NOT_FOUND: "No such review exists.",
  REVIEW_NOT_ELIGIBLE: "You have no application history for the selected type and card.",
  REVIEW_ALREADY_EXISTS: "You have already written a review for this application type and card.",
  // Board / Event / Inquiry
  BOARD_NOT_FOUND: "No such post exists.",
  EVENT_NOT_FOUND: "No such event exists.",
  INQUIRY_NOT_FOUND: "No such inquiry exists.",
};

/** Pick the message to show for an API error in the current language. */
export function resolveServerErrorMessage(
  language: string,
  errorCode: string | null | undefined,
  serverMessage: string | null | undefined,
  fallback: string,
): string {
  if (language === "en" && errorCode && serverErrorMessages[errorCode]) return serverErrorMessages[errorCode];
  return serverMessage || fallback;
}
