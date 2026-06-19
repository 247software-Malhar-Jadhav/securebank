import type { FetchBaseQueryError } from "@reduxjs/toolkit/query";
import type { SerializedError } from "@reduxjs/toolkit";
import type { ProblemDetail } from "@/types";

/**
 * Turn any RTK Query error into a user-facing message.
 *
 * The backend returns errors as RFC-7807 application/problem+json with a `message`
 * field already localized to the caller's Accept-Language. We prefer that message so
 * users see the error in their chosen language. We fall back through `detail`/`title`
 * and finally to a generic translated string (passed in by the caller) for transport
 * errors that never reached the backend.
 */
export function extractErrorMessage(
  error: FetchBaseQueryError | SerializedError | undefined,
  fallback: string,
): string {
  if (!error) return fallback;

  // RTK Query "FETCH_ERROR"/"PARSING_ERROR"/"TIMEOUT_ERROR" — no HTTP response.
  if ("status" in error) {
    if (typeof error.status === "string") {
      return fallback;
    }
    const data = error.data as ProblemDetail | undefined;
    if (data) {
      return data.message ?? data.detail ?? data.title ?? fallback;
    }
    return fallback;
  }

  // SerializedError (thrown JS errors).
  return error.message ?? fallback;
}

/** True when the error is a typed HTTP error with the given status code. */
export function isStatus(
  error: FetchBaseQueryError | SerializedError | undefined,
  status: number,
): boolean {
  return !!error && "status" in error && error.status === status;
}
