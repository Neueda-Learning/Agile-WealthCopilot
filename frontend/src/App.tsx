import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import AppLayout from './components/AppLayout';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import HoldingsPage from './pages/HoldingsPage';
import TransactionsPage from './pages/TransactionsPage';
import LogTransactionPage from './pages/LogTransactionPage';
import CopilotPage from './pages/CopilotPage';

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  // Wait for the initial /auth/me check — redirecting first would bounce a
  // signed-in user to login on every refresh.
  if (loading) return null;
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  const { user, loading } = useAuth();

  return (
    <Routes>
      <Route
        path="/login"
        element={loading ? null : user ? <Navigate to="/" replace /> : <LoginPage />}
      />
      <Route
        path="/register"
        element={loading ? null : user ? <Navigate to="/" replace /> : <RegisterPage />}
      />
      <Route
        path="/*"
        element={
          <RequireAuth>
            <AppLayout>
              <Routes>
                <Route path="/" element={<DashboardPage />} />
                <Route path="/holdings" element={<HoldingsPage />} />
                <Route path="/transactions" element={<TransactionsPage />} />
                <Route path="/log" element={<LogTransactionPage />} />
                <Route path="/copilot" element={<CopilotPage />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </AppLayout>
          </RequireAuth>
        }
      />
    </Routes>
  );
}
