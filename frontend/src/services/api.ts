import {
  createApi,
  fetchBaseQuery,
  type BaseQueryFn,
  type FetchArgs,
  type FetchBaseQueryError,
} from "@reduxjs/toolkit/query/react";
import { Mutex } from "./mutex";
import i18n from "@/i18n";
import { logout, setTokens } from "@/features/auth/authSlice";
import type { RootState } from "@/app/store";
import type {
  Account,
  AssistantRequest,
  AssistantResponse,
  AuditLog,
  AuthResponse,
  Beneficiary,
  CreateBeneficiaryRequest,
  Customer,
  DepositRequest,
  LoginRequest,
  OpenAccountRequest,
  RegisterRequest,
  SpendingInsights,
  Transaction,
  TransferRequest,
  WithdrawRequest,
} from "@/types";

/**
 * RTK Query API definition — the single networking layer for SecureBank.
 *
 * Everything the UI fetches goes through here. RTK Query gives us, for free:
 * - auto-generated React hooks (useGetAccountsQuery, useTransferMutation, …)
 * - request de-duplication and a normalized cache
 * - declarative cache invalidation via "tags" (see tagTypes below)
 * - loading/error state on every hook so components stay declarative.
 */

// ---------------------------------------------------------------------------
// baseQuery: the low-level fetch wrapper shared by all endpoints.
// ---------------------------------------------------------------------------

/**
 * The raw base query. Two cross-cutting concerns are wired here:
 *  1. Authorization: attach the JWT bearer token from the auth slice.
 *  2. Accept-Language: tell the backend which language to localize messages and
 *     validation errors in. We read i18n.language so backend + frontend always agree.
 */
const rawBaseQuery = fetchBaseQuery({
  baseUrl: "/api",
  prepareHeaders: (headers, { getState }) => {
    const token = (getState() as RootState).auth.accessToken;
    if (token) {
      headers.set("authorization", `Bearer ${token}`);
    }
    // i18n.language can be "en-US"; the backend expects an en/hi/mr family, and
    // Accept-Language q-values let it negotiate, so the base name is enough.
    headers.set("accept-language", i18n.language || "en");
    return headers;
  },
});

// A mutex ensures that if many requests 401 at the same moment, only ONE refresh
// call is fired; the rest wait for it and then retry with the new token. Without
// this we'd hammer /auth/refresh with concurrent refreshes and possibly rotate the
// refresh token out from under ourselves.
const refreshMutex = new Mutex();

/**
 * baseQueryWithReauth — wraps rawBaseQuery to implement silent re-auth on 401.
 *
 * Flow:
 *   1. Run the request normally.
 *   2. If it comes back 401 (expired access token), grab the mutex.
 *   3. Call POST /auth/refresh with the stored refresh token.
 *      - success: save the new tokens, then RETRY the original request.
 *      - failure: the refresh token is dead too -> dispatch logout(); the protected
 *        route wrapper will then bounce the user to /login.
 *   4. If another request is already refreshing, just wait for it, then retry.
 */
const baseQueryWithReauth: BaseQueryFn<
  string | FetchArgs,
  unknown,
  FetchBaseQueryError
> = async (args, apiCtx, extraOptions) => {
  // If a refresh is in progress, wait for it to finish before sending our request.
  await refreshMutex.waitForUnlock();

  let result = await rawBaseQuery(args, apiCtx, extraOptions);

  if (result.error && result.error.status === 401) {
    // Only one refresh at a time.
    if (!refreshMutex.isLocked()) {
      const release = await refreshMutex.acquire();
      try {
        const refreshToken = (apiCtx.getState() as RootState).auth.refreshToken;
        if (refreshToken) {
          const refreshResult = await rawBaseQuery(
            {
              url: "/auth/refresh",
              method: "POST",
              body: { refreshToken },
            },
            apiCtx,
            extraOptions,
          );

          if (refreshResult.data) {
            const data = refreshResult.data as AuthResponse;
            apiCtx.dispatch(
              setTokens({
                accessToken: data.accessToken,
                refreshToken: data.refreshToken,
              }),
            );
            // Retry the original request with the refreshed token.
            result = await rawBaseQuery(args, apiCtx, extraOptions);
          } else {
            apiCtx.dispatch(logout());
          }
        } else {
          apiCtx.dispatch(logout());
        }
      } finally {
        release();
      }
    } else {
      // Someone else is refreshing — wait, then retry once they're done.
      await refreshMutex.waitForUnlock();
      result = await rawBaseQuery(args, apiCtx, extraOptions);
    }
  }

  return result;
};

// ---------------------------------------------------------------------------
// The API slice.
// ---------------------------------------------------------------------------

export const api = createApi({
  reducerPath: "api",
  baseQuery: baseQueryWithReauth,
  /**
   * Tag types power cache invalidation. An endpoint that READS data "provides" tags;
   * an endpoint that WRITES data "invalidates" tags. When tags overlap, RTK Query
   * automatically refetches the affected queries. Example: a Transfer invalidates
   * "Account" + "Transaction", so the dashboard balances and the transaction list
   * refresh themselves with no manual refetch calls.
   */
  tagTypes: [
    "Account",
    "Transaction",
    "Beneficiary",
    "Customer",
    "Insights",
    "AuditLog",
  ],
  endpoints: (builder) => ({
    // ---- Auth -------------------------------------------------------------
    register: builder.mutation<AuthResponse, RegisterRequest>({
      query: (body) => ({ url: "/auth/register", method: "POST", body }),
    }),
    login: builder.mutation<AuthResponse, LoginRequest>({
      query: (body) => ({ url: "/auth/login", method: "POST", body }),
    }),
    // Note: refresh is handled imperatively inside baseQueryWithReauth, but we also
    // expose it as an endpoint for completeness/testing.
    refresh: builder.mutation<AuthResponse, { refreshToken: string }>({
      query: (body) => ({ url: "/auth/refresh", method: "POST", body }),
    }),

    // ---- Customer ---------------------------------------------------------
    getMe: builder.query<Customer, void>({
      query: () => "/customers/me",
      providesTags: ["Customer"],
    }),

    // ---- Accounts ---------------------------------------------------------
    getAccounts: builder.query<Account[], void>({
      query: () => "/accounts",
      // Provide a tag per account id PLUS a list-level tag, so both "refetch one
      // account" and "refetch the whole list" invalidations work.
      providesTags: (result) =>
        result
          ? [
              ...result.map((a) => ({ type: "Account" as const, id: a.id })),
              { type: "Account" as const, id: "LIST" },
            ]
          : [{ type: "Account" as const, id: "LIST" }],
    }),
    getAccount: builder.query<Account, number>({
      query: (id) => `/accounts/${id}`,
      providesTags: (_r, _e, id) => [{ type: "Account", id }],
    }),
    openAccount: builder.mutation<Account, OpenAccountRequest>({
      query: (body) => ({ url: "/accounts", method: "POST", body }),
      invalidatesTags: [{ type: "Account", id: "LIST" }],
    }),
    getAccountTransactions: builder.query<Transaction[], number>({
      query: (id) => `/accounts/${id}/transactions`,
      providesTags: (result, _e, id) =>
        result
          ? [
              ...result.map((t) => ({ type: "Transaction" as const, id: t.id })),
              { type: "Transaction" as const, id: `ACCOUNT-${id}` },
            ]
          : [{ type: "Transaction" as const, id: `ACCOUNT-${id}` }],
    }),

    // ---- Transactions -----------------------------------------------------
    deposit: builder.mutation<Transaction, DepositRequest>({
      query: (body) => ({ url: "/transactions/deposit", method: "POST", body }),
      // A deposit changes one account's balance and adds a transaction.
      invalidatesTags: (_r, _e, arg) => [
        { type: "Account", id: arg.accountId },
        { type: "Account", id: "LIST" },
        { type: "Transaction", id: `ACCOUNT-${arg.accountId}` },
        "Insights",
      ],
    }),
    withdraw: builder.mutation<Transaction, WithdrawRequest>({
      query: (body) => ({ url: "/transactions/withdraw", method: "POST", body }),
      invalidatesTags: (_r, _e, arg) => [
        { type: "Account", id: arg.accountId },
        { type: "Account", id: "LIST" },
        { type: "Transaction", id: `ACCOUNT-${arg.accountId}` },
        "Insights",
      ],
    }),
    transfer: builder.mutation<Transaction, TransferRequest>({
      query: (body) => ({ url: "/transactions/transfer", method: "POST", body }),
      // A transfer touches BOTH the source account and possibly internal
      // destination accounts. We invalidate the whole account list (so every
      // balance refreshes) plus the source account's transaction list and insights.
      invalidatesTags: (_r, _e, arg) => [
        { type: "Account", id: "LIST" },
        { type: "Account", id: arg.fromAccountId },
        { type: "Transaction", id: `ACCOUNT-${arg.fromAccountId}` },
        "Insights",
      ],
    }),
    getTransactionByReference: builder.query<Transaction, string>({
      query: (reference) => `/transactions/${reference}`,
      providesTags: (result) =>
        result ? [{ type: "Transaction", id: result.id }] : [],
    }),

    // ---- Beneficiaries ----------------------------------------------------
    getBeneficiaries: builder.query<Beneficiary[], void>({
      query: () => "/beneficiaries",
      providesTags: (result) =>
        result
          ? [
              ...result.map((b) => ({ type: "Beneficiary" as const, id: b.id })),
              { type: "Beneficiary" as const, id: "LIST" },
            ]
          : [{ type: "Beneficiary" as const, id: "LIST" }],
    }),
    createBeneficiary: builder.mutation<Beneficiary, CreateBeneficiaryRequest>({
      query: (body) => ({ url: "/beneficiaries", method: "POST", body }),
      invalidatesTags: [{ type: "Beneficiary", id: "LIST" }],
    }),

    // ---- AI: insights & assistant ----------------------------------------
    getSpendingInsights: builder.query<SpendingInsights, void>({
      query: () => "/insights/spending",
      providesTags: ["Insights"],
    }),
    askAssistant: builder.mutation<AssistantResponse, AssistantRequest>({
      query: (body) => ({ url: "/assistant/ask", method: "POST", body }),
    }),

    // ---- Admin ------------------------------------------------------------
    getAuditLogs: builder.query<AuditLog[], void>({
      query: () => "/admin/audit-logs",
      providesTags: ["AuditLog"],
    }),
  }),
});

// RTK Query auto-generates a typed hook per endpoint. Export them for components.
export const {
  useRegisterMutation,
  useLoginMutation,
  useRefreshMutation,
  useGetMeQuery,
  useGetAccountsQuery,
  useGetAccountQuery,
  useOpenAccountMutation,
  useGetAccountTransactionsQuery,
  useDepositMutation,
  useWithdrawMutation,
  useTransferMutation,
  useGetTransactionByReferenceQuery,
  useGetBeneficiariesQuery,
  useCreateBeneficiaryMutation,
  useGetSpendingInsightsQuery,
  useAskAssistantMutation,
  useGetAuditLogsQuery,
} = api;
