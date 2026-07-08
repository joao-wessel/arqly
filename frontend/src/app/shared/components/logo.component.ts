import { Component } from '@angular/core';

@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    <div class="flex items-center gap-3">
      <svg class="h-11 w-11" viewBox="0 0 64 64" aria-hidden="true">
        <path d="M5 58 25 6h15L20 58H5Z" style="fill: rgb(var(--arqly-600));"/>
        <path d="M31 6h14l15 52H44L31 18V6Z" style="fill: rgb(var(--arqly-700));"/>
        <path d="M22 58 32 32l10 26H22Z" style="fill: rgb(var(--arqly-100));"/>
      </svg>
      <span class="arqly-logo-text text-[2rem] font-extrabold tracking-tight text-slate-950">Arqly</span>
    </div>
  `
})
export class LogoComponent {}
