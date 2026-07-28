import * as React from 'react';

export interface NavItem { id: string; label: string; icon: string; badge?: React.ReactNode }
export interface NavGroup { label?: string; items: NavItem[] }

/**
 * Fixed 236px product sidebar.
 */
export interface SidebarNavProps extends React.HTMLAttributes<HTMLElement> {
  groups?: NavGroup[];
  /** id of the current item. */
  active?: string;
  onNavigate?: (id: string) => void;
  /** Bottom-pinned slot — account switcher, plan status. */
  footer?: React.ReactNode;
  showWordmark?: boolean;
}

export declare function SidebarNav(props: SidebarNavProps): JSX.Element;
