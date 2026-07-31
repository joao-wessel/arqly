export function renderMarkdown(markdown: string): string {
  const escaped = (markdown || '')
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/\[([^\]]+)]\((https?:\/\/[^\s)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>');

  const lines = escaped.split('\n');
  let inList = false;
  const html: string[] = [];
  for (const line of lines) {
    if (line.startsWith('- ')) {
      if (!inList) { html.push('<ul>'); inList = true; }
      html.push(`<li>${line.slice(2)}</li>`);
      continue;
    }
    if (inList) { html.push('</ul>'); inList = false; }
    if (line.startsWith('### ')) html.push(`<h3>${line.slice(4)}</h3>`);
    else if (line.startsWith('## ')) html.push(`<h2>${line.slice(3)}</h2>`);
    else if (line.startsWith('# ')) html.push(`<h1>${line.slice(2)}</h1>`);
    else if (!line.trim()) html.push('<br>');
    else html.push(`<p>${line}</p>`);
  }
  if (inList) html.push('</ul>');
  return html.join('');
}
