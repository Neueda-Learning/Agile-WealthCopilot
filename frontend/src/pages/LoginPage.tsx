import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Banner, Button, Card, Input, Wordmark } from '../design-system';
import { ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login } = useAuth();
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
          ? 'That email and password do not match. Check both and try again.'
          : err instanceof ApiError ? err.message : 'Sign in failed.',
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card stack-sm">
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 'var(--space-3)' }}>
          <Wordmark size={26} />
        </div>
        <Card title="Sign in" subtitle="Track your portfolio and ask Copilot about it.">
          <form onSubmit={submit} className="stack-sm">
            {error && <Banner tone="loss" title="Cannot sign in">{error}</Banner>}
            <Input
              label="Email" type="email" autoComplete="email" required
              value={email} onChange={(e) => setEmail(e.target.value)}
            />
            <Input
              label="Password" type="password" autoComplete="current-password" required
              value={password} onChange={(e) => setPassword(e.target.value)}
            />
            <Button type="submit" fullWidth disabled={busy || !email || !password}>
              {busy ? 'Signing in…' : 'Sign in'}
            </Button>
          </form>
        </Card>
        <div style={{ textAlign: 'center', fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>
          No account yet? <Link to="/register">Create one</Link>
        </div>
      </div>
    </div>
  );
}
