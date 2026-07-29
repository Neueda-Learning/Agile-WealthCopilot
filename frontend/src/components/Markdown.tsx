import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

/**
 * Assistant replies arrive as markdown — bold, lists, and GFM tables (the agent
 * likes tabulating holdings). Rendered through react-markdown, which escapes
 * raw HTML by default: model output is untrusted text and must never be able to
 * inject markup into the page.
 */
export default function Markdown({ children }: { children: string }) {
  return (
    <div className="md">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        components={{
          // News answers cite sources; keep the app in place when they're opened.
          a: (props) => <a {...props} target="_blank" rel="noopener noreferrer" />,
        }}
      >
        {children}
      </ReactMarkdown>
    </div>
  );
}
