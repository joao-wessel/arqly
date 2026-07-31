import { Component, Input, OnDestroy, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Editor, NgxEditorModule, Toolbar } from 'ngx-editor';
import TurndownService from 'turndown';
import { renderMarkdown } from './markdown.utils';

@Component({
  selector: 'app-markdown-editor',
  standalone: true,
  imports: [FormsModule, NgxEditorModule],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => MarkdownEditorComponent),
    multi: true
  }],
  template: `
    <div class="arqly-rich-editor NgxEditor__Wrapper" [style.--editor-min-height]="minHeight + 'px'"
         (click)="preventToolbarSubmit($event)">
      <ngx-editor-menu [editor]="editor" [toolbar]="toolbar" [disabled]="disabled" />
      <ngx-editor [editor]="editor" [ngModel]="html" [ngModelOptions]="{ standalone: true }"
                  [disabled]="disabled" [placeholder]="placeholder" outputFormat="html"
                  (ngModelChange)="contentChanged($event)" (focusOut)="onTouched()" />
    </div>
  `,
  styles: [`
    :host { display: block; }
    :host ::ng-deep .arqly-rich-editor {
      --ngx-editor-border-radius: 1rem;
      --ngx-editor-background-color: var(--color-panel);
      --ngx-editor-text-color: var(--color-heading);
      --ngx-editor-placeholder-color: #94a3b8;
      --ngx-editor-border-color: rgb(226 232 240);
      --ngx-editor-wrapper-border-color: rgb(226 232 240);
      --ngx-editor-menubar-bg-color: rgb(248 250 252 / 0.8);
      --ngx-editor-menubar-padding: 0.5rem;
      --ngx-editor-menubar-height: 2.75rem;
      --ngx-editor-icon-size: 2.5rem;
      --ngx-editor-menu-item-border-radius: 0.75rem;
      --ngx-editor-menu-item-active-color: rgb(var(--arqly-700));
      --ngx-editor-menu-item-active-bg-color: rgb(var(--arqly-100));
      --ngx-editor-menu-item-hover-bg-color: rgb(var(--arqly-50));
      --ngx-editor-focus-ring-color: rgb(var(--arqly-500));
      overflow: visible;
      background: var(--color-panel);
      transition: border-color 150ms ease, box-shadow 150ms ease;
    }
    :host ::ng-deep .arqly-rich-editor:focus-within {
      border-color: rgb(var(--arqly-500));
      box-shadow: 0 0 0 4px rgb(var(--arqly-100));
    }
    :host ::ng-deep .arqly-rich-editor .NgxEditor__Content {
      min-height: var(--editor-min-height);
      max-height: 32rem;
      overflow-y: auto;
      padding: 1rem;
      font-family: inherit;
      font-size: 0.875rem;
      line-height: 1.65;
    }
    :host ::ng-deep .arqly-rich-editor .NgxEditor__Content ul {
      margin: 0.75rem 0;
      padding-left: 1.5rem;
      list-style: disc;
    }
    :host ::ng-deep .arqly-rich-editor .NgxEditor__Content a {
      color: rgb(var(--arqly-700));
      font-weight: 700;
      text-decoration: underline;
    }
    :host ::ng-deep .arqly-rich-editor .NgxEditor__Popup {
      z-index: 80;
      border: 1px solid rgb(226 232 240);
      color: var(--color-heading);
    }
  `]
})
export class MarkdownEditorComponent implements ControlValueAccessor, OnDestroy {
  @Input() placeholder = '';
  @Input() minHeight = 160;

  readonly editor = new Editor({ history: true, keyboardShortcuts: true, inputRules: true });
  readonly toolbar: Toolbar = [['bold', 'italic'], ['bullet_list'], ['link']];
  private readonly turndown = new TurndownService({ bulletListMarker: '-', emDelimiter: '*', strongDelimiter: '**' });
  html = '';
  disabled = false;
  private onChange: (value: string) => void = () => undefined;
  onTouched: () => void = () => undefined;

  writeValue(value: string | null): void {
    const nextHtml = renderMarkdown(value || '');
    if (nextHtml !== this.html) this.html = nextHtml;
  }

  registerOnChange(fn: (value: string) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
  setDisabledState(disabled: boolean): void { this.disabled = disabled; }

  contentChanged(html: string) {
    this.html = html || '';
    this.onChange(this.html ? this.turndown.turndown(this.html).trim() : '');
  }

  insertText(text: string) {
    this.editor.commands.focus().insertText(text).exec();
  }

  preventToolbarSubmit(event: MouseEvent) {
    const target = event.target as HTMLElement;
    const toolbarButton = target.closest('.NgxEditor__MenuBar button');
    if (toolbarButton && !target.closest('.NgxEditor__Popup')) event.preventDefault();
  }

  ngOnDestroy() {
    this.editor.destroy();
  }
}
