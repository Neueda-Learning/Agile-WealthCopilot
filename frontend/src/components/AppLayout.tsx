import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Badge, Icon, SidebarNav, TopBar, Button, IconButton } from '../design-system';
import { useAuth } from '../context/AuthContext';

const NAV_GROUPS = [
  {
    items: [
      { id: '/', label: 'Dashboard', icon: 'layout-dashboard' },
      { id: '/holdings', label: 'Holdings', icon: 'layers' },
      { id: '/transactions', label: 'Transactions', icon: 'receipt' },
    ],
  },
  {
    label: 'Copilot',
    items: [
      // Lucide renamed plus-circle -> circle-plus; the old name 404s on the
      // icon CDN and silently renders nothing.
      { id: '/log', label: 'Log transaction', icon: 'circle-plus' },
      { id: '/copilot', label: 'Ask Copilot', icon: 'sparkles', badge: <Badge tone="brand">New</Badge> },
    ],
  },
];

const TITLES: Record<string, [string, string]> = {
  '/': ['Dashboard', 'Your portfolio at a glance'],
  '/holdings': ['Holdings', 'Every open position'],
  '/transactions': ['Transactions', 'Everything you have recorded'],
  '/log': ['Log transaction', 'Describe it, review it, save it'],
  '/copilot': ['Ask Copilot', 'Portfolio and markets assistant'],
};

function initials(name: string): string {
  return name.trim().split(/\s+/).slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '').join('') || '?';
}

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const { user, logout } = useAuth();

  const [title, subtitle] = TITLES[pathname] ?? ['WealthCopilot', ''];
  const onCopilot = pathname === '/copilot';

  return (
    <div className="app-shell">
      <SidebarNav
        active={pathname}
        onNavigate={(id: string) => navigate(id)}
        groups={NAV_GROUPS}
        footer={
          <div className="account-chip">
            <span className="account-chip__avatar">{initials(user?.displayName ?? '')}</span>
            <div style={{ lineHeight: 1.2, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>
                {user?.displayName}
              </div>
              <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{user?.email}</div>
            </div>
            <IconButton icon="chevrons-up-down" label="Sign out" size="sm" onClick={logout} />
          </div>
        }
      />
      <div className="app-main">
        <TopBar title={title} subtitle={subtitle}>
          {!onCopilot && (
            <Button size="sm" iconLeft="plus" onClick={() => navigate('/log')}>
              Log transaction
            </Button>
          )}
          {onCopilot && (
            <span className="row" style={{ color: 'var(--text-muted)', fontSize: 'var(--fs-sm)' }}>
              <Icon name="wrench" size={14} />
              You confirm every change
            </span>
          )}
        </TopBar>
        <div className="app-scroll">
          <div className="app-content">{children}</div>
        </div>
      </div>
    </div>
  );
}
