import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { CategoryService } from '../../core/services/category.service';
import { PersonService } from '../../core/services/person.service';
import { TransactionService } from '../../core/services/transaction.service';
import { Category } from '../../core/models/category.models';
import { Person } from '../../core/models/person.models';
import {
  Transaction,
  CreateTransactionRequest,
  UpdateTransactionRequest,
  TransactionType,
} from '../../core/models/transaction.models';

export interface TransactionFormDialogData {
  transaction: Transaction | null;
  month: string;
}

@Component({
  selector: 'app-transaction-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDialogModule,
  ],
  templateUrl: './transaction-form.component.html',
  styleUrl: './transaction-form.component.scss',
})
export class TransactionFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private categoryService = inject(CategoryService);
  private personService = inject(PersonService);
  private transactionService = inject(TransactionService);
  private dialogRef = inject(MatDialogRef<TransactionFormComponent>);
  protected data: TransactionFormDialogData = inject(MAT_DIALOG_DATA);

  protected loading = signal(false);
  protected loadingCategories = signal(false);
  protected loadingPersons = signal(false);
  protected apiError = signal<string | null>(null);
  protected fieldErrors = signal<Record<string, string>>({});
  protected categories = signal<Category[]>([]);
  protected persons = signal<Person[]>([]);

  protected form = this.fb.group({
    type: ['EXPENSE' as TransactionType, [Validators.required]],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    date: [this.todayString(), [Validators.required]],
    description: ['', [Validators.maxLength(255)]],
    categoryId: ['', [Validators.required]],
    paidByPersonId: ['', [Validators.required]],
    paymentMethod: ['PIX', [Validators.required]],
    splitRule: ['FIFTY_FIFTY', [Validators.required]],
  });

  ngOnInit(): void {
    this.loadCategories();
    this.loadPersons();

    const tx = this.data.transaction;
    if (tx) {
      this.form.patchValue({
        type: tx.type,
        amount: tx.amount,
        date: tx.date,
        description: tx.description ?? '',
        categoryId: tx.category.id,
        paidByPersonId: tx.paidByPerson.id,
        paymentMethod: tx.paymentMethod,
        splitRule: tx.splitRule,
      });
    }
  }

  protected setType(type: TransactionType): void {
    this.form.patchValue({ type });
    this.form.patchValue({ categoryId: '' });
    this.loadCategories();
  }

  protected submit(): void {
    if (this.form.invalid || this.loading()) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.apiError.set(null);
    this.fieldErrors.set({});

    const v = this.form.getRawValue();

    if (this.data.transaction) {
      const req: UpdateTransactionRequest = {
        type: v.type as TransactionType,
        amount: v.amount!,
        date: v.date!,
        description: v.description || undefined,
        categoryId: v.categoryId!,
        paidByPersonId: v.paidByPersonId!,
        paymentMethod: v.paymentMethod as UpdateTransactionRequest['paymentMethod'],
        splitRule: v.splitRule as UpdateTransactionRequest['splitRule'],
        installmentsTotal: 1,
        version: this.data.transaction.version,
      };

      this.transactionService.updateTransaction(this.data.transaction.id, req).subscribe({
        next: () => {
          this.loading.set(false);
          this.dialogRef.close(true);
        },
        error: (err: { error?: { message?: string; errors?: Record<string, string> } }) => {
          this.handleError(err);
        },
      });
    } else {
      const req: CreateTransactionRequest = {
        type: v.type as TransactionType,
        amount: v.amount!,
        date: v.date!,
        description: v.description || undefined,
        categoryId: v.categoryId!,
        paidByPersonId: v.paidByPersonId!,
        paymentMethod: v.paymentMethod as CreateTransactionRequest['paymentMethod'],
        splitRule: v.splitRule as CreateTransactionRequest['splitRule'],
        installmentsTotal: 1,
      };

      this.transactionService.createTransaction(req).subscribe({
        next: () => {
          this.loading.set(false);
          this.dialogRef.close(true);
        },
        error: (err: { error?: { message?: string; errors?: Record<string, string> } }) => {
          this.handleError(err);
        },
      });
    }
  }

  private handleError(err: { error?: { message?: string; errors?: Record<string, string> } }): void {
    this.loading.set(false);
    const error = err?.error;
    if (error?.message) {
      this.apiError.set(error.message);
    } else {
      this.apiError.set('Ocorreu um erro inesperado. Tente novamente.');
    }
    if (error?.errors) {
      this.fieldErrors.set(error.errors);
    }
  }

  private loadCategories(): void {
    this.loadingCategories.set(true);
    const type = this.form.value.type ?? 'EXPENSE';
    this.categoryService.getCategories(type).subscribe({
      next: (cats: Category[]) => {
        this.categories.set(cats);
        this.loadingCategories.set(false);
      },
      error: () => {
        this.loadingCategories.set(false);
      },
    });
  }

  private loadPersons(): void {
    this.loadingPersons.set(true);
    this.personService.getPersons().subscribe({
      next: (ps: Person[]) => {
        this.persons.set(ps);
        this.loadingPersons.set(false);
      },
      error: () => {
        this.loadingPersons.set(false);
      },
    });
  }

  private todayString(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
