import { request } from './client';
import type {
  ChatMessageRecord, ChatResponse, Conversation, Holding, HoldingDetail,
  LoginResponse, Page, ParseTransactionResponse, Performance, PerformanceRange,
  PortfolioSummary, Quote, SymbolSearchResult, Transaction, TransactionQuery,
  TransactionRequest, User,
} from '../types/api';

export const auth = {
  register: (email: string, password: string, displayName: string) =>
    request<User>('/auth/register', {
      method: 'POST', anonymous: true, body: { email, password, displayName },
    }),

  login: (email: string, password: string) =>
    request<LoginResponse>('/auth/login', {
      method: 'POST', anonymous: true, body: { email, password },
    }),

  me: () => request<User>('/auth/me'),
};

export const transactions = {
  list: (query: TransactionQuery = {}) =>
    request<Page<Transaction>>('/transactions', {
      query: { page: 0, size: 20, sort: 'tradeDate,desc', ...query },
    }),

  get: (id: number) => request<Transaction>(`/transactions/${id}`),

  create: (body: TransactionRequest) =>
    request<Transaction>('/transactions', { method: 'POST', body }),

  update: (id: number, body: TransactionRequest) =>
    request<Transaction>(`/transactions/${id}`, { method: 'PUT', body }),

  remove: (id: number) =>
    request<void>(`/transactions/${id}`, { method: 'DELETE' }),
};

export const portfolio = {
  summary: () => request<PortfolioSummary>('/portfolio/summary'),
  holdings: () => request<Holding[]>('/portfolio/holdings'),
  holding: (ticker: string) => request<HoldingDetail>(`/portfolio/holdings/${ticker}`),
  performance: (range: PerformanceRange) =>
    request<Performance>('/portfolio/performance', { query: { range } }),
};

export const market = {
  quote: (ticker: string) => request<Quote>(`/market/quote/${ticker}`),
  search: (query: string, signal?: AbortSignal) =>
    request<SymbolSearchResult[]>('/market/search', { query: { query }, signal }),
};

export const ai = {
  /** Feature 1. Returns a draft for confirmation — never writes. */
  parseTransaction: (text: string) =>
    request<ParseTransactionResponse>('/ai/parse-transaction', {
      method: 'POST', body: { text },
    }),

  /** Feature 2. Omit conversationId to start a new conversation. */
  chat: (message: string, conversationId?: number) =>
    request<ChatResponse>('/ai/chat', {
      method: 'POST',
      body: conversationId ? { conversationId, message } : { message },
    }),

  conversations: () => request<Page<Conversation>>('/ai/conversations'),
  messages: (id: number) => request<ChatMessageRecord[]>(`/ai/conversations/${id}/messages`),
  deleteConversation: (id: number) =>
    request<void>(`/ai/conversations/${id}`, { method: 'DELETE' }),
};
