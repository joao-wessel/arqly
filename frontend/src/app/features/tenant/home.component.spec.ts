import { ComponentFixture, TestBed } from '@angular/core/testing';
import { importProvidersFrom } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, Subject, of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { Activity, ArrowUpRight, CalendarCheck2, CalendarDays, CalendarX2, ChevronRight, CircleAlert, CircleCheck, CircleCheckBig, Clock3, FolderClock, FolderKanban, FolderOpen, ListTodo, LucideAngularModule, OctagonAlert, ReceiptText, TriangleAlert, WalletCards } from 'lucide-angular';
import { AuthService } from '../../core/auth/auth.service';
import {
  HomeActivity,
  HomeApiService,
  HomeAttentionItem,
  HomeFinancialSummary,
  HomeIndicators,
  HomeProject,
  HomeTodayItem
} from './home-api.service';
import { HomePageComponent } from './home.component';

interface HomeApiMock {
  attention: () => Observable<{ data: HomeAttentionItem[] }>;
  today: () => Observable<{ data: HomeTodayItem[] }>;
  projects: () => Observable<{ data: HomeProject[] }>;
  indicators: () => Observable<{ data: HomeIndicators }>;
  financialSummary: () => Observable<{ data: HomeFinancialSummary }>;
  activities: () => Observable<{ data: HomeActivity[] }>;
}

const emptyApi = (): HomeApiMock => ({
  attention: () => of({ data: [] }),
  today: () => of({ data: [] }),
  projects: () => of({ data: [] }),
  indicators: () => of({ data: { activeProjects: 0, inProgressStages: 0, overdueStages: 0, pendingApprovals: 0 } }),
  financialSummary: () => throwError(() => new Error('Forbidden')),
  activities: () => of({ data: [] })
});

describe('HomePageComponent', () => {
  let fixture: ComponentFixture<HomePageComponent>;
  let component: HomePageComponent;
  let router: { navigateByUrl: ReturnType<typeof vi.fn> };

  async function create(api: HomeApiMock): Promise<void> {
    router = { navigateByUrl: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [HomePageComponent],
      providers: [
        { provide: HomeApiService, useValue: api },
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: { currentUser: () => ({ name: 'João Wessel' }) } },
        importProvidersFrom(LucideAngularModule.pick({ Activity, ArrowUpRight, CalendarCheck2, CalendarDays, CalendarX2, ChevronRight, CircleAlert, CircleCheck, CircleCheckBig, Clock3, FolderClock, FolderKanban, FolderOpen, ListTodo, OctagonAlert, ReceiptText, TriangleAlert, WalletCards }))
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(HomePageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('loads the independent sections and hides finance when it is not authorized', async () => {
    await create(emptyApi());

    const content = fixture.nativeElement.textContent as string;
    expect(content).toContain('João');
    expect(content).toContain('Nenhuma pendência importante.');
    expect(content).toContain('Nenhum compromisso para hoje.');
    expect(content).toContain('Nenhum projeto relacionado a você.');
    expect(content).toContain('Nenhuma atividade recente.');
    expect(content).not.toContain('Resumo Financeiro');
  });

  it('keeps other sections available when attention fails', async () => {
    const api = emptyApi();
    api.attention = () => throwError(() => new Error('Unavailable'));
    api.projects = () => of({ data: [{ id: 'project-1', code: 'PRJ-001', name: 'Residência Silva', clientName: 'Cliente Silva', status: 'IN_PROGRESS', completionPercentage: 55, updatedAt: new Date().toISOString(), responsible: true, manager: false, stageResponsible: false }] });
    await create(api);

    const content = fixture.nativeElement.textContent as string;
    expect(content).toContain('Não foi possível carregar esta seção.');
    expect(content).toContain('Residência Silva');
  });

  it('renders a loading state for a section while its request is pending', async () => {
    const api = emptyApi();
    const attention = new Subject<{ data: HomeAttentionItem[] }>();
    api.attention = () => attention.asObservable();
    await create(api);

    expect(component.attention().loading).toBe(true);
    expect(fixture.nativeElement.querySelector('.animate-pulse')).not.toBeNull();
  });

  it('navigates through an attention action URL', async () => {
    const api = emptyApi();
    api.attention = () => of({ data: [{ type: 'OVERDUE_STAGE', severity: 'HIGH', title: 'Etapa atrasada', actionUrl: '/app/projects/project-1/stages/stage-1' }] });
    await create(api);

    const button = fixture.nativeElement.querySelector('section.card button') as HTMLButtonElement;
    button.click();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/app/projects/project-1/stages/stage-1');
  });

  it('navigates to the project from a project card', async () => {
    const api = emptyApi();
    api.projects = () => of({ data: [{ id: 'project-1', code: 'PRJ-001', name: 'Residência Silva', clientName: 'Cliente Silva', status: 'IN_PROGRESS', completionPercentage: 55, updatedAt: new Date().toISOString(), responsible: true, manager: false, stageResponsible: false }] });
    await create(api);

    component.open('/app/projects/project-1');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/app/projects/project-1');
  });
});
