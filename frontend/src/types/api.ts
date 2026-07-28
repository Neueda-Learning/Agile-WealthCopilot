/* Types mirroring docs/api-spec.md. Keep field names identical to the spec —
   the backend is the source of truth and these are just the wire shapes. */

export type Side = 'BUY' | 'SELL';
export type TransactionSource = 'MANUAL' | 'AI_ASSISTED';
export type Confidence = 'HIGH' | 'MEDIUM' | 'LOW';
export type PerformanceRange = '1M' | '3M' | '6M' | '1Y' | 'ALL';

/** Standard error body — every 4xx/5xx shares this shape (spec §Standard error body). */
export interface ApiErrorBody {
  code: string;
  message: string;
  details?: Array<{ field?: string; issue: string }>;
  timestamp: string;
  path: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/* ── Auth ─────────────────────────────────────────────────────────────── */

export interface User {
  id: number;
  email: string;
  displayName: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

/* ── Transactions ─────────────────────────────────────────────────────── */

export interface Transaction {
  id: number;
  ticker: string;
  instrumentName: string;
  side: Side;
  quantity: number;
  price: number;
  fees: number;
  tradeDate: string;
  note: string | null;
  source: TransactionSource;
  createdAt: string;
}

export interface TransactionRequest {
  ticker: string;
  side: Side;
  quantity: number;
  price: number;
  fees: number;
  tradeDate: string;
  note?: string | null;
  source: TransactionSource;
}

export interface TransactionQuery {
  ticker?: string;
  side?: Side;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  sort?: string;
}

/* ── Portfolio ────────────────────────────────────────────────────────── */

export interface PortfolioSummary {
  totalValue: number;
  totalCostBasis: number;
  unrealizedPnl: number;
  unrealizedPnlPct: number;
  realizedPnl: number;
  dayChange: number;
  dayChangePct: number;
  currency: string;
  pricesAsOf: string;
  stale: boolean;
}

export interface Holding {
  ticker: string;
  name: string;
  type: string;
  quantity: number;
  avgCost: number;
  costBasis: number;
  currentPrice: number;
  marketValue: number;
  unrealizedPnl: number;
  unrealizedPnlPct: number;
  /** null when the cached quote has no previousClose — never treat as 0. */
  dayChangePct: number | null;
  weightPct: number;
  priceAsOf: string;
  stale: boolean;
}

export interface HoldingDetail extends Holding {
  transactions: Transaction[];
}

export interface Performance {
  range: PerformanceRange;
  from: string;
  to: string;
  investedAmount: number;
  proceedsAmount: number;
  netInvested: number;
  realizedPnl: number;
  buyCount: number;
  sellCount: number;
  currency: string;
}

/* ── Market data ──────────────────────────────────────────────────────── */

export interface Quote {
  ticker: string;
  price: number;
  previousClose: number;
  asOf: string;
  stale: boolean;
}

export interface SymbolSearchResult {
  ticker: string;
  name: string;
  exchange: string;
  type: string;
  currency: string;
}

/* ── AI ───────────────────────────────────────────────────────────────── */

export interface TransactionDraft {
  ticker: string;
  side: Side;
  quantity: number;
  price: number;
  tradeDate: string;
}

export interface ParseTransactionResponse {
  draft: TransactionDraft;
  confidence: Confidence;
  warnings: string[];
}

export interface ToolCall {
  name: string;
  durationMs: number;
}

export interface ChatResponse {
  conversationId: number;
  reply: string;
  toolCalls: ToolCall[];
  /** Non-null only when the agent used draft_transaction. The agent never writes. */
  draftTransaction: TransactionDraft | null;
}

export interface Conversation {
  id: number;
  title: string;
  updatedAt: string;
}

export interface ChatMessageRecord {
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
}
