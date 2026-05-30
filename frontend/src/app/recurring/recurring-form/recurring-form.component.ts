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
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CategoryService } from '../../core/services/category.service';
import { PersonService } from '../../core/services/person.service';
import { RecurringRuleService } from '../recurring-rule.service';
import { Category } from '../../core/models/category.models';
import { Person } from '../../core/models/person.models';
import {
  CreateRecurringRuleRequest,
  RecurringFrequency,
  TransactionType,
  PaymentMethod,
  SplitRule,
} from '../models/recurring-rule.models';

@Component({
  selector: 'app-recurring-form',
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
    MatDatepickerModule,
    MatNativeDateModule,
    MatSnackBarModule,
  ],
  templateUrl: './recurring-form.component.html',
  styleUrl: './recurring-form.component.scss',
})
export class RecurringFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private recurringService = inject(RecurringRuleService);
  private categoryService = inject(CategoryService);
  private personService = inject(PersonService);
  private snackBar = inject(MatSnackBar);
  private dialogRef = inject(MatDialogRef<RecurringFormComponent>);
  protected data: unknown = inject(MAT_DIALOG_DATA);

  protected categories = signal<Category[]>([]);
  protected persons = signal<Person[]>([]);
  protected loadingData = signal(false);
  protected saving = signal(false);

  protected form = this.fb.group({
    description: ['', [Validators.required, Validators.maxLength(100)]],
    type: ['EXPENSE' as TransactionType, Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    categoryId: ['', Validators.required],
    paidByPersonId: ['', Validators.required],
    paymentMethod: ['CREDIT' as PaymentMethod, Validators.required],
    splitRule: ['FIFTY_FIFTY' as SplitRule, Validators.required],
    frequency: ['MONTHLY' as RecurringFrequency, Validators.required],
    nextDate: [null as Date | null, Validators.required],
  });

  readonly transactionTypes: { value: TransactionType; label: string }[] = [
    { value: 'EXPENSE', label: 'Despesa' },
    { value: 'INCOME', label: 'Receita' },
  ];

  readonly paymentMethods: { value: PaymentMethod; label: string }[] = [
    { value: 'CASH', label: 'Dinheiro' },
    { value: 'DEBIT', label: 'Débito' },
    { value: 'CREDIT', label: 'Crédito' },
    { value: 'PIX', label: 'Pix' },
    { value: 'TRANSFER', label: 'Transferência' },
  ];

  readonly splitRules: { value: SplitRule; label: string }[] = [
    { value: 'FIFTY_FIFTY', label: '50/50' },
    { value: 'PERSON_A', label: 'Pessoa A (100%)' },
    { value: 'PERSON_B', label: 'Pessoa B (100%)' },
    { value: 'PROPORTIONAL', label: 'Proporcional à renda' },
  ];

  readonly frequencies: { value: RecurringFrequency; label: string }[] = [
    { value: 'MONTHLY', label: 'Mensal' },
    { value: 'WEEKLY', label: 'Semanal' },
    { value: 'YEARLY', label: 'Anual' },
  ];

  ngOnInit(): void {
    this.loadFormData();
  }

  private loadFormData(): void {
    this.loadingData.set(true);

    let categoriesDone = false;
    let personsDone = false;

    const checkDone = () => {
      if (categoriesDone && personsDone) {
        this.loadingData.set(false);
      }
    };

    this.categoryService.getCategories().subscribe({
      next: (cats) => {
        this.categories.set(cats);
        categoriesDone = true;
        checkDone();
      },
      error: () => {
        categoriesDone = true;
        checkDone();
        this.snackBar.open('Erro ao carregar categorias.', 'Fechar', {
          duration: 4000,
          panelClass: 'snack--error',
        });
      },
    });

    this.personService.getPersons().subscribe({
      next: (persons) => {
        this.persons.set(persons);
        personsDone = true;
        checkDone();
      },
      error: () => {
        personsDone = true;
        checkDone();
        this.snackBar.open('Erro ao carregar pessoas.', 'Fechar', {
          duration: 4000,
          panelClass: 'snack--error',
        });
      },
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const nextDateObj = raw.nextDate as Date;
    const nextDateStr = this.formatDateToISO(nextDateObj);

    const request: CreateRecurringRuleRequest = {
      type: raw.type as TransactionType,
      amount: Number(raw.amount),
      description: raw.description!,
      categoryId: raw.categoryId!,
      paidByPersonId: raw.paidByPersonId!,
      paymentMethod: raw.paymentMethod as PaymentMethod,
      splitRule: raw.splitRule as SplitRule,
      frequency: raw.frequency as RecurringFrequency,
      nextDate: nextDateStr,
    };

    this.saving.set(true);
    this.recurringService.create(request).subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogRef.close(true);
      },
      error: (err: { error?: { message?: string } }) => {
        this.saving.set(false);
        const message =
          err?.error?.message ?? 'Erro ao criar regra recorrente. Tente novamente.';
        this.snackBar.open(message, 'Fechar', {
          duration: 5000,
          panelClass: 'snack--error',
        });
      },
    });
  }

  protected cancel(): void {
    this.dialogRef.close(false);
  }

  private formatDateToISO(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
