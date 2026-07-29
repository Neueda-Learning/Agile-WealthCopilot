import { createContext, useContext, useMemo, useState } from 'react';

export type AppLocale = 'en' | 'zh-CN';

const LOCALE_KEY = 'wc.locale';

interface LocaleContextValue {
  locale: AppLocale;
  isChinese: boolean;
  setLocale: (locale: AppLocale) => void;
  toggleLocale: () => void;
  t: (english: string, chinese: string) => string;
}

const LocaleContext = createContext<LocaleContextValue | null>(null);

function initialLocale(): AppLocale {
  const stored = localStorage.getItem(LOCALE_KEY);
  if (stored === 'en' || stored === 'zh-CN') return stored;
  return navigator.language.toLowerCase().startsWith('zh') ? 'zh-CN' : 'en';
}

export function LocaleProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocaleState] = useState<AppLocale>(() => {
    const value = initialLocale();
    document.documentElement.lang = value;
    return value;
  });

  function setLocale(value: AppLocale) {
    localStorage.setItem(LOCALE_KEY, value);
    document.documentElement.lang = value;
    setLocaleState(value);
  }

  const value = useMemo<LocaleContextValue>(() => ({
    locale,
    isChinese: locale === 'zh-CN',
    setLocale,
    toggleLocale: () => setLocale(locale === 'en' ? 'zh-CN' : 'en'),
    t: (english, chinese) => locale === 'zh-CN' ? chinese : english,
  }), [locale]);

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>;
}

export function useLocale(): LocaleContextValue {
  const value = useContext(LocaleContext);
  if (!value) throw new Error('useLocale must be used inside LocaleProvider');
  return value;
}

