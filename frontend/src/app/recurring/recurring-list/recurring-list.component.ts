import {
  Component,
  OnInit,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { RecurringRuleService } from '../recurring-rule.service';
import { RecurringRuleResponse, RecurringFrequency } from '../models/recurring-rule.models';
import { RecurringFormComponent } from '../recurring-form/recurring-form.component';
import { ThemeService } from '../../core/theme/theme.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-recurring-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatTableModule,
    MatChipsModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './recurring-list.component.html',
  styleUrl: './recurring-list.component.scss',
})
export class RecurringListComponent implements OnInit {
  private recurringService = inject(RecurringRuleService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  protected theme = inject(ThemeService);
  protected auth = inject(AuthService);

  protected loading = signal(false);
  protected rules = signal<RecurringRuleResponse[]>([]);

  readonly displayedColumns = [
    'description',
    'type',
    'amount',
    'frequency',
    'nextDate',
    'splitRule',
    'paidBy',
    'actions',
  ];

  ngOnInit(): void {
    this.loadRules();
  }

  protected frequencyLabel(frequency: RecurringFrequency): string {
    const labels: Record<RecurringFrequency, string> = {
      MONTHLY: 'Mensal',
      WEEKLY: 'Semanal',
      YEARLY: 'Anual',
    };
    return labels[frequency] ?? frequency;
  }

  protected splitRuleLabel(rule: string): string {
    const labels: Record<string, string> = {
      FIFTY_FIFTY: '50/50',
      PERSON_A: 'Pessoa A',
      PERSON_B: 'Pessoa B',
      PROPORTIONAL: 'Proporcional',
    };
    return labels[rule] ?? rule;
  }

  protected formatDate(dateStr: string): string {
    const [year, month, day] = dateStr.split('-');
    return `${day}/${month}/${year}`;
  }

  protected formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
    });
  }

  protected openNewForm(): void {
    const ref = this.dialog.open(RecurringFormComponent, {
      width: '560px',
      maxWidth: '100vw',
      panelClass: 'form-dialog',
    });

    ref.afterClosed().subscribe((saved: boolean) => {
      if (saved) {
        this.loadRules();
        this.snackBar.open('Regra recorrente criada com sucesso!', 'Fechar', {
          duration: 3000,
          panelClass: 'snack--success',
        });
      }
    });
  }

  protected confirmDeactivate(rule: RecurringRuleResponse): void {
    const confirmed = confirm(
      `Desativar a regra "${rule.description}"?\n\nEla não gerará novos lançamentos após isso.`
    );
    if (!confirmed) return;

    this.recurringService.deactivate(rule.id).subscribe({
      next: () => {
        this.loadRules();
        this.snackBar.open('Regra desativada.', 'Fechar', { duration: 3000 });
      },
      error: (err: { error?: { message?: string } }) => {
        const message =
          err?.error?.message ?? 'Erro ao desativar regra. Tente novamente.';
        this.snackBar.open(message, 'Fechar', {
          duration: 4000,
          panelClass: 'snack--error',
        });
      },
    });
  }

  private loadRules(): void {
    this.loading.set(true);
    this.recurringService.getAll().subscribe({
      next: (data) => {
        this.rules.set(data);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading.set(false);
        const message =
          err?.error?.message ?? 'Erro ao carregar regras recorrentes. Tente novamente.';
        this.snackBar.open(message, 'Fechar', {
          duration: 4000,
          panelClass: 'snack--error',
        });
      },
    });
  }
}
