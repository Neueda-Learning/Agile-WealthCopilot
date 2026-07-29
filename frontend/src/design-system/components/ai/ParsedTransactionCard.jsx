import React from 'react';
import { Icon } from '../core/Icon.jsx';
import { Badge } from '../core/Badge.jsx';
import { Button } from '../core/Button.jsx';

export function ParsedTransactionCard({
  fields = [], source, confidence, onConfirm, onEdit, onDiscard,
  confirmLabel = 'Confirm & save', className = '', ...rest
}) {
  const zh = document.documentElement.lang === 'zh-CN';
  return (
    <div className={'wc-parsed ' + className} {...rest}>
      <div className="wc-parsed__head">
        <Icon name="sparkles" size={14} />
        {zh ? '已从文本解析，请在保存前核对' : 'Parsed from your text — review before saving'}
        {confidence ? (
          <Badge tone={confidence === 'low' ? 'caution' : 'brand'}>
            {zh
              ? `${confidence === 'high' ? '高' : confidence === 'medium' ? '中' : '低'}置信度`
              : `${confidence} confidence`}
          </Badge>
        ) : null}
      </div>
      {source ? <div className="wc-parsed__source">“{source}”</div> : null}
      <div className="wc-parsed__grid">
        {fields.map(function (fl) {
          return (
            <div className="wc-parsed__field" key={fl.key}>
              <span className="wc-parsed__key">{fl.key}</span>
              <span className="wc-parsed__val">{fl.value}</span>
            </div>
          );
        })}
      </div>
      <div className="wc-parsed__foot">
        <Button size="sm" onClick={onConfirm}>{confirmLabel}</Button>
        <Button size="sm" variant="secondary" iconLeft="pencil" onClick={onEdit}>{zh ? '编辑' : 'Edit'}</Button>
        <span style={{ flex: 1 }} />
        <Button size="sm" variant="ghost" onClick={onDiscard}>{zh ? '放弃' : 'Discard'}</Button>
      </div>
    </div>
  );
}
