import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SettlementService } from './settlement.service';
import { SettlementResponse, PersonSettlementDTO } from './settlement.model';
import { ThemeService } from '../core/theme/theme.service';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-settlement',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatTooltipModule,
  ],
  templateUrl: './settlement.component.html',
  styleUrl: './settlement.component.scss',
})
export class SettlementComponent implements OnInit {
  private settlementService = inject(SettlementService);
  private snackBar = inject(MatSnackBar);
  protected theme = inject(ThemeService);
  protected auth = inject(AuthService);

  private currentDate = signal(new Date());

  protected loading = signal(false);
  protected data = signal<SettlementResponse | null>(null);

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

  /** Retorna o objeto da pessoa credora (quem vai receber) */
  protected creditorPerson = computed<PersonSettlementDTO | null>(() => {
    const d = this.data();
    if (!d || !d.creditor) return null;
    return d.creditor === 'PERSON_A' ? d.personA : d.personB;
  });

  /** Retorna o objeto da pessoa devedora (quem vai pagar) */
  protected debtorPerson = computed<PersonSettlementDTO | null>(() => {
    const d = this.data();
    if (!d || !d.debtor) return null;
    return d.debtor === 'PERSON_A' ? d.personA : d.personB;
  });

  ngOnInit(): void {
    this.loadSettlement();
  }

  protected previousMonth(): void {
    const d = this.currentDate();
    this.currentDate.set(new Date(d.getFullYear(), d.getMonth() - 1, 1));
    this.loadSettlement();
  }

  protected nextMonth(): void {
    const d = this.currentDate();
    this.currentDate.set(new Date(d.getFullYear(), d.getMonth() + 1, 1));
    this.loadSettlement();
  }

  /**
   * Formata valor monetário em pt-BR com sinal explícito.
   * positive=true → prefixo "+", positive=false → prefixo "−"
   */
  protected formatBalance(value: number): string {
    const abs = Math.abs(value);
    const formatted = abs.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
    });
    return value >= 0 ? `+${formatted}` : `−${formatted}`;
  }

  protected formatCurrency(value: number): string {
    return value.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
    });
  }

  private loadSettlement(): void {
    this.loading.set(true);
    this.data.set(null);
    this.settlementService.getSettlement(this.currentMonth()).subscribe({
      next: (response) => {
        this.data.set(response);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading.set(false);
        const message =
          err?.error?.message ?? 'Erro ao carregar acerto. Tente novamente.';
        this.snackBar.open(message, 'Fechar', {
          duration: 5000,
          panelClass: 'snack--error',
        });
      },
    });
  }
}
