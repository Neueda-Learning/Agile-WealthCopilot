import { Icon } from '../design-system';
import { useLocale } from '../context/LocaleContext';

export default function LanguageSwitch() {
  const { isChinese, toggleLocale, t } = useLocale();

  return (
    <button
      type="button"
      className="language-switch"
      onClick={toggleLocale}
      aria-label={t('Switch language to Simplified Chinese', '切换语言为英文')}
      title={t('Switch to Simplified Chinese', '切换为英文')}
    >
      <Icon name="languages" size={15} />
      <span>{isChinese ? 'EN' : '简体中文'}</span>
    </button>
  );
}
