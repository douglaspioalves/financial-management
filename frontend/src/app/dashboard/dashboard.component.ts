import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { BaseChartDirective } from 'ng2-charts';
import { ChartData, ChartOptions } from 'chart.js';
import { AuthService } from '../core/services/auth.service';
import { ThemeService } from '../core/theme/theme.service';
import { DashboardService } from './services/dashboard.service';
import {
  DashboardResponse,
  CategoryExpense,
  RecentTransaction,
} from './models/dashboard.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    DecimalPipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatCardModule,
    BaseChartDirective,
  ],
  template: `
    <div class="dash-page">
      <!-- ===== HEADER ===== -->
      <header class="dash-header">
        <div class="brand">
          <div class="brand__logo">G</div>
          <div>
            <b class="brand__name">Gastos do Casal</b>
            <small class="brand__sub">dashboard</small>
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

      <!-- ===== PROGRESS BAR ===== -->
      <mat-progress-bar *ngIf="loading()" mode="indeterminate" class="loading-bar"></mat-progress-bar>

      <div class="dash-content">

        <!-- ===== NAVEGAÇÃO DO MÊS ===== -->
        <div class="month-nav">
          <button mat-icon-button (click)="previousMonth()" matTooltip="Mês anterior">
            <mat-icon>chevron_left</mat-icon>
          </button>
          <span class="month-nav__label">{{ monthLabel() }}</span>
          <button mat-icon-button (click)="nextMonth()" matTooltip="Próximo mês">
            <mat-icon>chevron_right</mat-icon>
          </button>
        </div>

        <!-- ===== CARDS DE RESUMO ===== -->
        <div class="summary-grid">
          <!-- Receitas -->
          <div class="summary-card summary-card--income">
            <div class="summary-card__icon">
              <mat-icon>trending_up</mat-icon>
            </div>
            <div class="summary-card__body">
              <span class="summary-card__label">Receitas</span>
              <span class="summary-card__amount">
                {{ formatCurrency(data()?.totalIncome ?? 0) }}
              </span>
              <span class="summary-card__variation"
                    [class.variation--up]="(data()?.incomeVariation ?? 0) >= 0"
                    [class.variation--down]="(data()?.incomeVariation ?? 0) < 0">
                <ng-container *ngIf="data()?.incomeVariation !== null && data()?.incomeVariation !== undefined; else noVar">
                  <mat-icon class="variation__icon">
                    {{ (data()?.incomeVariation ?? 0) >= 0 ? 'arrow_upward' : 'arrow_downward' }}
                  </mat-icon>
                  {{ formatVariation(data()?.incomeVariation) }}
                </ng-container>
                <ng-template #noVar>—</ng-template>
              </span>
            </div>
          </div>

          <!-- Despesas -->
          <div class="summary-card summary-card--expense">
            <div class="summary-card__icon">
              <mat-icon>trending_down</mat-icon>
            </div>
            <div class="summary-card__body">
              <span class="summary-card__label">Despesas</span>
              <span class="summary-card__amount">
                {{ formatCurrency(data()?.totalExpense ?? 0) }}
              </span>
              <span class="summary-card__variation"
                    [class.variation--up]="(data()?.expenseVariation ?? 0) < 0"
                    [class.variation--down]="(data()?.expenseVariation ?? 0) >= 0">
                <ng-container *ngIf="data()?.expenseVariation !== null && data()?.expenseVariation !== undefined; else noVarExp">
                  <mat-icon class="variation__icon">
                    {{ (data()?.expenseVariation ?? 0) >= 0 ? 'arrow_upward' : 'arrow_downward' }}
                  </mat-icon>
                  {{ formatVariation(data()?.expenseVariation) }}
                </ng-container>
                <ng-template #noVarExp>—</ng-template>
              </span>
            </div>
          </div>

          <!-- Saldo -->
          <div class="summary-card summary-card--balance">
            <div class="summary-card__icon">
              <mat-icon>account_balance_wallet</mat-icon>
            </div>
            <div class="summary-card__body">
              <span class="summary-card__label">Saldo</span>
              <span class="summary-card__amount"
                    [class.amount--positive]="(data()?.balance ?? 0) >= 0"
                    [class.amount--negative]="(data()?.balance ?? 0) < 0">
                {{ (data()?.balance ?? 0) >= 0 ? '+' : '' }}{{ formatCurrency(data()?.balance ?? 0) }}
              </span>
              <span class="summary-card__variation">vs. mês anterior</span>
            </div>
          </div>
        </div>

        <!-- ===== SEÇÃO INFERIOR: GRÁFICO + LISTA ===== -->
        <div class="bottom-grid">

          <!-- Gráfico Donut -->
          <div class="panel">
            <h2 class="panel__title">Despesas por categoria</h2>

            <ng-container *ngIf="!loading()">
              <ng-container *ngIf="(data()?.expenseByCategory?.length ?? 0) > 0; else emptyChart">
                <div class="chart-container">
                  <canvas baseChart
                          [data]="donutData()"
                          [options]="donutOptions"
                          type="doughnut">
                  </canvas>
                </div>
                <ul class="category-legend">
                  <li *ngFor="let cat of data()?.expenseByCategory"
                      class="legend-item">
                    <span class="legend-item__dot"
                          [style.background]="cat.categoryColor"></span>
                    <span class="legend-item__name">{{ cat.categoryName }}</span>
                    <span class="legend-item__pct">{{ cat.percentage | number:'1.1-1' }}%</span>
                    <span class="legend-item__val">{{ formatCurrency(cat.total) }}</span>
                  </li>
                </ul>
              </ng-container>
              <ng-template #emptyChart>
                <div class="empty-state">
                  <mat-icon class="empty-state__icon">pie_chart</mat-icon>
                  <p>Nenhuma despesa registrada neste mês.</p>
                </div>
              </ng-template>
            </ng-container>
          </div>

          <!-- Lista de lançamentos recentes -->
          <div class="panel">
            <div class="panel__title-row">
              <h2 class="panel__title">Lançamentos recentes</h2>
              <a routerLink="/transactions" class="panel__link">Ver todos</a>
            </div>

            <ng-container *ngIf="!loading()">
              <ng-container *ngIf="(data()?.recentTransactions?.length ?? 0) > 0; else emptyTx">
                <ul class="tx-list">
                  <li *ngFor="let tx of data()?.recentTransactions"
                      class="tx-item">
                    <div class="tx-item__type-dot"
                         [class.tx-item__type-dot--income]="tx.type === 'INCOME'"
                         [class.tx-item__type-dot--expense]="tx.type === 'EXPENSE'">
                      <mat-icon>{{ tx.type === 'INCOME' ? 'arrow_upward' : 'arrow_downward' }}</mat-icon>
                    </div>
                    <div class="tx-item__info">
                      <span class="tx-item__desc">{{ tx.description || tx.categoryName }}</span>
                      <span class="tx-item__meta">
                        {{ formatDate(tx.date) }} · {{ tx.categoryName }} · {{ tx.paidByPersonName }}
                      </span>
                    </div>
                    <span class="tx-item__amount"
                          [class.amount--positive]="tx.type === 'INCOME'"
                          [class.amount--negative]="tx.type === 'EXPENSE'">
                      {{ tx.type === 'INCOME' ? '+' : '-' }}{{ formatCurrency(tx.amount) }}
                    </span>
                  </li>
                </ul>
              </ng-container>
              <ng-template #emptyTx>
                <div class="empty-state">
                  <mat-icon class="empty-state__icon">receipt_long</mat-icon>
                  <p>Nenhum lançamento neste mês.</p>
                </div>
              </ng-template>
            </ng-container>
          </div>
        </div>

        <!-- ===== NAV SHORTCUTS ===== -->
        <nav class="shortcuts">
          <a routerLink="/transactions" class="shortcut-card">
            <div class="shortcut-card__icon shortcut-card__icon--blue">
              <mat-icon>receipt_long</mat-icon>
            </div>
            <span class="shortcut-card__label">Lançamentos</span>
          </a>
          <a routerLink="/cards" class="shortcut-card">
            <div class="shortcut-card__icon shortcut-card__icon--lilac">
              <mat-icon>credit_card</mat-icon>
            </div>
            <span class="shortcut-card__label">Cartões</span>
          </a>
          <a routerLink="/settlement" class="shortcut-card">
            <div class="shortcut-card__icon shortcut-card__icon--sand">
              <mat-icon>handshake</mat-icon>
            </div>
            <span class="shortcut-card__label">Acerto</span>
          </a>
        </nav>

      </div><!-- /dash-content -->
    </div>
  `,
  styles: [`
    /* ===== PAGE ===== */
    .dash-page {
      min-height: 100vh;
      background: var(--color-bg);
      font-family: var(--font-body);
    }

    /* ===== HEADER ===== */
    .dash-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 20px;
      background: color-mix(in srgb, var(--color-bg) 85%, transparent);
      backdrop-filter: blur(12px);
      border-bottom: 1px solid var(--color-border);
      position: sticky;
      top: 0;
      z-index: 10;
    }
    .dash-header__actions { display: flex; align-items: center; gap: 4px; }
    .brand { display: flex; align-items: center; gap: 12px; }
    .brand__logo {
      width: 38px; height: 38px;
      border-radius: 12px;
      background: linear-gradient(135deg, var(--color-primary-light), var(--color-primary-dark));
      display: grid; place-items: center;
      color: #fff;
      font-family: var(--font-display);
      font-weight: 700; font-size: 1.1rem;
      box-shadow: var(--shadow-sm);
    }
    .brand__name { font-family: var(--font-display); font-size: 1.05rem; font-weight: 600; color: var(--color-text-primary); display: block; }
    .brand__sub { font-size: 0.7rem; color: var(--color-text-disabled); text-transform: uppercase; letter-spacing: 0.05em; display: block; }

    /* ===== LOADING ===== */
    .loading-bar { position: sticky; top: 0; z-index: 11; }

    /* ===== CONTENT ===== */
    .dash-content { padding: 24px 16px; max-width: 1100px; margin: 0 auto; }

    /* ===== MÊS ===== */
    .month-nav {
      display: flex; align-items: center; justify-content: center; gap: 8px;
      margin-bottom: 24px;
    }
    .month-nav__label {
      font-family: var(--font-display);
      font-size: 1.25rem;
      font-weight: 600;
      color: var(--color-text-primary);
      min-width: 180px;
      text-align: center;
      text-transform: capitalize;
    }

    /* ===== SUMMARY CARDS ===== */
    .summary-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 12px;
      margin-bottom: 24px;
    }
    @media (min-width: 600px) {
      .summary-grid { grid-template-columns: repeat(3, 1fr); }
    }

    .summary-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px;
      background: var(--color-bg-card);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-sm);
    }
    .summary-card__icon {
      width: 48px; height: 48px;
      border-radius: 14px;
      display: grid; place-items: center;
      flex-shrink: 0;
    }
    .summary-card__icon mat-icon { font-size: 24px; width: 24px; height: 24px; }

    .summary-card--income .summary-card__icon {
      background: color-mix(in srgb, var(--color-income) 18%, var(--color-bg-card));
      color: var(--color-income);
    }
    .summary-card--expense .summary-card__icon {
      background: color-mix(in srgb, var(--color-expense) 18%, var(--color-bg-card));
      color: var(--color-expense);
    }
    .summary-card--balance .summary-card__icon {
      background: color-mix(in srgb, var(--color-primary) 15%, var(--color-bg-card));
      color: var(--color-primary);
    }

    .summary-card__body { display: flex; flex-direction: column; gap: 2px; }
    .summary-card__label { font-size: 0.78rem; color: var(--color-text-secondary); text-transform: uppercase; letter-spacing: 0.06em; }
    .summary-card__amount {
      font-family: var(--font-display);
      font-size: 1.35rem;
      font-weight: 700;
      color: var(--color-text-primary);
      letter-spacing: -0.02em;
    }
    .summary-card__variation {
      display: flex; align-items: center; gap: 2px;
      font-size: 0.78rem;
      color: var(--color-text-secondary);
    }
    .variation__icon { font-size: 14px; width: 14px; height: 14px; }
    .variation--up { color: var(--color-income); }
    .variation--down { color: var(--color-expense); }
    .amount--positive { color: var(--color-income); }
    .amount--negative { color: var(--color-expense); }

    /* ===== BOTTOM GRID ===== */
    .bottom-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 16px;
      margin-bottom: 24px;
    }
    @media (min-width: 900px) {
      .bottom-grid { grid-template-columns: 1fr 1fr; }
    }

    /* ===== PANELS ===== */
    .panel {
      background: var(--color-bg-card);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-sm);
      padding: 20px;
    }
    .panel__title {
      font-family: var(--font-display);
      font-size: 1rem;
      font-weight: 600;
      color: var(--color-text-primary);
      margin: 0 0 16px;
    }
    .panel__title-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;
    }
    .panel__title-row .panel__title { margin: 0; }
    .panel__link {
      font-size: 0.82rem;
      color: var(--color-primary);
      text-decoration: none;
      font-weight: 500;
    }
    .panel__link:hover { text-decoration: underline; }

    /* ===== DONUT CHART ===== */
    .chart-container {
      width: 100%;
      max-width: 220px;
      margin: 0 auto 16px;
    }

    /* ===== LEGEND ===== */
    .category-legend {
      list-style: none;
      padding: 0; margin: 0;
      display: flex; flex-direction: column; gap: 8px;
    }
    .legend-item {
      display: flex; align-items: center; gap: 8px;
      font-size: 0.85rem;
    }
    .legend-item__dot {
      width: 12px; height: 12px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .legend-item__name { flex: 1; color: var(--color-text-primary); }
    .legend-item__pct { color: var(--color-text-secondary); min-width: 40px; text-align: right; }
    .legend-item__val {
      font-family: var(--font-display);
      font-size: 0.85rem;
      color: var(--color-expense);
      min-width: 80px;
      text-align: right;
    }

    /* ===== TRANSACTION LIST ===== */
    .tx-list {
      list-style: none;
      padding: 0; margin: 0;
      display: flex; flex-direction: column; gap: 4px;
    }
    .tx-item {
      display: flex; align-items: center; gap: 12px;
      padding: 10px 8px;
      border-radius: var(--radius-sm);
      transition: background var(--transition-fast);
    }
    .tx-item:hover { background: var(--color-bg-surface); }
    .tx-item__type-dot {
      width: 34px; height: 34px;
      border-radius: 10px;
      display: grid; place-items: center;
      flex-shrink: 0;
    }
    .tx-item__type-dot mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .tx-item__type-dot--income {
      background: color-mix(in srgb, var(--color-income) 18%, var(--color-bg-card));
      color: var(--color-income);
    }
    .tx-item__type-dot--expense {
      background: color-mix(in srgb, var(--color-expense) 18%, var(--color-bg-card));
      color: var(--color-expense);
    }
    .tx-item__info { flex: 1; display: flex; flex-direction: column; gap: 1px; min-width: 0; }
    .tx-item__desc {
      font-size: 0.88rem;
      font-weight: 500;
      color: var(--color-text-primary);
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .tx-item__meta {
      font-size: 0.74rem;
      color: var(--color-text-secondary);
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .tx-item__amount {
      font-family: var(--font-display);
      font-size: 0.92rem;
      font-weight: 600;
      white-space: nowrap;
    }

    /* ===== EMPTY STATE ===== */
    .empty-state {
      display: flex; flex-direction: column; align-items: center;
      padding: 32px 16px;
      color: var(--color-text-disabled);
      text-align: center;
    }
    .empty-state__icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 12px; }
    .empty-state p { margin: 0; font-size: 0.9rem; }

    /* ===== SHORTCUTS ===== */
    .shortcuts {
      display: flex; gap: 10px; flex-wrap: wrap;
    }
    .shortcut-card {
      display: flex; align-items: center; gap: 10px;
      padding: 12px 18px;
      background: var(--color-bg-card);
      border: 1px solid var(--color-border);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-sm);
      text-decoration: none;
      color: var(--color-text-primary);
      font-size: 0.88rem;
      font-weight: 500;
      cursor: pointer;
      transition: box-shadow var(--transition-fast), transform var(--transition-fast);
    }
    .shortcut-card:hover:not(.shortcut-card--disabled) {
      box-shadow: var(--shadow-md);
      transform: translateY(-1px);
    }
    .shortcut-card--disabled { opacity: 0.5; pointer-events: none; }
    .shortcut-card__icon {
      width: 36px; height: 36px;
      border-radius: 10px;
      display: grid; place-items: center;
    }
    .shortcut-card__icon mat-icon { font-size: 20px; width: 20px; height: 20px; }
    .shortcut-card__icon--blue { background: color-mix(in srgb, var(--color-primary) 15%, var(--color-bg-card)); color: var(--color-primary); }
    .shortcut-card__icon--lilac { background: color-mix(in srgb, var(--color-highlight) 20%, var(--color-bg-card)); color: var(--color-highlight); }
    .shortcut-card__icon--sand { background: color-mix(in srgb, var(--color-warning) 20%, var(--color-bg-card)); color: var(--color-warning); }
    .shortcut-card__label { color: var(--color-text-primary); }
  `],
})
export class DashboardComponent implements OnInit {
  protected auth  = inject(AuthService);
  protected theme = inject(ThemeService);
  private dashboardService = inject(DashboardService);
  private snackBar = inject(MatSnackBar);

  protected loading = signal(false);
  protected data    = signal<DashboardResponse | null>(null);

  private currentDate = signal(new Date());

  protected monthLabel = computed(() => {
    const d = this.currentDate();
    return d.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
  });

  protected currentMonth = computed(() => {
    const d = this.currentDate();
    const year  = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    return `${year}-${month}`;
  });

  /** Dados para o gráfico donut */
  protected donutData = computed<ChartData<'doughnut'>>(() => {
    const categories: CategoryExpense[] = this.data()?.expenseByCategory ?? [];
    return {
      labels:   categories.map(c => c.categoryName),
      datasets: [{
        data:            categories.map(c => c.total),
        backgroundColor: categories.map(c => c.categoryColor),
        borderWidth: 2,
        borderColor: 'transparent',
        hoverBorderColor: 'transparent',
      }],
    };
  });

  protected donutOptions: ChartOptions<'doughnut'> = {
    responsive: true,
    cutout: '65%',
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (ctx) => {
            const val = ctx.parsed as number;
            return ` ${this.formatCurrency(val)}`;
          },
        },
      },
    },
  };

  ngOnInit(): void {
    this.loadDashboard();
  }

  protected previousMonth(): void {
    const d = this.currentDate();
    this.currentDate.set(new Date(d.getFullYear(), d.getMonth() - 1, 1));
    this.loadDashboard();
  }

  protected nextMonth(): void {
    const d = this.currentDate();
    this.currentDate.set(new Date(d.getFullYear(), d.getMonth() + 1, 1));
    this.loadDashboard();
  }

  protected formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  protected formatVariation(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    const sign = value >= 0 ? '+' : '';
    return `${sign}${value.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%`;
  }

  protected formatDate(dateStr: string): string {
    const [year, month, day] = dateStr.split('-');
    return `${day}/${month}/${year}`;
  }

  private loadDashboard(): void {
    this.loading.set(true);
    this.dashboardService.getDashboard(this.currentMonth()).subscribe({
      next: (response) => {
        this.data.set(response);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading.set(false);
        const message = err?.error?.message ?? 'Erro ao carregar dashboard. Tente novamente.';
        this.snackBar.open(message, 'Fechar', {
          duration: 5000,
          panelClass: 'snack--error',
        });
      },
    });
  }
}
