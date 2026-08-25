export interface TailorResumeRequest {
  resumePdfBase64: string;
  jobDescription: string;
  includeCoverLetter?: boolean;
}

export interface TailorResumeResult {
  pdfBase64: string;
  status: string;
  coverLetterPdfBase64?: string | null;
  atsScore?: number;
  extractedRequirements?: string[];
}

export interface TailorResumeApiResponse {
  result: TailorResumeResult;
  storageKey?: string;
}

export interface SendEmailRequest {
  email: string;
  pdfBase64?: string | null;
  coverLetterPdfBase64?: string | null;
  jobUrl?: string | null;
}

export interface AnalyticsResponse {
  resume_tailored: number;
  website_visit: number;
}

export interface MasterResumeResponse {
  s3Key: string;
  resumePdfBase64: string;
}
