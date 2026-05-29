import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { BudgetService } from '../services/budget.service';
import { BudgetResponse, BudgetStatus } from '../models/budget.model';
import { BudgetFormComponent, BudgetFormDialogData } from '../budget-form/budget-form.component';
import { ThemeService } from '../../core/theme/theme.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-budget-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule,
  ],
  templateUrl: './budget-list.component.html',
  styleUrl: './budget-list.component.scss',
})
export class BudgetListComponent implements OnInit {
  private budgetService = inject(BudgetService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  protected theme = inject(ThemeService);
  protected auth = inject(AuthService);

  private currentDate = signal(new Date());

  protected loading = signal(false);
  protected budgets = signal<BudgetResponse[]>([]);

  protected monthLabel = computed(() => {
    const d = this.currentDate();
    return d.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
  });

  protected currentMonth = computed(() => {
    const d = this.currentDate();
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    return `${year}-${month}-01`; // "2026-05-01"
  });

  protected totalLimit = computed(() =>
    this.budgets().reduce((sum, b) => sum + b.limitAmount, 0)
  );

  protected totalSpent = computed(() =>
    this.budgets().reduce((sum, b) => sum + b.spentAmount, 0)
  );

  protected overallPercentage = computed(() => {
    const limit = this.totalLimit();
    if (limit === 0) return 0;
    return Math.round((this.totalSpent() / limit) * 100);
  });

  ngOnInit(): void {
    this.loadBudgets();
  }

  protected previousMonth(): void {
    const d = this.currentDate();
    this.currentDate.set(new Date(d.getFullYear(), d.getMonth() - 1, 1));
    this.loadBudgets();
  }

  protected nextMonth(): void {
    const d = this.currentDate();
    this.currentDate.set(new Date(d.getFullYear(), d.getMonth() + 1, 1));
    this.loadBudgets();
  }

  protected openNewForm(): void {
    const ref = this.dialog.open(BudgetFormComponent, {
      width: '520px',
      maxWidth: '100vw',
      panelClass: 'form-dialog',
      data: {
        budget: null,
        month: this.currentMonth(),
      } as BudgetFormDialogData,
    });

    ref.afterClosed().subscribe((saved: boolean) => {
      if (saved) {
        this.loadBudgets();
        this.snackBar.open('Orçamento criado com sucesso!', 'Fechar', {
          duration: 3000,
          panelClass: 'snack--success',
        });
      }
    });
  }

  protected openEditForm(budget: BudgetResponse): void {
    const ref = this.dialog.open(BudgetFormComponent, {
      width: '520px',
      maxWidth: '100vw',
      panelClass: 'form-dialog',
      data: {
        budget,
        month: this.currentMonth(),
      } as BudgetFormDialogData,
    });

    ref.afterClosed().subscribe((saved: boolean) => {
      if (saved) {
        this.loadBudgets();
        this.snackBar.open('Orçamento atualizado!', 'Fechar', {
          duration: 3000,
          panelClass: 'snack--success',
        });
      }
    });
  }

  protected confirmDelete(budget: BudgetResponse): void {
    const msg = `Excluir orçamento de "${budget.categoryName}"?`;
    const ref = this.snackBar.open(msg, 'Excluir', {
      duration: 5000,
      panelClass: 'snack--warn',
    });

    ref.onAction().subscribe(() => {
      this.budgetService.delete(budget.id).subscribe({
        next: () => {
          this.loadBudgets();
          this.snackBar.open('Orçamento excluído.', 'Fechar', {
            duration: 3000,
          });
        },
        error: (err: { error?: { message?: string } }) => {
          const message =
            err?.error?.message ?? 'Erro ao excluir orçamento. Tente novamente.';
          this.snackBar.open(message, 'Fechar', {
            duration: 4000,
            panelClass: 'snack--error',
          });
        },
      });
    });
  }

  protected statusColor(status: BudgetStatus): string {
    switch (status) {
      case 'OK':
        return '#7fc4a0';  // verde-menta
      case 'WARNING':
        return '#f0c080';  // areia
      case 'EXCEEDED':
        return '#e8927c';  // coral
    }
  }

  protected statusClass(status: BudgetStatus): string {
    switch (status) {
      case 'OK':
        return 'status--ok';
      case 'WARNING':
        return 'status--warning';
      case 'EXCEEDED':
        return 'status--exceeded';
    }
  }

  protected progressValue(percentage: number): number {
    return Math.min(percentage, 100);
  }

  protected formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
    });
  }

  private loadBudgets(): void {
    this.loading.set(true);
    this.budgetService.getByMonth(this.currentMonth()).subscribe({
      next: (data) => {
        this.budgets.set(data);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading.set(false);
        const message =
          err?.error?.message ?? 'Erro ao carregar orçamentos. Tente novamente.';
        this.snackBar.open(message, 'Fechar', {
          duration: 4000,
          panelClass: 'snack--error',
        });
      },
    });
  }
}
