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
import { logInOutline, personAddOutline } from 'ionicons/icons';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: 'login.page.html',
  styleUrls: ['login.page.scss'],
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    IonHeader, IonToolbar, IonTitle, IonContent,
    IonItem, IonLabel, IonInput, IonButton, IonIcon,
    IonSpinner, IonToast
  ]
})
export class LoginPage {
  email: string = '';
  password: string = '';
  isLoading: boolean = false;
  toastOpen: boolean = false;
  toastMessage: string = '';
  toastColor: string = 'danger';

  constructor(private authService: AuthService, private router: Router) {
    addIcons({ logInOutline, personAddOutline });
  }

  login(): void {
    if (!this.email || !this.password) {
      this.mostrarToast('Por favor completa todos los campos.', 'warning');
      return;
    }
    this.isLoading = true;
    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigate(['/tabs/tab1']);
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Login error detail:', err);
        const detailedError = err.error?.message || err.message || JSON.stringify(err);
        this.mostrarToast('Error en login: ' + detailedError, 'danger');
      }
    });
  }

  irARegistro(): void {
    this.router.navigate(['/register']);
  }

  private mostrarToast(mensaje: string, color: string): void {
    this.toastMessage = mensaje;
    this.toastColor = color;
    this.toastOpen = true;
  }
}