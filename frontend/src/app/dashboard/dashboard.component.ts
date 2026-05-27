import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="dashboard-placeholder">
      <h1>Bem-vindo, {{ auth.currentUser()?.name }}!</h1>
      <p>Dashboard em construção — Sprint 02.</p>
      <button mat-stroked-button (click)="auth.logout()">
        <mat-icon>logout</mat-icon> Sair
      </button>
    </div>
  `,
  styles: [`
    .dashboard-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100vh;
      gap: 16px;
      font-family: var(--font-body);
      color: var(--color-text-primary);
    }
    h1 { font-family: var(--font-display); margin: 0; color: var(--color-primary); }
    p  { color: var(--color-text-secondary); }
  `],
})
export class DashboardComponent {
  protected auth = inject(AuthService);
}
