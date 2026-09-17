import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { importProvidersFrom } from '@angular/core';
import { Router } from '@angular/router';
import { ArrowRight, ArrowUpRight, ClipboardList, File, FileText, FolderKanban, ListTodo, LucideAngularModule, Search, UserRound } from 'lucide-angular';
import { Observable, Subject, of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { GlobalSearchComponent } from './global-search.component';

describe('GlobalSearchComponent', () => {
  let fixture: ComponentFixture<GlobalSearchComponent>;
  let component: GlobalSearchComponent;
  let get: ReturnType<typeof vi.fn>;
  let router: { navigateByUrl: ReturnType<typeof vi.fn>; navigate: ReturnType<typeof vi.fn> };

  async function create(response: Observable<unknown> = of({ data: { results: [], hasMore: false } })) {
    get = vi.fn(() => response);
    router = { navigateByUrl: vi.fn(), navigate: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [GlobalSearchComponent],
      providers: [
        { provide: HttpClient, useValue: { get } },
        { provide: Router, useValue: router },
        importProvidersFrom(LucideAngularModule.pick({ ArrowRight, ArrowUpRight, ClipboardList, File, FileText, FolderKanban, ListTodo, Search, UserRound }))
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(GlobalSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function search(value: string) {
    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('debounces a query and renders grouped results', async () => {
    vi.useFakeTimers();
    await create(of({ data: { results: [
      { id: 'client-1', type: 'CLIENT', title: 'João Silva', subtitle: 'joao@email.com', actionUrl: '/app/clients', icon: 'UserRound' },
      { id: 'project-1', type: 'PROJECT', title: 'Residência Silva', subtitle: 'João Silva', actionUrl: '/app/projects/project-1', icon: 'FolderKanban' }
    ], hasMore: true } }));

    search('silva');
    expect(get).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(300);
    fixture.detectChanges();

    expect(get).toHaveBeenCalledOnce();
    expect(fixture.nativeElement.textContent).toContain('Clientes');
    expect(fixture.nativeElement.textContent).toContain('Projetos');
    expect(fixture.nativeElement.textContent).toContain('Residência Silva');
    expect(fixture.nativeElement.textContent).toContain('Ver todos os resultados');
    vi.useRealTimers();
  });

  it('cancels the previous request when a newer query is emitted', async () => {
    vi.useFakeTimers();
    const first = new Subject<unknown>();
    const second = new Subject<unknown>();
    get = vi.fn().mockReturnValueOnce(first).mockReturnValueOnce(second);
    await TestBed.configureTestingModule({
      imports: [GlobalSearchComponent],
      providers: [
        { provide: HttpClient, useValue: { get } },
        { provide: Router, useValue: { navigateByUrl: vi.fn(), navigate: vi.fn() } },
        importProvidersFrom(LucideAngularModule.pick({ ArrowRight, ArrowUpRight, ClipboardList, File, FileText, FolderKanban, ListTodo, Search, UserRound }))
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(GlobalSearchComponent); component = fixture.componentInstance; fixture.detectChanges();

    search('silva'); await vi.advanceTimersByTimeAsync(300);
    search('executivo'); await vi.advanceTimersByTimeAsync(300);
    first.next({ data: { results: [{ id: 'old', type: 'PROJECT', title: 'Antigo', actionUrl: '/app/old', icon: 'FolderKanban' }], hasMore: false } });
    second.next({ data: { results: [{ id: 'new', type: 'PROJECT_STAGE', title: 'Projeto Executivo', actionUrl: '/app/new', icon: 'ListTodo' }], hasMore: false } });
    fixture.detectChanges();

    expect(component.results().map(item => item.id)).toEqual(['new']);
    vi.useRealTimers();
  });

  it('supports keyboard selection, enter, escape and the global shortcut', async () => {
    vi.useFakeTimers();
    await create(of({ data: { results: [
      { id: 'one', type: 'PROJECT', title: 'Residência Silva', actionUrl: '/app/projects/one', icon: 'FolderKanban' },
      { id: 'two', type: 'PROJECT', title: 'Residência Verde', actionUrl: '/app/projects/two', icon: 'FolderKanban' }
    ], hasMore: false } }));
    search('res'); await vi.advanceTimersByTimeAsync(300); fixture.detectChanges();

    component.onKeydown(new KeyboardEvent('keydown', { key: 'ArrowDown' }));
    component.onKeydown(new KeyboardEvent('keydown', { key: 'Enter' }));
    expect(router.navigateByUrl).toHaveBeenCalledWith('/app/projects/two');

    component.show(); component.onKeydown(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(component.open()).toBe(false);
    component.keyboardShortcut(new KeyboardEvent('keydown', { key: 'k', ctrlKey: true }));
    expect(component.open()).toBe(true);
    vi.useRealTimers();
  });

  it('keeps search failures isolated and shows an empty state when no result exists', async () => {
    vi.useFakeTimers();
    await create(of({ data: { results: [], hasMore: false } }));
    search('xyz'); await vi.advanceTimersByTimeAsync(300); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Nenhum resultado para “xyz”.');
    vi.useRealTimers();
  });

  it('shows a discreet failure state', async () => {
    vi.useFakeTimers();
    await create(throwError(() => new Error('network')));
    search('silva'); await vi.advanceTimersByTimeAsync(300); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Não foi possível pesquisar agora.');
    vi.useRealTimers();
  });
});
