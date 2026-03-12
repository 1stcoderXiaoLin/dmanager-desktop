import { ref, watch } from "vue";
import { messages, type AppLocale } from "./messages";

const STORAGE_KEY = "dmanager.locale";
const DEFAULT_LOCALE: AppLocale = "zh-CN";

function isLocale(value: string | null): value is AppLocale {
  return value === "zh-CN" || value === "en-US";
}

function getInitialLocale(): AppLocale {
  if (typeof window === "undefined") {
    return DEFAULT_LOCALE;
  }

  const savedLocale = window.localStorage.getItem(STORAGE_KEY);
  return isLocale(savedLocale) ? savedLocale : DEFAULT_LOCALE;
}

function resolvePath(locale: AppLocale, key: string): string | undefined {
  return key.split(".").reduce<unknown>((current, part) => {
    if (current && typeof current === "object" && part in current) {
      return (current as Record<string, unknown>)[part];
    }
    return undefined;
  }, messages[locale]) as string | undefined;
}

function formatMessage(template: string, params?: Record<string, string | number>) {
  if (!params) {
    return template;
  }

  return template.replace(/\{(\w+)\}/g, (_, token) => String(params[token] ?? `{${token}}`));
}

const locale = ref<AppLocale>(getInitialLocale());

watch(
  locale,
  (value) => {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(STORAGE_KEY, value);
      document.documentElement.lang = value;
    }
  },
  { immediate: true },
);

export function useI18n() {
  function setLocale(value: AppLocale) {
    locale.value = value;
  }

  function t(key: string, params?: Record<string, string | number>) {
    const message = resolvePath(locale.value, key) ?? resolvePath(DEFAULT_LOCALE, key) ?? key;
    return formatMessage(message, params);
  }

  return {
    locale,
    setLocale,
    t,
  };
}

export { DEFAULT_LOCALE };
