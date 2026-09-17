import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { FileExplorerComponent } from '../../shared/files/file-explorer.component';
import { FileOwnerType } from '../../shared/files/file.models';

@Component({
  selector: 'app-files',
  standalone: true,
  imports: [FileExplorerComponent, RouterLink],
  template: `
    <section class="space-y-5">
      <div>
        @if (contextual()) {
          <a class="mb-4 inline-flex items-center gap-2 text-sm font-bold text-arqly-700" [routerLink]="backLink()">
            <span aria-hidden="true">←</span> Voltar
          </a>
        }
        <p class="text-xs font-extrabold uppercase tracking-[0.22em] text-arqly-700">Acervo do escritório</p>
        <h2 class="mt-2 text-3xl font-extrabold tracking-tight">Arquivos</h2>
        <p class="mt-2 text-slate-500">Gerencie arquivos, versões, pastas e documentos técnicos em um só lugar.</p>
      </div>
      @if (tenantId()) {
        <div>
          <app-file-explorer [ownerType]="ownerType()" [ownerId]="ownerId()!" [browseAll]="!contextual()"
            [title]="explorerTitle()" [eyebrow]="contextual() ? 'Arquivos vinculados' : 'Biblioteca central'" />
        </div>
      }
    </section>
  `
})
export class FilesComponent {
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly routeOwnerType = this.route.snapshot.paramMap.get('ownerType') as FileOwnerType | null;
  private readonly routeOwnerId = this.route.snapshot.paramMap.get('ownerId');

  tenantId = () => this.auth.currentUser()?.tenantId || null;
  contextual = () => !!this.routeOwnerType && !!this.routeOwnerId;
  ownerType = () => this.routeOwnerType || 'TENANT';
  ownerId = () => this.routeOwnerId || this.tenantId();
  explorerTitle = () => this.contextual() ? `Arquivos de ${this.ownerTypeLabel(this.ownerType())}` : 'Arquivos do escritório';
  backLink = () => ({
    BRIEFING: '/app/briefings',
    PROPOSAL: '/app/proposals',
    PROJECT: '/app/projects',
    PROJECT_STAGE: '/app/projects',
    CONSTRUCTION_DIARY_ENTRY: '/app/construction-diary',
    DOCUMENT: '/app/documents/generated',
    TENANT: '/app/files'
  })[this.ownerType()];

  private ownerTypeLabel(type: FileOwnerType) {
    return ({ BRIEFING: 'briefing', PROPOSAL: 'proposta', PROJECT: 'projeto', PROJECT_STAGE: 'etapa', CONSTRUCTION_DIARY_ENTRY: 'registro do Diário',
      DOCUMENT: 'documento', TENANT: 'escritório' })[type];
  }
}
