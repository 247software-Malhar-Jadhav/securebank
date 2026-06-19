import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { AuthResponse, UserSummary } from "@/types";

/**
 * Auth slice — owns the *session*: the JWT pair and the current user.
 *
 * Why tokens live in both Redux AND localStorage:
 * - Redux is the live, in-memory source of truth that components and the RTK Query
 *   baseQuery read synchronously on every request.
 * - localStorage persists the session across full page reloads (Redux state is wiped
 *   on reload). On boot we hydrate the slice from localStorage (see loadInitialState).
 *
 * This is a deliberately simple token store suitable for this app. In a hardened
 * production setting you might keep the refresh token in an httpOnly cookie instead;
 * the trade-offs are documented in docs/state-management.md.
 */

const STORAGE_KEY = "securebank.auth";

export interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserSummary | null;
}

/** Read persisted auth from localStorage so a reload keeps the user signed in. */
function loadInitialState(): AuthState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      return JSON.parse(raw) as AuthState;
    }
  } catch {
    // Corrupt/unavailable storage -> start logged out.
  }
  return { accessToken: null, refreshToken: null, user: null };
}

/** Mirror the current slice into localStorage (called by every reducer that mutates). */
function persist(state: AuthState): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    // Storage may be full or blocked; the in-memory session still works.
  }
}

const authSlice = createSlice({
  name: "auth",
  initialState: loadInitialState(),
  reducers: {
    /** Store a fresh session after login/register. */
    setCredentials(state, action: PayloadAction<AuthResponse>) {
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      state.user = action.payload.user;
      persist(state);
    },
    /**
     * Update just the tokens after a silent refresh (the user object is unchanged).
     * Used by the baseQuery re-auth flow in services/api.ts.
     */
    setTokens(
      state,
      action: PayloadAction<{ accessToken: string; refreshToken: string }>,
    ) {
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      persist(state);
    },
    /** Clear the session (manual logout or unrecoverable 401). */
    logout(state) {
      state.accessToken = null;
      state.refreshToken = null;
      state.user = null;
      try {
        localStorage.removeItem(STORAGE_KEY);
      } catch {
        /* ignore */
      }
    },
  },
});

export const { setCredentials, setTokens, logout } = authSlice.actions;
export default authSlice.reducer;
