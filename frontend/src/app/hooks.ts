import { useDispatch, useSelector, type TypedUseSelectorHook } from "react-redux";
import type { AppDispatch, RootState } from "./store";

/**
 * Pre-typed Redux hooks. Always import these instead of the plain react-redux
 * useDispatch/useSelector so dispatch knows our thunk types and selectors get
 * full RootState typing — no `any`, no manual generics at call sites.
 */
export const useAppDispatch = () => useDispatch<AppDispatch>();
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
