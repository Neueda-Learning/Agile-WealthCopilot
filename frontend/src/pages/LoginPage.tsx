import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Banner, Button, Card, Input, Wordmark } from '../design-system';
import { ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useLocale } from '../context/LocaleContext';
import LanguageSwitch from '../components/LanguageSwitch';

export default function LoginPage() {
  const { login } = useAuth();
  const { t } = useLocale();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await login(email, password);
    } catch (err) {
      // 401 here means bad credentials, not an expired session.
      setError(
        err instanceof ApiError && err.status === 401
          ? t('That email and password do not match. Check both and try again.', '邮箱或密码不正确，请检查后重试。')
          : err instanceof ApiError ? err.message : t('Sign in failed.', '登录失败。'),
      );
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
        <Card title={t('Sign in', '登录')} subtitle={t('Track your portfolio and ask Copilot about it.', '跟踪您的投资组合并向智能助手提问。')}>
          <form onSubmit={submit} className="stack-sm">
            {error && <Banner tone="loss" title={t('Cannot sign in', '无法登录')}>{error}</Banner>}
            <Input
              label={t('Email', '电子邮箱')} type="email" autoComplete="email" required
              value={email} onChange={(e) => setEmail(e.target.value)}
            />
            <Input
              label={t('Password', '密码')} type="password" autoComplete="current-password" required
              value={password} onChange={(e) => setPassword(e.target.value)}
            />
            <Button type="submit" fullWidth disabled={busy || !email || !password}>
              {busy ? t('Signing in…', '正在登录…') : t('Sign in', '登录')}
            </Button>
          </form>
        </Card>
        <div style={{ textAlign: 'center', fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>
          {t('No account yet?', '还没有账户？')} <Link to="/register">{t('Create one', '创建账户')}</Link>
        </div>
      </div>
    </div>
  );
}
