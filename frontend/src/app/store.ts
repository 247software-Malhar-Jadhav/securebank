import { configureStore } from "@reduxjs/toolkit";
import { setupListeners } from "@reduxjs/toolkit/query";
import { api } from "@/services/api";
import authReducer from "@/features/auth/authSlice";

/**
 * The Redux store.
 *
 * Two reducers live here:
 *  - `auth`: our small hand-written slice for the JWT session + current user.
 *  - `api`: the RTK Query reducer that holds the entire server-data cache. Its
 *    middleware MUST be added below for caching, invalidation, and polling to work.
 *
 * setupListeners enables RTK Query's refetchOnFocus / refetchOnReconnect behaviors.
 */
export const store = configureStore({
  reducer: {
    auth: authReducer,
    [api.reducerPath]: api.reducer,
  },
  middleware: (getDefault) => getDefault().concat(api.middleware),
});

setupListeners(store.dispatch);

// Strongly-typed helpers used throughout the app (see app/hooks.ts).
export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
