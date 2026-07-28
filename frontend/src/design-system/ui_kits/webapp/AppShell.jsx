const { SidebarNav, TopBar, Badge, Wordmark, Icon } = window.WealthCopilotDesignSystem_f10604;

function AppShell({ active, onNavigate, title, subtitle, actions, children }) {
  return (
    <div style={{ display: 'flex', height: '100%', background: 'var(--surface-page)' }}>
      <SidebarNav
        active={active}
        onNavigate={onNavigate}
        groups={[
          { items: [
            { id: 'dashboard', label: 'Dashboard', icon: 'layout-dashboard' },
            { id: 'holdings', label: 'Holdings', icon: 'layers' },
            { id: 'transactions', label: 'Transactions', icon: 'receipt' }
          ] },
          { label: 'Copilot', items: [
            { id: 'log', label: 'Log transaction', icon: 'plus-circle' },
            { id: 'chat', label: 'Ask Copilot', icon: 'sparkles', badge: <Badge tone="brand">New</Badge> }
          ] }
        ]}
        footer={
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-4)', padding: 'var(--space-4)', borderTop: '1px solid var(--border-subtle)' }}>
            <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: 28, height: 28, borderRadius: 999, background: 'var(--surface-inverse)', color: 'var(--ink-0)', fontSize: 12, fontWeight: 600 }}>DM</span>
            <div style={{ lineHeight: 1.2 }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' }}>Dana Mercer</div>
              <div style={{ fontSize: 11, color: 'var(--text-muted)' }}>3 accounts</div>
            </div>
            <Icon name="chevrons-up-down" size={14} style={{ marginLeft: 'auto', color: 'var(--text-disabled)' }} />
          </div>
        }
      />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
        <TopBar title={title} subtitle={subtitle}>{actions}</TopBar>
        <div style={{ flex: 1, overflowY: 'auto', padding: 'var(--page-padding)' }}>
          <div style={{ maxWidth: 'var(--content-max)', margin: '0 auto' }}>{children}</div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { AppShell });
