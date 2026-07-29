import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Badge, Banner, Button, Card, ChatComposer, ParsedTransactionCard,
} from '../design-system';
import { ApiError } from '../api/client';
import { ai, transactions as txApi } from '../api/endpoints';
import { money, quantity, tradeDate } from '../lib/format';
import TransactionForm, { emptyValues, valuesFromDraft } from '../components/TransactionForm';
import type { Confidence, Transaction, TransactionDraft } from '../types/api';
import { useLocale } from '../context/LocaleContext';

/** Copilot's own confidence, lower-cased for the badge. */
const CONFIDENCE_LABEL: Record<Confidence, 'high' | 'medium' | 'low'> = {
  HIGH: 'high', MEDIUM: 'medium', LOW: 'low',
};

export default function LogTransactionPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { locale, t } = useLocale();
  const examples = locale === 'zh-CN'
    ? ['上周二以 142 美元买入 15 股 Nvidia', '昨天以 182.40 美元卖出 20 股 AAPL']
    : ['Bought 15 Nvidia at 142 last Tuesday', 'Sold 20 AAPL at 182.40 yesterday'];
  // The Copilot agent can hand over a draft; it never writes one itself.
  const handoff = (location.state as { draft?: TransactionDraft } | null)?.draft ?? null;

  const [text, setText] = useState('');
  const [draft, setDraft] = useState<TransactionDraft | null>(handoff);
  const [confidence, setConfidence] = useState<Confidence | null>(handoff ? 'HIGH' : null);
  const [warnings, setWarnings] = useState<string[]>([]);
  const [sourceText, setSourceText] = useState<string | null>(null);

  const [parseError, setParseError] = useState<ApiError | null>(null);
  const [parsing, setParsing] = useState(false);
  // A handoff arrives because the user chose "Edit" in the chat, so go
  // straight to the form; a draft parsed here starts on the review card.
  const [editing, setEditing] = useState(handoff !== null);
  const [manual, setManual] = useState(false);
  const [saved, setSaved] = useState<Transaction | null>(null);
  const [saving, setSaving] = useState(false);

  async function parse() {
    const t = text.trim();
    if (!t) return;
    setParsing(true);
    setParseError(null);
    setSaved(null);
    try {
      const res = await ai.parseTransaction(t, locale);
      setDraft(res.draft);
      setConfidence(res.confidence);
      setWarnings(res.warnings ?? []);
      setSourceText(t);
      setEditing(false);
    } catch (e) {
      if (e instanceof ApiError) setParseError(e);
      setDraft(null);
    } finally {
      setParsing(false);
    }
  }

  /**
   * Confirming the card saves it outright — the review card already showed
   * every field, so re-presenting the same values in a form asked the user to
   * approve twice. The form is only for when they want to change something,
   * or when the save is rejected and they need to fix it.
   */
  async function confirmDraft() {
    if (!draft || saving) return;
    setSaving(true);
    setParseError(null);
    try {
      const body = {
        ticker: draft.ticker, side: draft.side,
        quantity: draft.quantity, price: draft.price,
        fees: 0, tradeDate: draft.tradeDate, note: null,
        source: 'AI_ASSISTED' as const,
      };
      const t = draft.transactionId
        ? await txApi.update(draft.transactionId, body)
        : await txApi.create(body);
      onSaved(t);
    } catch (e) {
      // Unknown ticker, or a timeline conflict — the form surfaces the
      // field-level detail and lets them correct it.
      if (e instanceof ApiError) setParseError(e);
      setEditing(true);
    } finally {
      setSaving(false);
    }
  }

  function onSaved(t: Transaction) {
    setSaved(t);
    setDraft(null);
    setEditing(false);
    setManual(false);
    setText('');
    setWarnings([]);
    setSourceText(null);
  }

  const parseFailed = parseError?.code === 'AI_PARSE_FAILED';
  const aiDown = parseError?.code === 'AI_UNAVAILABLE';

  return (
    <div className="grid-2">
      <div className="stack">
        <Card
          title={t('Describe the transaction', '描述交易')}
          subtitle={t('Plain English. Copilot fills the form, you confirm it.', '使用自然语言描述，智能助手填写表单，再由您确认。')}
          action={<Badge tone="brand" icon="sparkles">AI</Badge>}
        >
          <ChatComposer
            value={text}
            onChange={setText}
            onSubmit={parse}
            placeholder={t('e.g. bought 15 NVDA at 142 last Tuesday', '例如：上周二以 142 美元买入 15 股 NVDA')}
            hint={t('Nothing is saved until you confirm.', '只有您确认后才会保存。')}
            submitLabel={parsing ? t('Parsing…', '正在解析…') : t('Parse', '解析')}
            suggestions={examples}
            onSuggestion={setText}
            disabled={parsing}
          />
        </Card>

        {parseError && (
          <Banner
            tone={aiDown ? 'caution' : 'loss'}
            title={aiDown
              ? t('Copilot is unavailable right now', '智能助手目前不可用')
              : parseFailed ? t('A few details are missing', '还缺少一些信息') : t('Parsing failed', '解析失败')}
            action={
              <Button size="sm" variant="secondary" onClick={() => { setManual(true); setParseError(null); }}>
                {t('Enter it manually', '手动输入')}
              </Button>
            }
          >
            {aiDown
              ? t('The parser is offline. Enter the transaction manually — nothing else is affected.', '解析服务当前离线，您可以手动输入交易，其他功能不受影响。')
              : /* The parser names exactly which fields it still needs — show that,
                   not a generic hint, so the user can just add them and retry. */
                parseError.message}
          </Banner>
        )}

        {draft && !editing && (
          <ParsedTransactionCard
            source={sourceText ?? undefined}
            confidence={confidence ? CONFIDENCE_LABEL[confidence] : undefined}
            fields={[
              { key: t('Action', '操作'), value: draft.side === 'BUY' ? t('Buy', '买入') : t('Sell', '卖出') },
              { key: t('Symbol', '代码'), value: draft.ticker },
              { key: t('Quantity', '数量'), value: quantity(draft.quantity) },
              { key: t('Price', '价格'), value: money(draft.price) },
              { key: t('Date', '日期'), value: tradeDate(draft.tradeDate) },
              { key: t('Total', '总额'), value: money(draft.quantity * draft.price) },
            ]}
            confirmLabel={saving
              ? t('Saving…', '正在保存…')
              : draft.transactionId ? t('Confirm & update', '确认并更新') : t('Confirm & save', '确认并保存')}
            onConfirm={confirmDraft}
            onEdit={() => setEditing(true)}
            onDiscard={() => { setDraft(null); setWarnings([]); setSourceText(null); }}
          />
        )}

        {warnings.length > 0 && draft && !editing && (
          <Banner tone="info" title={t('How Copilot read that', '智能助手的解析方式')}>
            <ul style={{ margin: 0, paddingLeft: 'var(--space-6)' }}>
              {warnings.map((w, i) => <li key={i}>{w}</li>)}
            </ul>
          </Banner>
        )}

        {(editing && draft) && (
          <Card title={t('Review and save', '核对并保存')} subtitle={t('Pre-filled by Copilot — check every field.', '智能助手已预填，请检查每个字段。')}>
            <TransactionForm
              initial={valuesFromDraft(draft)}
              source="AI_ASSISTED"
              editingId={draft.transactionId ?? undefined}
              submitLabel={draft.transactionId ? t('Confirm & update', '确认并更新') : t('Confirm & save', '确认并保存')}
              onSaved={onSaved}
              onCancel={() => setEditing(false)}
            />
          </Card>
        )}

        {manual && !draft && (
          <Card title={t('Enter manually', '手动输入')} subtitle={t('No AI involved.', '不使用 AI。')}>
            <TransactionForm
              initial={emptyValues()}
              source="MANUAL"
              onSaved={onSaved}
              onCancel={() => setManual(false)}
            />
          </Card>
        )}

        {saved && (
          <Banner
            tone="gain"
            title={t('Transaction saved', '交易已保存')}
            action={<Button size="sm" variant="secondary" onClick={() => navigate('/transactions')}>{t('View all', '查看全部')}</Button>}
          >
            {locale === 'zh-CN'
              ? `${saved.side === 'BUY' ? '已买入' : '已卖出'} ${quantity(saved.quantity)} 股 ${saved.ticker}，价格 ${money(saved.price)}，日期 ${tradeDate(saved.tradeDate)}。成本基础和损益已重新计算。`
              : `${saved.side === 'BUY' ? 'Bought' : 'Sold'} ${quantity(saved.quantity)} ${saved.ticker} at ${money(saved.price)} on ${tradeDate(saved.tradeDate)}. Cost basis and P&L recalculated.`}
          </Banner>
        )}
      </div>

      <div className="stack">
        <Banner tone="info" title={t('How parsing works', '解析方式')}>
          {t(
            'Copilot extracts the fields from your sentence and pre-fills the form. It never writes to your portfolio on its own — the transaction is saved only when you confirm.',
            '智能助手会从您的句子中提取字段并预填表单。它绝不会自行写入投资组合，只有您确认后交易才会保存。',
          )}
        </Banner>

        {!manual && !draft && (
          <Card title={t('Prefer to type it in?', '想要手动输入？')} subtitle={t('The manual form is always available.', '您随时可以使用手动表单。')}>
            <Button variant="secondary" fullWidth iconLeft="pencil" onClick={() => setManual(true)}>
              {t('Enter manually', '手动输入')}
            </Button>
          </Card>
        )}
      </div>
    </div>
  );
}
