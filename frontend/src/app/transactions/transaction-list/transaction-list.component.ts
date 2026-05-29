import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatChipsModule } from '@angular/material/chips';
import { TransactionService } from '../../core/services/transaction.service';
import { InstallmentService } from '../services/installment.service';
import { ThemeService } from '../../core/theme/theme.service';
import {
  Transaction,
  InstallmentResponse,
  PaymentMethod,
  SplitRule,
} from '../../core/models/transaction.models';
import { TransactionFormComponent } from '../transaction-form/transaction-form.component';

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
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
    MatExpansionModule,
    MatChipsModule,
  ],
  templateUrl: './transaction-list.component.html',
  styleUrl: './transaction-list.component.scss',
})
export class TransactionListComponent implements OnInit {
  private transactionService = inject(TransactionService);
  private installmentService = inject(InstallmentService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  protected theme = inject(ThemeService);

  private currentDate = signal(new Date());

  protected loading = signal(false);
  protected transactions = signal<Transaction[]>([]);

  // Mapa de id da transação -> lista de parcelas carregadas
  protected installmentsMap = signal<Record<string, InstallmentResponse[]>>({});
  // Mapa de id da transação -> estado de loading das parcelas
  protected installmentsLoading = signal<Record<string, boolean>>({});

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

  ngOnInit(): void {
    this.loadTransactions();
  }

  protected previousMonth(): void {
    const d = this.currentDate();
    this.currentDate.set(new Date(d.getFullYear(), d.getMonth() - 1, 1));
    this.installmentsMap.set({});
    this.installmentsLoading.set({});
    this.loadTransactions();
  }

  protected nextMonth(): void {
    const d = this.currentDate();
    this.currentDate.set(new Date(d.getFullYear(), d.getMonth() + 1, 1));
    this.installmentsMap.set({});
    this.installmentsLoading.set({});
    this.loadTransactions();
  }

  protected openNewForm(): void {
    const ref = this.dialog.open(TransactionFormComponent, {
      width: '560px',
      maxWidth: '100vw',
      panelClass: 'form-dialog',
      data: { transaction: null, month: this.currentMonth() },
    });

    ref.afterClosed().subscribe((saved: boolean) => {
      if (saved) {
        this.loadTransactions();
        this.snackBar.open('Lancamento criado com sucesso!', 'Fechar', {
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

    ref.afterClosed().subscribe((saved: boolean) => {
      if (saved) {
        this.loadTransactions();
        this.snackBar.open('Lancamento atualizado!', 'Fechar', {
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
          this.snackBar.open('Lancamento excluido.', 'Fechar', {
            duration: 3000,
          });
        },
        error: (err: { error?: { message?: string } }) => {
          const message =
            err?.error?.message ?? 'Erro ao excluir lancamento. Tente novamente.';
          this.snackBar.open(message, 'Fechar', {
            duration: 4000,
            panelClass: 'snack--error',
          });
        },
      });
    });
  }

  // Carrega parcelas de uma transação ao expandir o painel (lazy loading)
  protected onInstallmentPanelOpened(tx: Transaction): void {
    if (!tx.installmentsTotal || tx.installmentsTotal <= 1) return;
    // Se já carregou, não recarrega
    if (this.installmentsMap()[tx.id]) return;

    const loadingMap = { ...this.installmentsLoading(), [tx.id]: true };
    this.installmentsLoading.set(loadingMap);

    this.installmentService.getInstallments(tx.id).subscribe({
      next: (installments: InstallmentResponse[]) => {
        this.installmentsMap.set({ ...this.installmentsMap(), [tx.id]: installments });
        const updated = { ...this.installmentsLoading(), [tx.id]: false };
        this.installmentsLoading.set(updated);
      },
      error: () => {
        const updated = { ...this.installmentsLoading(), [tx.id]: false };
        this.installmentsLoading.set(updated);
        this.snackBar.open('Erro ao carregar parcelas.', 'Fechar', {
          duration: 4000,
          panelClass: 'snack--error',
        });
      },
    });
  }

  // Retorna o número da parcela cujo referenceMonth coincide com o mês selecionado
  protected currentInstallmentNumber(tx: Transaction): number {
    const month = this.currentMonth(); // "yyyy-MM"
    const installments = this.installmentsMap()[tx.id];
    if (installments) {
      const found = installments.find(
        (i) => i.referenceMonth.substring(0, 7) === month
      );
      return found ? found.number : 1;
    }
    // Antes de carregar as parcelas, estima pela data da transação e mês atual
    return this.estimateCurrentInstallmentNumber(tx);
  }

  // Estimativa simples: calcula quantos meses a mais o mês atual está em relação à data da compra
  private estimateCurrentInstallmentNumber(tx: Transaction): number {
    const month = this.currentMonth(); // "yyyy-MM"
    const txDate = tx.date.substring(0, 7); // "yyyy-MM"
    const [txYear, txMon] = txDate.split('-').map(Number);
    const [curYear, curMon] = month.split('-').map(Number);
    const diff = (curYear - txYear) * 12 + (curMon - txMon) + 1;
    if (diff < 1) return 1;
    if (diff > tx.installmentsTotal) return tx.installmentsTotal;
    return diff;
  }

  // Verifica se a parcela é do mês atual (para destaque visual)
  protected isCurrentMonthInstallment(installment: InstallmentResponse): boolean {
    const month = this.currentMonth(); // "yyyy-MM"
    return installment.referenceMonth.substring(0, 7) === month;
  }

  // Formata referenceMonth ("yyyy-MM-dd") como "mmm/yyyy" em pt-br
  protected formatReferenceMonth(referenceMonth: string): string {
    // referenceMonth é sempre "yyyy-MM-01"
    const [year, month] = referenceMonth.split('-');
    const date = new Date(Number(year), Number(month) - 1, 1);
    return date.toLocaleDateString('pt-BR', { month: 'short', year: 'numeric' })
      .replace('.', '')
      .replace(' de ', '/');
  }

  protected categoryIcon(categoryName: string): string {
    const name = categoryName.toLowerCase();
    if (name.includes('mercado') || name.includes('alimenta')) return 'shopping_cart';
    if (name.includes('salario') || name.includes('renda') || name.includes('receita')) return 'work';
    if (name.includes('transporte') || name.includes('uber') || name.includes('combustivel')) return 'directions_car';
    if (name.includes('lazer') || name.includes('cinema') || name.includes('entretenimento')) return 'movie';
    if (name.includes('saude') || name.includes('farmacia') || name.includes('medico')) return 'local_hospital';
    if (name.includes('educacao') || name.includes('curso')) return 'school';
    if (name.includes('moradia') || name.includes('aluguel')) return 'home';
    if (name.includes('viagem')) return 'flight';
    return 'receipt';
  }

  protected paymentMethodLabel(method: PaymentMethod): string {
    const labels: Record<PaymentMethod, string> = {
      CASH: 'Dinheiro',
      DEBIT: 'Debito',
      CREDIT: 'Credito',
      PIX: 'Pix',
      TRANSFER: 'Transferencia',
    };
    return labels[method] ?? method;
  }

  protected splitRuleLabel(rule: SplitRule): string {
    const labels: Record<SplitRule, string> = {
      FIFTY_FIFTY: '50/50',
      PERSON_A: 'So A',
      PERSON_B: 'So B',
      PROPORTIONAL: 'Proporcional',
    };
    return labels[rule] ?? rule;
  }

  private loadTransactions(): void {
    this.loading.set(true);
    this.transactionService.getTransactions(this.currentMonth()).subscribe({
      next: (data: Transaction[]) => {
        const sorted = [...data].sort(
          (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()
        );
        this.transactions.set(sorted);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading.set(false);
        const message =
          err?.error?.message ?? 'Erro ao carregar lancamentos. Tente novamente.';
        this.snackBar.open(message, 'Fechar', {
          duration: 4000,
          panelClass: 'snack--error',
        });
      },
    });
  }
}
