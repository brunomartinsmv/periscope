import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import * as authApi from '../api/auth';
import {
  clearSession,
  getStoredToken,
  getStoredUserJson,
  setStoredToken,
  setStoredUserJson,
  setUnauthorizedHandler,
} from '../api/client';
import type { User } from '../types';
import { AuthContext, type AuthContextValue } from './authContext';

function parseUser(json: string | null): User | null {
  if (!json) return null;
  try {
    return JSON.parse(json) as User;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate();
  const initialToken = getStoredToken();
  const [token, setToken] = useState<string | null>(initialToken);
  const [user, setUser] = useState<User | null>(() => parseUser(getStoredUserJson()));
  const [loading, setLoading] = useState(!!initialToken);

  const handleUnauthorized = useCallback(() => {
    setToken(null);
    setUser(null);
    setLoading(false);
    navigate('/login', { replace: true });
  }, [navigate]);

  useEffect(() => {
    setUnauthorizedHandler(handleUnauthorized);
    return () => setUnauthorizedHandler(null);
  }, [handleUnauthorized]);

  useEffect(() => {
    if (!token) {
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const me = await authApi.me();
        if (!cancelled) {
          setUser(me);
          setStoredUserJson(JSON.stringify(me));
        }
      } catch {
        if (!cancelled) {
          clearSession();
          setToken(null);
          setUser(null);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token]);

  const login = useCallback(async (username: string, password: string) => {
    const result = await authApi.login({ username, password });
    setStoredToken(result.token);
    setStoredUserJson(JSON.stringify(result.user));
    setLoading(false);
    setToken(result.token);
    setUser(result.user);
  }, []);

  const logout = useCallback(async () => {
    try {
      if (getStoredToken()) {
        await authApi.logout();
      }
    } catch {
      // discard token locally regardless of server response
    } finally {
      clearSession();
      setToken(null);
      setUser(null);
      setLoading(false);
      navigate('/login', { replace: true });
    }
  }, [navigate]);

  const refreshUser = useCallback(async () => {
    const me = await authApi.me();
    setUser(me);
    setStoredUserJson(JSON.stringify(me));
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      isAuthenticated: !!token && !!user,
      isAdmin: user?.userLevel === 'ADMIN',
      loading: !!token && loading,
      login,
      logout,
      refreshUser,
    }),
    [user, token, loading, login, logout, refreshUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
