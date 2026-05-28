import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TransactionService } from '../../core/services/transaction.service';
import { ThemeService } from '../../core/theme/theme.service';
import {
  Transaction,
  PaymentMethod,
  SplitRule,
} from '../../core/models/transaction.models';
import { TransactionFormComponent } from '../transaction-form/transaction-form.component';

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    CurrencyPipe,
    DatePipe,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatSnackBarModule,
  ],
  templateUrl: './transaction-list.component.html',
  styleUrl: './transaction-list.component.scss',
})
export class TransactionListComponent implements OnInit {
  private transactionService = inject(TransactionService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  protected theme = inject(ThemeService);

  // Estado do mês navegável
  private currentDate = signal(new Date());

  protected loading = signal(false);
  protected transactions = signal<Transaction[]>([]);

  protected monthLabel = computed(() => {
    const d = this.currentDate();
    return d.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
  });

  protected currentMonth = computed(() => {
    const d = this.currentDate();
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    return `${year}-${month}`;
  });

  protected totalIncome = computed(() =>
    this.transactions()
      .filter((t) => t.type === 'INCOME')
      .reduce((sum, t) => sum + t.amount, 0)
  );

  protected totalExpense = computed(() =>
    this.transactions()
      .filter((t) => t.type === 'EXPENSE')
      .reduce((sum, t) => sum + t.amount, 0)
  );

  protected themeIcon = computed(() =>
    this.theme.isDark() ? 'Modo claro' : 'Modo escuro'
  );

  ngOnInit(): void {
    this.loadTransactions();
  }

  protected previousMonth(): void {
    const d = this.currentDate();
    this.currentDate.set(new Date(d.getFullYear(), d.getMonth() - 1, 1));
    this.loadTransactions();
  }

  protected nextMonth(): void {
    const d = this.currentDate();
    this.currentDate.set(new Date(d.getFullYear(), d.getMonth() + 1, 1));
    this.loadTransactions();
  }

  protected openNewForm(): void {
    const ref = this.dialog.open(TransactionFormComponent, {
      width: '560px',
      maxWidth: '100vw',
      panelClass: 'form-dialog',
      data: { transaction: null, month: this.currentMonth() },
    });

    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.loadTransactions();
        this.snackBar.open('Lançamento criado com sucesso!', 'Fechar', {
          duration: 3000,
          panelClass: 'snack--success',
        });
      }
    });
  }

  protected openEditForm(tx: Transaction): void {
    const ref = this.dialog.open(TransactionFormComponent, {
      width: '560px',
      maxWidth: '100vw',
      panelClass: 'form-dialog',
      data: { transaction: tx, month: this.currentMonth() },
    });

    ref.afterClosed().subscribe((saved) => {
      if (saved) {
        this.loadTransactions();
        this.snackBar.open('Lançamento atualizado!', 'Fechar', {
          duration: 3000,
          panelClass: 'snack--success',
        });
      }
    });
  }

  protected confirmDelete(tx: Transaction): void {
    const msg = `Excluir "${tx.description || tx.category.name}"?`;
    const ref = this.snackBar.open(msg, 'Excluir', {
      duration: 5000,
      panelClass: 'snack--warn',
    });

    ref.onAction().subscribe(() => {
      this.transactionService.deleteTransaction(tx.id).subscribe({
        next: () => {
          this.loadTransactions();
          this.snackBar.open('Lançamento excluído.', 'Fechar', {
            duration: 3000,
          });
        },
        error: (err) => {
          const msg =
            err?.error?.message ?? 'Erro ao excluir lançamento. Tente novamente.';
          this.snackBar.open(msg, 'Fechar', {
            duration: 4000,
            panelClass: 'snack--error',
          });
        },
      });
    });
  }

  protected categoryIcon(categoryName: string): string {
    const name = categoryName.toLowerCase();
    if (name.includes('mercado') || name.includes('alimenta')) return 'shopping_cart';
    if (name.includes('salário') || name.includes('renda') || name.includes('receita')) return 'work';
    if (name.includes('transporte') || name.includes('uber') || name.includes('combustível')) return 'directions_car';
    if (name.includes('lazer') || name.includes('cinema') || name.includes('entretenimento')) return 'movie';
    if (name.includes('saúde') || name.includes('farmácia') || name.includes('médico')) return 'local_hospital';
    if (name.includes('educação') || name.includes('curso')) return 'school';
    if (name.includes('moradia') || name.includes('aluguel')) return 'home';
    if (name.includes('viagem')) return 'flight';
    return 'receipt';
  }

  protected paymentMethodLabel(method: PaymentMethod): string {
    const labels: Record<PaymentMethod, string> = {
      CASH: 'Dinheiro',
      DEBIT: 'Débito',
      CREDIT: 'Crédito',
      PIX: 'Pix',
      TRANSFER: 'Transferência',
    };
    return labels[method] ?? method;
  }

  protected splitRuleLabel(rule: SplitRule): string {
    const labels: Record<SplitRule, string> = {
      FIFTY_FIFTY: '50/50',
      PERSON_A: 'Só Pessoa A',
      PERSON_B: 'Só Pessoa B',
      PROPORTIONAL: 'Proporcional',
    };
    return labels[rule] ?? rule;
  }

  private loadTransactions(): void {
    this.loading.set(true);
    this.transactionService.getTransactions(this.currentMonth()).subscribe({
      next: (data) => {
        // Ordena por data decrescente
        const sorted = [...data].sort(
          (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()
        );
        this.transactions.set(sorted);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        const msg =
          err?.error?.message ?? 'Erro ao carregar lançamentos. Tente novamente.';
        this.snackBar.open(msg, 'Fechar', {
          duration: 4000,
          panelClass: 'snack--error',
        });
      },
    });
  }
}
