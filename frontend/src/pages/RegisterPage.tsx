import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Banner, Button, Card, Input, Wordmark } from '../design-system';
import { ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useLocale } from '../context/LocaleContext';
import LanguageSwitch from '../components/LanguageSwitch';

export default function RegisterPage() {
  const { register } = useAuth();
  const { t } = useLocale();
  const [displayName, setDisplayName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  const tooShort = password.length > 0 && password.length < 8;

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    setBusy(true);
    try {
      await register(email, password, displayName);
    } catch (err) {
      if (err instanceof ApiError) {
        setFieldErrors(err.fieldIssues());
        setError(
          err.code === 'CONFLICT'
            ? t('That email already has an account. Sign in instead.', '该邮箱已注册，请直接登录。')
            : err.message,
        );
      } else {
        setError(t('Could not create the account.', '无法创建账户。'));
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-language"><LanguageSwitch /></div>
      <div className="auth-card stack-sm">
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 'var(--space-3)' }}>
          <Wordmark size={26} />
        </div>
        <Card title={t('Create an account', '创建账户')} subtitle={t('Your portfolio is visible only to you.', '只有您能查看自己的投资组合。')}>
          <form onSubmit={submit} className="stack-sm">
            {error && <Banner tone="loss" title={t('Cannot create account', '无法创建账户')}>{error}</Banner>}
            <Input
              label={t('Display name', '显示名称')} required autoComplete="name"
              value={displayName} onChange={(e) => setDisplayName(e.target.value)}
              error={fieldErrors.displayName}
            />
            <Input
              label={t('Email', '电子邮箱')} type="email" required autoComplete="email"
              value={email} onChange={(e) => setEmail(e.target.value)}
              error={fieldErrors.email}
            />
            <Input
              label={t('Password', '密码')} type="password" required autoComplete="new-password"
              value={password} onChange={(e) => setPassword(e.target.value)}
              hint={t('At least 8 characters.', '至少 8 个字符。')}
              error={fieldErrors.password || (tooShort ? t('Use at least 8 characters.', '请至少输入 8 个字符。') : undefined)}
            />
            <Button
              type="submit" fullWidth
              disabled={busy || !email || !displayName || password.length < 8}
            >
              {busy ? t('Creating account…', '正在创建账户…') : t('Create account', '创建账户')}
            </Button>
          </form>
        </Card>
        <div style={{ textAlign: 'center', fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>
          {t('Already have an account?', '已有账户？')} <Link to="/login">{t('Sign in', '登录')}</Link>
        </div>
      </div>
    </div>
  );
}
