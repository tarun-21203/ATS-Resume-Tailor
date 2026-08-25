import { apiClient } from "./client";
import type {
  AnalyticsResponse,
  MasterResumeResponse,
  SendEmailRequest,
  TailorResumeApiResponse,
  TailorResumeRequest,
} from "../types/api";

export async function tailorResume(payload: TailorResumeRequest): Promise<TailorResumeApiResponse> {
  const { data } = await apiClient.post<TailorResumeApiResponse>("/tailorResume", payload);
  return data;
}

export async function sendResumeEmail(payload: SendEmailRequest): Promise<void> {
  await apiClient.post("/sendEmail", payload);
}

export async function trackMetric(metricType: string): Promise<void> {
  await apiClient.post("/track", { metricType });
}

export async function getAnalytics(): Promise<AnalyticsResponse> {
  const { data } = await apiClient.get<AnalyticsResponse>("/analytics");
  return data;
}

export async function getMasterResume(userId: string): Promise<MasterResumeResponse> {
  const { data } = await apiClient.get<MasterResumeResponse>("/masterResume", { headers: { "x-user-id": userId } });
  return data;
}

export async function putMasterResume(userId: string, resumePdfBase64: string): Promise<void> {
  await apiClient.put("/masterResume", { resumePdfBase64 }, { headers: { "x-user-id": userId } });
}

export async function deleteMasterResume(userId: string): Promise<void> {
  await apiClient.delete("/masterResume", { headers: { "x-user-id": userId } });
}
