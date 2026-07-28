import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { auth } from '../api/endpoints';
import { onUnauthorized, tokenStore } from '../api/client';
import type { User } from '../types/api';

interface AuthValue {
  user: User | null;
  /** True until the initial token check resolves — routes wait on this. */
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // A token in storage proves nothing — it may be expired or revoked, so the
  // session is only real once /auth/me confirms it.
  useEffect(() => {
    if (!tokenStore.get()) {
      setLoading(false);
      return;
    }
    let cancelled = false;
    auth.me()
      .then((u) => { if (!cancelled) setUser(u); })
      .catch(() => { tokenStore.clear(); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  // Any 401 from anywhere in the app drops the session.
  useEffect(() => onUnauthorized(() => setUser(null)), []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await auth.login(email, password);
    tokenStore.set(res.accessToken);
    setUser(await auth.me());
  }, []);

  const register = useCallback(async (email: string, password: string, displayName: string) => {
    await auth.register(email, password, displayName);
    await login(email, password);
  }, [login]);

  const logout = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({ user, loading, login, register, logout }),
    [user, loading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
