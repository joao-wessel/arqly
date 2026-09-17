import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../core/auth/auth.models';

const HOME_URL = '/api/tenant/home';

export interface HomeAttentionItem {
  type: string;
  severity: 'LOW' | 'NORMAL' | 'HIGH' | 'CRITICAL' | string;
  title: string;
  description?: string | null;
  projectId?: string | null;
  projectName?: string | null;
  sourceId?: string | null;
  actionUrl?: string | null;
  dueDate?: string | null;
}

export interface HomeTodayItem {
  id: string;
  sourceType: string;
  sourceId: string;
  title: string;
  start: string;
  end?: string | null;
  allDay: boolean;
  type?: string | null;
  status?: string | null;
  projectId?: string | null;
  projectName?: string | null;
  responsibleUserName?: string | null;
  sourceUrl?: string | null;
  color?: string | null;
}

export interface HomeProject {
  id: string;
  code: string;
  name: string;
  clientName: string;
  status: string;
  completionPercentage?: number | null;
  updatedAt?: string | null;
  responsible: boolean;
  manager: boolean;
  stageResponsible: boolean;
}

export interface HomeIndicators {
  activeProjects: number;
  inProgressStages: number;
  overdueStages: number;
  pendingApprovals: number;
}

export interface HomeFinancialSummary {
  expectedIncome: number;
  actualIncome: number;
  expectedExpense: number;
  actualExpense: number;
  expectedBalance: number;
  actualBalance: number;
}

export interface HomeActivity {
  id: string;
  title: string;
  description?: string | null;
  authorName?: string | null;
  type: string;
  projectId?: string | null;
  projectName?: string | null;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class HomeApiService {
  private readonly http = inject(HttpClient);

  attention(): Observable<ApiResponse<HomeAttentionItem[]>> {
    return this.http.get<ApiResponse<HomeAttentionItem[]>>(`${HOME_URL}/attention`);
  }

  today(): Observable<ApiResponse<HomeTodayItem[]>> {
    return this.http.get<ApiResponse<HomeTodayItem[]>>(`${HOME_URL}/today`);
  }

  projects(): Observable<ApiResponse<HomeProject[]>> {
    return this.http.get<ApiResponse<HomeProject[]>>(`${HOME_URL}/projects`);
  }

  indicators(): Observable<ApiResponse<HomeIndicators>> {
    return this.http.get<ApiResponse<HomeIndicators>>(`${HOME_URL}/indicators`);
  }

  financialSummary(): Observable<ApiResponse<HomeFinancialSummary>> {
    return this.http.get<ApiResponse<HomeFinancialSummary>>(`${HOME_URL}/financial-summary`);
  }

  activities(): Observable<ApiResponse<HomeActivity[]>> {
    return this.http.get<ApiResponse<HomeActivity[]>>(`${HOME_URL}/activities`);
  }
}
