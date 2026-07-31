import { Component, Input } from '@angular/core';
import { renderMarkdown } from './markdown.utils';

@Component({
  selector: 'app-markdown-preview',
  standalone: true,
  template: `<article class="markdown-document" [innerHTML]="renderMarkdown(content)"></article>`
})
export class MarkdownPreviewComponent {
  @Input() content = '';
  readonly renderMarkdown = renderMarkdown;
}
