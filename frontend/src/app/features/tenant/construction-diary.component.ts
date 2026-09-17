import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ApiResponse } from '../../core/auth/auth.models';
import { ArqlyDatePickerComponent } from '../../shared/components/arqly-date-picker.component';
import { ArqlySelectComponent } from '../../shared/components/arqly-select.component';
import { ToastService } from '../../shared/components/toast/toast.service';
import { FileExplorerComponent } from '../../shared/files/file-explorer.component';

interface Page<T> { content: T[]; totalElements: number; totalPages: number; }
interface Project { id: string; name: string; code: string; clientName: string; }
interface Stage { id: string; name: string; }
interface DiarySummary { id: string; projectId: string; projectName: string; title: string; entryType: string; status: string; entryDate: string; responsibleName?: string; occurrenceCount: number; photoCount: number; visibility: string; nextVisitDate?: string; }
interface Diary extends DiarySummary { summary?: string; location?: string; startTime?: string; endTime?: string; weatherCondition?: string; temperature?: number; responsibleUserId?: string; nextVisitNotes?: string; stageIds: string[]; stageNames: string[]; revisionNumber: number; participants: any[]; observations: any[]; occurrences: any[]; decisions: any[]; instructions: any[]; }
interface User { id: string; name: string; tenantAdmin: boolean; }

@Component({
  selector: 'app-construction-diary', standalone: true,
  imports: [DatePipe, FormsModule, ReactiveFormsModule, RouterLink, LucideAngularModule, ArqlySelectComponent, ArqlyDatePickerComponent, FileExplorerComponent],
  template: `
  @if (!editing()) {
    <section class="space-y-5">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div><p class="text-xs font-extrabold uppercase tracking-[0.2em] text-arqly-700">Acompanhamento da obra</p><h2 class="mt-1 text-3xl font-extrabold">Diário de Obra</h2><p class="mt-2 text-slate-500">Visitas, decisões, ocorrências e evolução da execução em um único lugar.</p></div>
        <a class="btn-primary" routerLink="/app/construction-diary/new"><lucide-icon name="Plus" size="18"/>Novo registro</a>
      </div>
      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <div class="card p-5"><p class="text-xs font-bold text-slate-400">REGISTROS</p><strong class="mt-2 block text-3xl">{{ stats()?.entries || 0 }}</strong></div>
        <div class="card p-5"><p class="text-xs font-bold text-slate-400">OCORRÊNCIAS ABERTAS</p><strong class="mt-2 block text-3xl text-amber-600">{{ stats()?.openOccurrences || 0 }}</strong></div>
        <div class="card p-5"><p class="text-xs font-bold text-slate-400">CRÍTICAS</p><strong class="mt-2 block text-3xl text-red-600">{{ stats()?.criticalOccurrences || 0 }}</strong></div>
        <div class="card p-5"><p class="text-xs font-bold text-slate-400">ÚLTIMA VISITA</p><strong class="mt-2 block text-lg">{{ stats()?.lastVisit ? (stats()!.lastVisit | date:'dd/MM/yyyy') : '-' }}</strong></div>
        <div class="card p-5"><p class="text-xs font-bold text-slate-400">PRÓXIMA VISITA</p><strong class="mt-2 block text-lg">{{ stats()?.nextVisit ? (stats()!.nextVisit | date:'dd/MM/yyyy') : '-' }}</strong></div>
      </div>
      <div class="card p-4"><div class="grid gap-3 md:grid-cols-[1fr_15rem_15rem_auto]"><input class="field" [value]="search()" (input)="search.set($any($event.target).value)" placeholder="Buscar título ou resumo"/><app-arqly-select [ngModel]="statusFilter()" (ngModelChange)="statusFilter.set($event)" placeholder="Status" [options]="statusOptions"/><app-arqly-select [ngModel]="typeFilter()" (ngModelChange)="typeFilter.set($event)" placeholder="Tipo" [options]="typeOptions"/><button class="btn-secondary" (click)="loadList()"><lucide-icon name="Search" size="17"/>Pesquisar</button></div></div>
      <div class="grid gap-4 lg:grid-cols-2">@for (entry of entries(); track entry.id) {<a class="card block p-5 transition hover:border-arqly-300 hover:shadow-lg" [routerLink]="['/app/construction-diary', entry.id]"><div class="flex items-start justify-between gap-3"><div><p class="text-xs font-extrabold uppercase tracking-[0.18em] text-arqly-700">{{ typeLabel(entry.entryType) }} · {{ entry.entryDate | date:'dd/MM/yyyy' }}</p><h3 class="mt-2 text-lg font-extrabold">{{ entry.title }}</h3><p class="mt-1 text-sm text-slate-500">{{ entry.projectName }} · {{ entry.responsibleName || 'Sem responsável' }}</p></div><span class="rounded-full px-3 py-1 text-xs font-bold" [class]="statusClass(entry.status)">{{ statusLabel(entry.status) }}</span></div><div class="mt-5 flex flex-wrap gap-2 text-xs font-bold text-slate-500"><span class="rounded-full bg-slate-100 px-3 py-1">{{ entry.occurrenceCount }} ocorrência(s)</span><span class="rounded-full bg-slate-100 px-3 py-1">{{ entry.photoCount }} foto(s)</span><span class="rounded-full px-3 py-1" [class.bg-arqly-50]="entry.visibility === 'CLIENT_VISIBLE'" [class.text-arqly-700]="entry.visibility === 'CLIENT_VISIBLE'">{{ entry.visibility === 'CLIENT_VISIBLE' ? 'Cliente visível' : 'Interno' }}</span></div></a>} @empty {<div class="card col-span-full py-16 text-center text-slate-500">Nenhum registro encontrado.</div>}</div>
    </section>
  } @else {
    <section class="space-y-5"><div class="flex flex-wrap items-center justify-between gap-3"><div><a class="inline-flex items-center gap-2 text-sm font-bold text-arqly-700" routerLink="/app/construction-diary"><lucide-icon name="ArrowLeft" size="16"/>Voltar ao Diário</a><p class="mt-4 text-xs font-extrabold uppercase tracking-[.2em] text-arqly-700">Diário de Obra</p><h2 class="mt-1 text-3xl font-extrabold">{{ diaryId() === 'new' ? 'Novo registro' : entry()?.title || 'Carregando registro' }}</h2></div><div class="flex gap-2">@if(entry() && entry()!.status === 'DRAFT'){<button class="btn-primary" (click)="publish()"><lucide-icon name="Send" size="17"/>Publicar registro</button>}</div></div>
      <form class="grid gap-5 xl:grid-cols-[1fr_20rem]" [formGroup]="form" (ngSubmit)="save()"><div class="space-y-5"><div class="card p-5"><p class="text-xs font-extrabold uppercase tracking-[.18em] text-arqly-700">Informações da visita</p><div class="mt-4 grid gap-4 md:grid-cols-2"><label class="space-y-1 md:col-span-2"><span class="text-xs font-bold text-slate-500">Título <span class="text-red-500">*</span></span><input class="field" formControlName="title" placeholder="Ex.: Visita técnica - revestimentos"/></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Projeto <span class="text-red-500">*</span></span><app-arqly-select formControlName="projectId" placeholder="Selecione o projeto" [options]="projectOptions()" panelMode="fixed"/></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Tipo <span class="text-red-500">*</span></span><app-arqly-select formControlName="entryType" [options]="typeOptions.slice(1)" panelMode="fixed"/></label><div class="space-y-1"><span class="text-xs font-bold text-slate-500">Data <span class="text-red-500">*</span></span><app-arqly-date-picker formControlName="entryDate" placeholder="Selecione a data"/></div><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Local</span><input class="field" formControlName="location" placeholder="Ex.: Obra - residência"/></label><div class="space-y-1"><span class="text-xs font-bold text-slate-500">Início</span><app-arqly-select formControlName="startTime" placeholder="Selecione o horário" [options]="timeOptions" panelMode="fixed"/></div><div class="space-y-1"><span class="text-xs font-bold text-slate-500">Fim</span><app-arqly-select formControlName="endTime" placeholder="Selecione o horário" [options]="timeOptions" panelMode="fixed"/></div><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Responsável</span><app-arqly-select formControlName="responsibleUserId" placeholder="Selecione" [options]="userOptions()" panelMode="fixed"/></label><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Visibilidade</span><app-arqly-select formControlName="visibility" [options]="visibilityOptions" panelMode="fixed"/></label><label class="space-y-1 md:col-span-2"><span class="text-xs font-bold text-slate-500">Resumo <span class="text-red-500">*</span></span><textarea class="field min-h-32" formControlName="summary" placeholder="Descreva a evolução, condições do local e principais pontos da visita."></textarea></label></div></div>
        <div class="card p-5"><div class="flex items-center justify-between"><p class="text-xs font-extrabold uppercase tracking-[.18em] text-arqly-700">Etapas relacionadas</p><span class="text-xs text-slate-400">Vincule as etapas tratadas neste registro.</span></div><div class="mt-4 grid gap-2 md:grid-cols-2">@for(stage of stages();track stage.id){<label class="flex cursor-pointer items-center gap-3 rounded-xl border p-3 transition hover:border-arqly-300 hover:bg-arqly-50" [class.border-arqly-300]="selectedStages().includes(stage.id)" [class.bg-arqly-50]="selectedStages().includes(stage.id)" [class.border-slate-200]="!selectedStages().includes(stage.id)"><input class="checkbox" type="checkbox" [checked]="selectedStages().includes(stage.id)" (change)="toggleStage(stage.id,$any($event.target).checked)"/><span class="font-semibold">{{stage.name}}</span></label>} @empty {<p class="text-sm text-slate-500">Selecione um projeto para carregar suas etapas.</p>}</div></div>
        <div class="card p-5"><p class="text-xs font-extrabold uppercase tracking-[.18em] text-arqly-700">Próxima visita</p><div class="mt-4 grid gap-4 md:grid-cols-2"><div class="space-y-1"><span class="text-xs font-bold text-slate-500">Data prevista</span><app-arqly-date-picker formControlName="nextVisitDate" placeholder="Opcional"/></div><label class="space-y-1"><span class="text-xs font-bold text-slate-500">Observações</span><input class="field" formControlName="nextVisitNotes" placeholder="O que deve ser acompanhado na próxima visita"/></label></div></div>
        @if(entry()){<div class="card p-5"><p class="text-xs font-extrabold uppercase tracking-[.18em] text-arqly-700">Fotos e arquivos</p><p class="mt-2 text-sm text-slate-500">Os envios são gravados no acervo do escritório. Para compartilhar no Portal, abra os detalhes do arquivo e altere a visibilidade para <strong>Visível ao cliente</strong>.</p><div class="mt-4"><app-file-explorer ownerType="CONSTRUCTION_DIARY_ENTRY" [ownerId]="entry()!.id" title="Arquivos do registro" eyebrow="Diário de Obra"/></div></div>}</div>
        <aside class="space-y-4"><div class="card p-5"><p class="text-sm font-extrabold">Publicação</p><p class="mt-2 text-sm text-slate-500">Rascunhos podem ser ajustados livremente. Registros publicados mantêm o número de revisão.</p><span class="mt-4 inline-flex rounded-full px-3 py-1 text-xs font-bold" [class]="statusClass(entry()?.status || 'DRAFT')">{{ statusLabel(entry()?.status || 'DRAFT') }}</span>@if(entry()){<p class="mt-4 text-xs text-slate-400">Revisão {{ entry()!.revisionNumber }}</p>}</div><button class="btn-primary w-full" type="submit"><lucide-icon name="Save" size="17"/>Salvar rascunho</button></aside></form>
    </section>
  }`
})
export class ConstructionDiaryComponent implements OnInit {
  private readonly http=inject(HttpClient); private readonly fb=inject(FormBuilder); private readonly route=inject(ActivatedRoute); private readonly router=inject(Router); private readonly toast=inject(ToastService); private readonly base='/api/tenant/construction-diary';
  readonly editing=signal(false); readonly diaryId=signal(''); readonly entries=signal<DiarySummary[]>([]); readonly entry=signal<Diary|null>(null); readonly stats=signal<any>(null); readonly projects=signal<Project[]>([]); readonly stages=signal<Stage[]>([]); readonly users=signal<User[]>([]); readonly selectedStages=signal<string[]>([]); readonly search=signal(''); readonly statusFilter=signal(''); readonly typeFilter=signal('');
  readonly statusOptions=[{label:'Todos os status',value:''},{label:'Rascunho',value:'DRAFT'},{label:'Publicado',value:'PUBLISHED'},{label:'Arquivado',value:'ARCHIVED'}]; readonly typeOptions=[{label:'Todos os tipos',value:''},{label:'Visita',value:'VISIT'},{label:'Acompanhamento',value:'FOLLOW_UP'},{label:'Inspeção',value:'INSPECTION'},{label:'Reunião',value:'MEETING'},{label:'Ocorrência',value:'OCCURRENCE'},{label:'Outro',value:'OTHER'}]; readonly visibilityOptions=[{label:'Interno',value:'INTERNAL'},{label:'Visível para o cliente após publicar',value:'CLIENT_VISIBLE'}];
  readonly timeOptions=Array.from({length:96},(_,index)=>{const value=`${String(Math.floor(index/4)).padStart(2,'0')}:${String((index%4)*15).padStart(2,'0')}`;return {label:value,value};});
  readonly form=this.fb.nonNullable.group({projectId:['',Validators.required],title:['',Validators.required],entryType:['VISIT'],entryDate:[new Date().toISOString().slice(0,10),Validators.required],startTime:[''],endTime:[''],location:[''],responsibleUserId:[''],summary:['',Validators.required],visibility:['INTERNAL'],nextVisitDate:[''],nextVisitNotes:['']});
  ngOnInit(){
    this.form.controls.projectId.valueChanges.subscribe(projectId=>{
      this.stages.set([]);
      this.selectedStages.set([]);
      if(projectId)this.loadStages(projectId);
    });
    this.route.queryParamMap.subscribe(params=>{
      if(this.route.snapshot.routeConfig?.path !== 'construction-diary/new') return;
      const projectId=params.get('projectId');
      if(projectId) this.form.controls.projectId.setValue(projectId);
    });
    this.route.paramMap.subscribe(params=>{
      const id=params.get('id');
      const isNew=this.route.snapshot.routeConfig?.path === 'construction-diary/new';
      this.entry.set(null);
      this.diaryId.set(isNew ? 'new' : id||'');
      this.editing.set(isNew || id!==null);
      this.loadProjects(); this.loadUsers();
      if(id && !isNew){this.loadEntry(id);return;}
      if(!isNew){this.loadList();this.loadStats();}
    });
  }
  loadList(){const params=new URLSearchParams({size:'40',sort:'entryDate,desc'});if(this.search())params.set('text',this.search());if(this.statusFilter())params.set('status',this.statusFilter());if(this.typeFilter())params.set('type',this.typeFilter());this.http.get<ApiResponse<Page<DiarySummary>>>(`${this.base}?${params}`).subscribe(r=>this.entries.set(r.data?.content||[]));}
  loadStats(){this.http.get<ApiResponse<any>>(`${this.base}/stats`).subscribe(r=>this.stats.set(r.data));} loadProjects(){this.http.get<ApiResponse<Page<Project>>>('/api/tenant/projects?size=200&sort=name,asc').subscribe(r=>this.projects.set(r.data?.content||[]));} loadUsers(){this.http.get<ApiResponse<Page<User>>>('/api/tenant/users?size=200&sort=name,asc').subscribe(r=>this.users.set(r.data?.content||[]));}
  loadEntry(id:string){
    this.http.get<ApiResponse<Diary>>(`${this.base}/${id}`).subscribe({
      next: response=>{
        const entry=response?.data;
        if(!entry){this.toast.error('Registro não encontrado.');this.router.navigate(['/app/construction-diary']);return;}
        this.entry.set(entry);
        this.form.reset({projectId:entry.projectId||'',title:entry.title||'',entryType:entry.entryType||'VISIT',entryDate:entry.entryDate||new Date().toISOString().slice(0,10),startTime:entry.startTime||'',endTime:entry.endTime||'',location:entry.location||'',responsibleUserId:entry.responsibleUserId||'',summary:entry.summary||'',visibility:entry.visibility||'INTERNAL',nextVisitDate:entry.nextVisitDate||'',nextVisitNotes:entry.nextVisitNotes||''});
        this.selectedStages.set(entry.stageIds||[]);
      },error:()=>this.toast.error('Não foi possível carregar o registro.')
    });
  }
  loadStages(projectId:string){this.http.get<ApiResponse<any[]>>(`/api/tenant/projects/${projectId}/phases`).subscribe(r=>this.stages.set((r.data||[]).flatMap((p:any)=>p.stages||[])));}
  save(){if(this.form.invalid){this.form.markAllAsTouched();this.toast.validation('Informe projeto, título, data e resumo.');return;}const existing=this.entry();const payload={...this.form.getRawValue(),responsibleUserId:this.form.value.responsibleUserId||null,nextVisitDate:this.form.value.nextVisitDate||null,stageIds:this.selectedStages(),participants:existing?.participants||[],observations:existing?.observations||[],occurrences:existing?.occurrences||[],decisions:existing?.decisions||[],instructions:existing?.instructions||[]};const id=this.diaryId();const request=id==='new'?this.http.post<ApiResponse<Diary>>(this.base,payload):this.http.put<ApiResponse<Diary>>(`${this.base}/${id}`,payload);request.subscribe({next:r=>{this.entry.set(r.data);this.toast.success('Registro salvo.');if(id==='new'&&r.data)this.router.navigate(['/app/construction-diary',r.data.id]);},error:()=>this.toast.error('Não foi possível salvar o registro.')});}
  publish(){const id=this.entry()?.id;if(!id)return;this.http.post<ApiResponse<Diary>>(`${this.base}/${id}/publish`,{}).subscribe({next:r=>{this.entry.set(r.data);this.toast.success('Registro publicado.',r.data?.visibility==='CLIENT_VISIBLE'?'O cliente já pode visualizá-lo no Portal.':'Disponível somente para a equipe.');},error:e=>this.toast.error(e.error?.message||'Preencha o resumo antes de publicar.')});}
  toggleStage(id:string,on:boolean){this.selectedStages.update(items=>on?[...items,id]:items.filter(value=>value!==id));} projectOptions(){return this.projects().map(p=>({label:`${p.code} · ${p.name}`,value:p.id}));} userOptions(){return this.users().map(u=>({label:u.name,value:u.id}));} statusLabel(v:string){return ({DRAFT:'Rascunho',PUBLISHED:'Publicado',ARCHIVED:'Arquivado'}[v]||v);} typeLabel(v:string){return ({VISIT:'Visita',FOLLOW_UP:'Acompanhamento',INSPECTION:'Inspeção',MEETING:'Reunião',OCCURRENCE:'Ocorrência',OTHER:'Outro'}[v]||v);} statusClass(v:string){return v==='PUBLISHED'?'bg-arqly-50 text-arqly-700':v==='ARCHIVED'?'bg-slate-100 text-slate-500':'bg-amber-50 text-amber-700';}
}
