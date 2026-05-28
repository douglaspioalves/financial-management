import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../core/services/auth.service';
import { ThemeService } from '../core/theme/theme.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="dash-page">
      <header class="dash-header">
        <div class="brand">
          <div class="brand__logo">G</div>
          <div>
            <b class="brand__name">Gastos do Casal</b>
            <small class="brand__sub">painel</small>
          </div>
        </div>
        <div class="dash-header__actions">
          <button mat-icon-button
                  [matTooltip]="theme.isDark() ? 'Modo claro' : 'Modo escuro'"
                  (click)="theme.toggle()">
            <mat-icon>{{ theme.isDark() ? 'light_mode' : 'dark_mode' }}</mat-icon>
          </button>
          <button mat-icon-button matTooltip="Sair" (click)="auth.logout()">
            <mat-icon>logout</mat-icon>
          </button>
        </div>
      </header>
      <div class="dash-welcome">
        <h1 class="dash-welcome__title">Ola, {{ auth.currentUser()?.name }}!</h1>
        <p class="dash-welcome__sub">Controle seus gastos de forma simples e clara.</p>
      </div>
      <nav class="dash-nav">
        <a routerLink="/transactions" class="nav-card">
          <div class="nav-card__icon nav-card__icon--blue">
            <mat-icon>receipt_long</mat-icon>
          </div>
          <div class="nav-card__body">
            <span class="nav-card__title">Lancamentos</span>
            <span class="nav-card__desc">Receitas e despesas do mes</span>
          </div>
          <mat-icon class="nav-card__arrow">chevron_right</mat-icon>
        </a>
        <a class="nav-card nav-card--disabled">
          <div class="nav-card__icon nav-card__icon--mint">
            <mat-icon>bar_chart</mat-icon>
          </div>
          <div class="nav-card__body">
            <span class="nav-card__title">Dashboard</span>
            <span class="nav-card__desc">Em breve - Sprint 04</span>
          </div>
          <mat-icon class="nav-card__arrow">chevron_right</mat-icon>
        </a>
        <a class="nav-card nav-card--disabled">
          <div class="nav-card__icon nav-card__icon--lilac">
            <mat-icon>credit_card</mat-icon>
          </div>
          <div class="nav-card__body">
            <span class="nav-card__title">Cartoes</span>
            <span class="nav-card__desc">Em breve - Sprint 03</span>
          </div>
          <mat-icon class="nav-card__arrow">chevron_right</mat-icon>
        </a>
        <a class="nav-card nav-card--disabled">
          <div class="nav-card__icon nav-card__icon--sand">
            <mat-icon>handshake</mat-icon>
          </div>
          <div class="nav-card__body">
            <span class="nav-card__title">Acerto de contas</span>
            <span class="nav-card__desc">Em breve - Sprint 06</span>
          </div>
          <mat-icon class="nav-card__arrow">chevron_right</mat-icon>
        </a>
      </nav>
    </div>
  `,
  styles: [`
    .dash-page { min-height: 100vh; background: var(--color-bg); font-family: var(--font-body); }
    .dash-header { display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; background: color-mix(in srgb, var(--color-bg) 85%, transparent); backdrop-filter: blur(12px); border-bottom: 1px solid var(--color-border); position: sticky; top: 0; z-index: 10; }
    .dash-header__actions { display: flex; align-items: center; gap: 4px; }
    .brand { display: flex; align-items: center; gap: 12px; }
    .brand__logo { width: 38px; height: 38px; border-radius: 12px; background: linear-gradient(135deg, var(--color-primary-light), var(--color-primary-dark)); display: grid; place-items: center; color: #fff; font-family: var(--font-display); font-weight: 700; font-size: 1.1rem; box-shadow: var(--shadow-sm); }
    .brand__name { font-family: var(--font-display); font-size: 1.05rem; font-weight: 600; color: var(--color-text-primary); display: block; }
    .brand__sub { font-size: 0.7rem; color: var(--color-text-disabled); text-transform: uppercase; letter-spacing: 0.05em; display: block; }
    .dash-welcome { padding: 32px 20px 20px; }
    .dash-welcome__title { font-family: var(--font-display); font-size: 1.6rem; font-weight: 600; color: var(--color-text-primary); margin: 0 0 4px; letter-spacing: -0.02em; }
    .dash-welcome__sub { color: var(--color-text-secondary); font-size: 0.95rem; margin: 0; }
    .dash-nav { display: flex; flex-direction: column; gap: 10px; padding: 0 16px 24px; }
    .nav-card { display: flex; align-items: center; gap: 14px; padding: 16px; background: var(--color-bg-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); text-decoration: none; color: inherit; transition: box-shadow var(--transition-fast), transform var(--transition-fast); cursor: pointer; }
    .nav-card:hover:not(.nav-card--disabled) { box-shadow: var(--shadow-md); transform: translateY(-1px); }
    .nav-card--disabled { opacity: 0.55; cursor: default; pointer-events: none; }
    .nav-card__icon { width: 44px; height: 44px; border-radius: 12px; display: grid; place-items: center; flex-shrink: 0; }
    .nav-card__icon mat-icon { font-size: 22px; width: 22px; height: 22px; }
    .nav-card__icon--blue { background: color-mix(in srgb, var(--color-primary) 15%, var(--color-bg-card)); color: var(--color-primary); }
    .nav-card__icon--mint { background: color-mix(in srgb, var(--color-income) 20%, var(--color-bg-card)); color: var(--color-income); }
    .nav-card__icon--lilac { background: color-mix(in srgb, var(--color-highlight) 20%, var(--color-bg-card)); color: var(--color-highlight); }
    .nav-card__icon--sand { background: color-mix(in srgb, var(--color-warning) 20%, var(--color-bg-card)); color: var(--color-warning); }
    .nav-card__body { flex: 1; display: flex; flex-direction: column; gap: 2px; }
    .nav-card__title { font-size: 0.95rem; font-weight: 600; color: var(--color-text-primary); }
    .nav-card__desc { font-size: 0.78rem; color: var(--color-text-secondary); }
    .nav-card__arrow { color: var(--color-text-disabled); font-size: 20px; width: 20px; height: 20px; }
    @media (min-width: 640px) {
      .dash-nav { display: grid; grid-template-columns: 1fr 1fr; padding: 0 24px 24px; }
      .dash-welcome { padding: 40px 24px 24px; }
      .dash-welcome__title { font-size: 2rem; }
    }
  `],
})
export class DashboardComponent {
  protected auth  = inject(AuthService);
  protected theme = inject(ThemeService);
}
