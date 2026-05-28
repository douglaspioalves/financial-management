import { Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CardService } from '../services/card.service';
import { PersonService } from '../../core/services/person.service';
import { Person } from '../../core/models/person.models';
import { Card } from '../models/card.model';

function dayRangeValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const val = Number(control.value);
    if (isNaN(val) || val < 1 || val > 31) {
      return { dayRange: true };
    }
    return null;
  };
}

@Component({
  selector: 'app-card-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './card-form.component.html',
  styleUrl: './card-form.component.scss',
})
export class CardFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private cardService = inject(CardService);
  private personService = inject(PersonService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private snackBar = inject(MatSnackBar);

  protected isEditMode = false;
  protected editCardId: string | null = null;
  protected editCardVersion: number | null = null;

  protected loading = signal(false);
  protected loadingData = signal(true);
  protected apiError = signal<string | null>(null);
  protected fieldErrors = signal<Record<string, string>>({});
  protected persons = signal<Person[]>([]);

  protected form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    ownerPersonId: ['', [Validators.required]],
    closingDay: [
      null as number | null,
      [Validators.required, dayRangeValidator()],
    ],
    dueDay: [
      null as number | null,
      [Validators.required, dayRangeValidator()],
    ],
  });

  ngOnInit(): void {
    this.editCardId = this.route.snapshot.paramMap.get('id');
    this.isEditMode = !!this.editCardId;

    this.loadPersons();

    if (this.isEditMode && this.editCardId) {
      this.loadCard(this.editCardId);
    } else {
      this.loadingData.set(false);
    }
  }

  private loadPersons(): void {
    this.personService.getPersons().subscribe({
      next: (ps) => this.persons.set(ps),
      error: () => {
        this.apiError.set('Erro ao carregar os participantes. Tente recarregar a página.');
      },
    });
  }

  private loadCard(id: string): void {
    this.loadingData.set(true);
    this.cardService.getById(id).subscribe({
      next: (card: Card) => {
        this.editCardVersion = card.version;
        this.form.patchValue({
          name: card.name,
          ownerPersonId: card.ownerPersonId,
          closingDay: card.closingDay,
          dueDay: card.dueDay,
        });
        this.loadingData.set(false);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loadingData.set(false);
        this.apiError.set(
          err?.error?.message ?? 'Erro ao carregar o cartão. Tente novamente.'
        );
      },
    });
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
    const req = {
      name: v.name!,
      ownerPersonId: v.ownerPersonId!,
      closingDay: v.closingDay!,
      dueDay: v.dueDay!,
    };

    if (this.isEditMode && this.editCardId) {
      this.cardService
        .update(this.editCardId, { ...req, version: this.editCardVersion! })
        .subscribe({
          next: () => {
            this.loading.set(false);
            this.snackBar.open('Cartão atualizado com sucesso.', 'Fechar', {
              duration: 3000,
            });
            this.router.navigate(['/cards']);
          },
          error: (err: { error?: { message?: string; errors?: Record<string, string> } }) => {
            this.handleError(err);
          },
        });
    } else {
      this.cardService.create(req).subscribe({
        next: () => {
          this.loading.set(false);
          this.snackBar.open('Cartão criado com sucesso.', 'Fechar', {
            duration: 3000,
          });
          this.router.navigate(['/cards']);
        },
        error: (err: { error?: { message?: string; errors?: Record<string, string> } }) => {
          this.handleError(err);
        },
      });
    }
  }

  private handleError(err: {
    error?: { message?: string; errors?: Record<string, string> };
  }): void {
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

  protected cancel(): void {
    this.router.navigate(['/cards']);
  }

  protected dayNumbers(): number[] {
    return Array.from({ length: 31 }, (_, i) => i + 1);
  }
}
