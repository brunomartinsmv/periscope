export interface User {
  id: string;
  username: string;
  firstname: string | null;
  lastname: string | null;
  email: string | null;
  userLevel: 'ADMIN' | 'USER' | string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface Project {
  id: string;
  title: string;
  description: string | null;
  isPublic: boolean | null;
  createdAt: string | null;
  updateAt: string | null;
  ownerId: string | null;
  ownerName: string | null;
  patentCount: number;
  observerIds: string[];
}

export interface ProjectRequest {
  title: string;
  description?: string | null;
  isPublic?: boolean | null;
}

export interface Patent {
  id: string;
  title: string | null;
  abstractText: string | null;
  publicationNumber: string | null;
  publicationDate: string | null;
  applicationNumber: string | null;
  applicationDate: string | null;
  applicationCountry: string | null;
  mainClassification: string | null;
  blacklisted: boolean | null;
  completed: boolean | null;
  projectId: string | null;
  applicants: string[];
  inventors: string[];
  presentationFileId: string | null;
  patentInfoFileId: string | null;
}

export interface PatentUpdateRequest {
  title?: string | null;
  abstractText?: string | null;
  blacklisted?: boolean | null;
  completed?: boolean | null;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ImportResult {
  imported: number;
  importer: string;
  fileName: string;
  messages: string[];
}

export type ImporterType = 'DPMA' | 'ESPACENET' | 'PATENTSCOPE';

export interface Rule {
  id: string;
  name: string;
  acronym: string | null;
  type: string;
  substitutions: string[] | Set<string> | null;
  countryAcronym: string | null;
  nature: string | null;
  projectId: string | null;
}

export interface RuleRequest {
  name: string;
  acronym?: string | null;
  type: 'APPLICANT' | 'INVENTOR' | string;
  substitutions?: string[];
  countryAcronym?: string | null;
  nature?: string | null;
}

export interface SuggestionsResponse {
  type: string;
  query: string;
  suggestions: string[];
}

export interface ReportItem {
  key: string;
  value: number;
}

export interface Report {
  name: string;
  label: string;
  items: ReportItem[];
}

export type ReportName =
  | 'main-applicant'
  | 'main-inventor'
  | 'main-ipc'
  | 'application-date'
  | 'publication-date';

export interface ApiErrorBody {
  error?: string;
  status?: number;
  message?: string;
}

export class ApiError extends Error {
  status: number;
  body: ApiErrorBody | null;

  constructor(status: number, message: string, body: ApiErrorBody | null = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }
}
