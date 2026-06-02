import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  IonHeader, IonToolbar, IonTitle, IonContent,
  IonItem, IonLabel, IonInput, IonButton, IonIcon,
  IonSpinner, IonToast
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import { personAddOutline, arrowBackOutline } from 'ionicons/icons';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-register',
  templateUrl: 'register.page.html',
  styleUrls: ['register.page.scss'],
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    IonHeader, IonToolbar, IonTitle, IonContent,
    IonItem, IonLabel, IonInput, IonButton, IonIcon,
    IonSpinner, IonToast
  ]
})
export class RegisterPage {
  email: string = '';
  password: string = '';
  username: string = '';
  isLoading: boolean = false;
  toastOpen: boolean = false;
  toastMessage: string = '';
  toastColor: string = 'danger';

  constructor(private authService: AuthService, private router: Router) {
    addIcons({ personAddOutline, arrowBackOutline });
  }

  register(): void {
    if (!this.email || !this.password || !this.username) {
      this.mostrarToast('Por favor completa todos los campos.', 'warning');
      return;
    }
    this.isLoading = true;
    this.authService.register({
      email: this.email,
      password: this.password,
      username: this.username
    }).subscribe({
      next: () => {
        this.isLoading = false;
        this.mostrarToast('¡Cuenta creada! Inicia sesión.', 'success');
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Registration error detail:', err);
        const detailedError = err.error?.message || err.message || JSON.stringify(err);
        this.mostrarToast('Error al registrar: ' + detailedError, 'danger');
      }
    });
  }

  irALogin(): void {
    this.router.navigate(['/login']);
  }

  private mostrarToast(mensaje: string, color: string): void {
    this.toastMessage = mensaje;
    this.toastColor = color;
    this.toastOpen = true;
  }
}