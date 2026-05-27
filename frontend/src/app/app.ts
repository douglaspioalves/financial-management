import { Component, OnInit, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HealthService } from './core/services/health.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  protected readonly title = signal('Gestor de Gastos');
  protected readonly backendStatus = signal<'loading' | 'ok' | 'error'>('loading');

  constructor(private healthService: HealthService) {}

  ngOnInit(): void {
    this.healthService.check().subscribe(response => {
      if (response && response.status === 'ok') {
        this.backendStatus.set('ok');
      } else {
        this.backendStatus.set('error');
      }
    });
  }
}
