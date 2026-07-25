import { apiRequest } from './client';
import type { Rule, RuleRequest, SuggestionsResponse } from '../types';

export function getSuggestions(
  projectId: string,
  type: 'applicant' | 'inventor',
  query: string,
): Promise<SuggestionsResponse> {
  const params = new URLSearchParams({ type, query });
  return apiRequest<SuggestionsResponse>(
    `/projects/${projectId}/harmonization/suggestions?${params}`,
  );
}

export function listRules(
  projectId: string,
  type?: 'applicant' | 'inventor',
): Promise<Rule[]> {
  const qs = type ? `?type=${type}` : '';
  return apiRequest<Rule[]>(`/projects/${projectId}/harmonization/rules${qs}`);
}

export function createRule(projectId: string, data: RuleRequest): Promise<Rule> {
  return apiRequest<Rule>(`/projects/${projectId}/harmonization/rules`, {
    method: 'POST',
    body: data,
  });
}

export function deleteRule(projectId: string, ruleId: string): Promise<void> {
  return apiRequest<void>(`/projects/${projectId}/harmonization/rules/${ruleId}`, {
    method: 'DELETE',
  });
}

export function applyAllRules(projectId: string): Promise<{ applied: number; message: string }> {
  return apiRequest(`/projects/${projectId}/harmonization/apply`, { method: 'POST' });
}

export function applyRule(
  projectId: string,
  ruleId: string,
): Promise<{ applied: number; ruleId: string; message: string }> {
  return apiRequest(`/projects/${projectId}/harmonization/rules/${ruleId}/apply`, {
    method: 'POST',
  });
}
