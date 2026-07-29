import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Badge, Icon, SidebarNav, TopBar, Button, IconButton } from '../design-system';
import { useAuth } from '../context/AuthContext';
import { useLocale } from '../context/LocaleContext';
import LanguageSwitch from './LanguageSwitch';

function initials(name: string): string {
  return name.trim().split(/\s+/).slice(0, 2).map((p) => p[0]?.toUpperCase() ?? '').join('') || '?';
}

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const { user, logout } = useAuth();
  const { t } = useLocale();

  const navGroups = [
    {
      items: [
        { id: '/', label: t('Dashboard', '概览'), icon: 'layout-dashboard' },
        { id: '/holdings', label: t('Holdings', '持仓'), icon: 'layers' },
        { id: '/transactions', label: t('Transactions', '交易记录'), icon: 'receipt' },
      ],
    },
    {
      label: t('Copilot', '智能助手'),
      items: [
        { id: '/log', label: t('Log transaction', '记录交易'), icon: 'circle-plus' },
        {
          id: '/copilot',
          label: t('Ask Copilot', '咨询智能助手'),
          icon: 'sparkles',
          badge: <Badge tone="brand">{t('New', '新')}</Badge>,
        },
      ],
    },
  ];

  const titles: Record<string, [string, string]> = {
    '/': [t('Dashboard', '概览'), t('Your portfolio at a glance', '一览您的投资组合')],
    '/holdings': [t('Holdings', '持仓'), t('Every open position', '所有当前持仓')],
    '/transactions': [t('Transactions', '交易记录'), t('Everything you have recorded', '您记录的所有交易')],
    '/log': [t('Log transaction', '记录交易'), t('Describe it, review it, save it', '描述、核对并保存交易')],
    '/copilot': [t('Ask Copilot', '咨询智能助手'), t('Portfolio and markets assistant', '投资组合与市场助手')],
  };

  const [title, subtitle] = titles[pathname] ?? ['WealthCopilot', ''];
  const onCopilot = pathname === '/copilot';

  return (
    <div className="app-shell">
      <SidebarNav
        active={pathname}
        onNavigate={(id: string) => navigate(id)}
        groups={navGroups}
        footer={
          <div className="account-chip">
            <span className="account-chip__avatar">{initials(user?.displayName ?? '')}</span>
            <div style={{ lineHeight: 1.2, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>
                {user?.displayName}
              </div>
              <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>{user?.email}</div>
            </div>
            <IconButton icon="log-out" label={t('Sign out', '退出登录')} size="sm" onClick={logout} />
          </div>
        }
      />
      <div className="app-main">
        <TopBar title={title} subtitle={subtitle}>
          <LanguageSwitch />
          {!onCopilot && (
            <Button size="sm" iconLeft="plus" onClick={() => navigate('/log')}>
              {t('Log transaction', '记录交易')}
            </Button>
          )}
          {onCopilot && (
            <span className="row" style={{ color: 'var(--text-muted)', fontSize: 'var(--fs-sm)' }}>
              <Icon name="wrench" size={14} />
              {t('You confirm every change', '所有更改均由您确认')}
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
