import React from "react";
import ReactDOM from "react-dom/client";
import { Provider } from "react-redux";
import { store } from "@/app/store";
import App from "@/App";
import "@/i18n"; // side-effect import: initializes react-i18next before anything renders
import "@/index.css";

/**
 * Application entry point.
 *
 * The provider order matters: <Provider> (Redux store) must wrap <App> so that
 * RTK Query hooks and our auth selectors work everywhere. i18n is imported for its
 * initialization side effect (no component needed — useTranslation reads the
 * singleton instance). StrictMode surfaces unsafe lifecycles in development.
 */
ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </React.StrictMode>,
);
