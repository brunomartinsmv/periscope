import { apiRequest } from './client';
import type { Report, ReportName } from '../types';

export function getReport(
  projectId: string,
  name: ReportName,
  limit = 10,
): Promise<Report> {
  const needsLimit =
    name === 'main-applicant' || name === 'main-inventor' || name === 'main-ipc';
  const qs = needsLimit ? `?limit=${limit}` : '';
  return apiRequest<Report>(`/projects/${projectId}/reports/${name}${qs}`);
}
