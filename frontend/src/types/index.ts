/**
 * SecureBank API DTOs.
 *
 * These TypeScript interfaces mirror the backend's request/response shapes
 * (see PROJECT_SPEC.md §4 data model and §5 REST surface). They are the single
 * typed contract used by RTK Query endpoints and every component. Money is always
 * a `string` here because the server serializes BigDecimal/NUMERIC(19,4) as a
 * string to avoid floating-point corruption — we only parse it for display.
 */

// ---------------------------------------------------------------------------
// Enums (string unions match the backend's enum names exactly)
// ---------------------------------------------------------------------------

export type Role = "CUSTOMER" | "TELLER" | "ADMIN";

export type AccountType = "SAVINGS" | "CURRENT" | "FIXED_DEPOSIT";

export type AccountStatus = "ACTIVE" | "FROZEN" | "CLOSED";

export type TransactionType = "DEPOSIT" | "WITHDRAWAL" | "TRANSFER";

export type TransactionStatus = "PENDING" | "COMPLETED" | "FAILED" | "REVERSED";

export type KycStatus = "PENDING" | "VERIFIED" | "REJECTED";

export type LedgerDirection = "DEBIT" | "CREDIT";

export type SupportedLocale = "en" | "hi" | "mr";

// ---------------------------------------------------------------------------
// Auth & identity
// ---------------------------------------------------------------------------

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  preferredLocale?: SupportedLocale;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

/** Returned by /auth/login, /auth/register and /auth/refresh. */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string; // "Bearer"
  expiresIn: number; // seconds
  user: UserSummary;
}

export interface UserSummary {
  id: number;
  username: string;
  email: string;
  role: Role;
  preferredLocale: SupportedLocale;
}

// ---------------------------------------------------------------------------
// Customer / KYC
// ---------------------------------------------------------------------------

export interface Customer {
  id: number;
  userId: number;
  firstName: string;
  lastName: string;
  phone: string | null;
  dateOfBirth: string | null;
  kycStatus: KycStatus;
  addressLine: string | null;
  city: string | null;
  state: string | null;
  postalCode: string | null;
  country: string | null;
}

// ---------------------------------------------------------------------------
// Accounts
// ---------------------------------------------------------------------------

export interface Account {
  id: number;
  accountNumber: string;
  customerId: number;
  type: AccountType;
  currency: string; // ISO 4217, e.g. "INR", "USD"
  balance: string; // BigDecimal as string
  status: AccountStatus;
  openedAt: string; // ISO timestamp
}

export interface OpenAccountRequest {
  type: AccountType;
  currency: string;
}

// ---------------------------------------------------------------------------
// Transactions / money movement
// ---------------------------------------------------------------------------

export interface Transaction {
  id: number;
  reference: string;
  accountId: number;
  counterpartyAccountId: number | null;
  type: TransactionType;
  amount: string;
  currency: string;
  status: TransactionStatus;
  description: string | null;
  balanceAfter: string;
  fraudScore: number | null;
  createdAt: string;
}

export interface DepositRequest {
  accountId: number;
  amount: string;
  description?: string;
}

export interface WithdrawRequest {
  accountId: number;
  amount: string;
  description?: string;
}

/** The locked, double-entry transfer path. */
export interface TransferRequest {
  fromAccountId: number;
  /** Destination is identified by raw account number (internal or a beneficiary's). */
  toAccountNumber: string;
  amount: string;
  description?: string;
}

// ---------------------------------------------------------------------------
// Beneficiaries
// ---------------------------------------------------------------------------

export interface Beneficiary {
  id: number;
  customerId: number;
  name: string;
  accountNumber: string;
  bankName: string | null;
  createdAt: string;
}

export interface CreateBeneficiaryRequest {
  name: string;
  accountNumber: string;
  bankName?: string;
}

// ---------------------------------------------------------------------------
// AI: spending insights & assistant
// ---------------------------------------------------------------------------

export interface SpendingCategory {
  category: string;
  amount: string;
  /** 0..100 percentage of total spending in this category. */
  percentage: number;
}

export interface SpendingInsights {
  currency: string;
  totalSpent: string;
  categories: SpendingCategory[];
  /** Natural-language summary, already localized by the backend. */
  summary: string;
  periodStart: string;
  periodEnd: string;
}

export interface AssistantRequest {
  question: string;
}

export interface AssistantResponse {
  answer: string;
  /** Backend signals whether the LLM or the deterministic fallback answered. */
  source: "LLM" | "FALLBACK";
}

// ---------------------------------------------------------------------------
// Admin: audit logs
// ---------------------------------------------------------------------------

export interface AuditLog {
  id: number;
  actor: string;
  action: string;
  entityType: string;
  entityId: string;
  details: Record<string, unknown> | null;
  createdAt: string;
}

// ---------------------------------------------------------------------------
// Errors — RFC-7807 problem+json
// ---------------------------------------------------------------------------

/**
 * The backend returns errors as application/problem+json. `message` is the
 * field localized to the request's Accept-Language; we surface it directly in
 * toasts so users see errors in their chosen language.
 */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  message?: string;
  /** Field-level validation errors keyed by field name (if present). */
  errors?: Record<string, string>;
}
