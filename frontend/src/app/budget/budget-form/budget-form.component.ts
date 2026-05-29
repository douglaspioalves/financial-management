import {
  Component,
  OnInit,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialogRef,
  MatDialogModule,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CategoryService } from '../../core/services/category.service';
import { BudgetService } from '../services/budget.service';
import { BudgetResponse } from '../models/budget.model';
import { Category } from '../../core/models/category.models';

export interface BudgetFormDialogData {
  budget: BudgetResponse | null;
  month: string; // "2026-05-01"
}

@Component({
  selector: 'app-budget-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './budget-form.component.html',
  styleUrl: './budget-form.component.scss',
})
export class BudgetFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private budgetService = inject(BudgetService);
  private categoryService = inject(CategoryService);
  private snackBar = inject(MatSnackBar);
  private dialogRef = inject(MatDialogRef<BudgetFormComponent>);
  protected data: BudgetFormDialogData = inject(MAT_DIALOG_DATA);

  protected categories = signal<Category[]>([]);
  protected loadingCategories = signal(false);
  protected saving = signal(false);

  protected isEditing = !!this.data.budget;

  protected form = this.fb.group({
    categoryId: [
      { value: this.data.budget?.categoryId ?? '', disabled: this.isEditing },
      Validators.required,
    ],
    limitAmount: [
      this.data.budget?.limitAmount ?? null,
      [Validators.required, Validators.min(0.01)],
    ],
  });

  protected get monthLabel(): string {
    const [year, month] = this.data.month.split('-');
    const date = new Date(Number(year), Number(month) - 1, 1);
    return date.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });
  }

  ngOnInit(): void {
    this.loadCategories();
  }

  private loadCategories(): void {
    this.loadingCategories.set(true);
    this.categoryService.getCategories().subscribe({
      next: (cats) => {
        // Filtra para categorias de despesa ou ambas (para orçamento faz sentido apenas EXPENSE/BOTH)
        this.categories.set(
          cats.filter((c) => c.type === 'EXPENSE' || c.type === 'BOTH')
        );
        this.loadingCategories.set(false);
      },
      error: () => {
        this.loadingCategories.set(false);
        this.snackBar.open(
          'Erro ao carregar categorias. Tente novamente.',
          'Fechar',
          { duration: 4000, panelClass: 'snack--error' }
        );
      },
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const rawValue = this.form.getRawValue();
    const req = {
      categoryId: rawValue.categoryId!,
      month: this.data.month,
      limitAmount: Number(rawValue.limitAmount),
    };

    this.saving.set(true);

    if (this.isEditing && this.data.budget) {
      this.budgetService
        .update(this.data.budget.id, { ...req, version: this.data.budget.version })
        .subscribe({
          next: () => {
            this.saving.set(false);
            this.dialogRef.close(true);
          },
          error: (err: { error?: { message?: string } }) => {
            this.saving.set(false);
            const message =
              err?.error?.message ?? 'Erro ao atualizar orçamento. Tente novamente.';
            this.snackBar.open(message, 'Fechar', {
              duration: 5000,
              panelClass: 'snack--error',
            });
          },
        });
    } else {
      this.budgetService.create(req).subscribe({
        next: () => {
          this.saving.set(false);
          this.dialogRef.close(true);
        },
        error: (err: { error?: { message?: string } }) => {
          this.saving.set(false);
          const message =
            err?.error?.message ?? 'Erro ao criar orçamento. Tente novamente.';
          this.snackBar.open(message, 'Fechar', {
            duration: 5000,
            panelClass: 'snack--error',
          });
        },
      });
    }
  }

  protected cancel(): void {
    this.dialogRef.close(false);
  }
}
