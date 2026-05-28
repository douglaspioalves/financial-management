import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CardService } from '../services/card.service';
import { Card } from '../models/card.model';
import {
  CardDeleteConfirmComponent,
  CardDeleteConfirmData,
} from './card-delete-confirm.component';

@Component({
  selector: 'app-card-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule,
  ],
  templateUrl: './card-list.component.html',
  styleUrl: './card-list.component.scss',
})
export class CardListComponent implements OnInit {
  private cardService = inject(CardService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);
  private router = inject(Router);

  protected cards = signal<Card[]>([]);
  protected loading = signal(true);
  protected errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.loadCards();
  }

  protected loadCards(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.cardService.getAll().subscribe({
      next: (cards) => {
        this.cards.set(cards);
        this.loading.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading.set(false);
        this.errorMessage.set(
          err?.error?.message ?? 'Erro ao carregar cartões. Tente novamente.'
        );
      },
    });
  }

  protected navigateToNew(): void {
    this.router.navigate(['/cards/new']);
  }

  protected navigateToEdit(card: Card): void {
    this.router.navigate(['/cards/edit', card.id]);
  }

  protected confirmDelete(card: Card): void {
    const ref = this.dialog.open(CardDeleteConfirmComponent, {
      width: '360px',
      data: { cardName: card.name } as CardDeleteConfirmData,
    });

    ref.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.deleteCard(card);
      }
    });
  }

  private deleteCard(card: Card): void {
    this.cardService.delete(card.id).subscribe({
      next: () => {
        this.snackBar.open('Cartão excluído com sucesso.', 'Fechar', {
          duration: 3000,
        });
        this.loadCards();
      },
      error: (err: { error?: { message?: string } }) => {
        const msg =
          err?.error?.message ?? 'Erro ao excluir o cartão. Tente novamente.';
        this.snackBar.open(msg, 'Fechar', { duration: 5000 });
      },
    });
  }

  protected formatDay(day: number): string {
    return `dia ${day}`;
  }
}
