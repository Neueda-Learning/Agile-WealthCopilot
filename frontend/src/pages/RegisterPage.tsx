import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Banner, Button, Card, Input, Wordmark } from '../design-system';
import { ApiError } from '../api/client';
import { useAuth } from '../context/AuthContext';

export default function RegisterPage() {
  const { register } = useAuth();
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
            ? 'That email already has an account. Sign in instead.'
            : err.message,
        );
      } else {
        setError('Could not create the account.');
      }
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
        <Card title="Create an account" subtitle="Your portfolio is visible only to you.">
          <form onSubmit={submit} className="stack-sm">
            {error && <Banner tone="loss" title="Cannot create account">{error}</Banner>}
            <Input
              label="Display name" required autoComplete="name"
              value={displayName} onChange={(e) => setDisplayName(e.target.value)}
              error={fieldErrors.displayName}
            />
            <Input
              label="Email" type="email" required autoComplete="email"
              value={email} onChange={(e) => setEmail(e.target.value)}
              error={fieldErrors.email}
            />
            <Input
              label="Password" type="password" required autoComplete="new-password"
              value={password} onChange={(e) => setPassword(e.target.value)}
              hint="At least 8 characters."
              error={fieldErrors.password || (tooShort ? 'Use at least 8 characters.' : undefined)}
            />
            <Button
              type="submit" fullWidth
              disabled={busy || !email || !displayName || password.length < 8}
            >
              {busy ? 'Creating account…' : 'Create account'}
            </Button>
          </form>
        </Card>
        <div style={{ textAlign: 'center', fontSize: 'var(--fs-sm)', color: 'var(--text-muted)' }}>
          Already have an account? <Link to="/login">Sign in</Link>
        </div>
      </div>
    </div>
  );
}
