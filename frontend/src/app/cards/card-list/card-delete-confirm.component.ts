import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

export interface CardDeleteConfirmData {
  cardName: string;
}

@Component({
  selector: 'app-card-delete-confirm',
  standalone: true,
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  template: `
    <div class="confirm-dialog">
      <div class="confirm-dialog__icon">
        <mat-icon>credit_card_off</mat-icon>
      </div>
      <h2 mat-dialog-title>Excluir cartão</h2>
      <mat-dialog-content>
        <p>Tem certeza que deseja excluir o cartão <strong>{{ data.cardName }}</strong>?</p>
        <p class="confirm-dialog__warning">Esta ação não pode ser desfeita.</p>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-stroked-button mat-dialog-close>Cancelar</button>
        <button mat-flat-button color="warn" [mat-dialog-close]="true">Excluir</button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .confirm-dialog {
      padding: 8px 4px 0;
    }
    .confirm-dialog__icon {
      display: flex;
      justify-content: center;
      margin-bottom: 8px;
      mat-icon {
        font-size: 40px;
        width: 40px;
        height: 40px;
        color: var(--color-expense);
      }
    }
    h2 {
      font-family: var(--font-display) !important;
      text-align: center;
    }
    p {
      margin: 0 0 8px;
      color: var(--color-text-primary);
    }
    .confirm-dialog__warning {
      color: var(--color-text-secondary);
      font-size: 0.875rem;
    }
  `],
})
export class CardDeleteConfirmComponent {
  protected data: CardDeleteConfirmData = inject(MAT_DIALOG_DATA);
  protected dialogRef = inject(MatDialogRef<CardDeleteConfirmComponent>);
}
