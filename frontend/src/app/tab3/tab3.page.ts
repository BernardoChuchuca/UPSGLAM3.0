import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonHeader, IonToolbar, IonTitle, IonContent,
  IonItem, IonLabel, IonTextarea, IonSelect,
  IonSelectOption, IonButton, IonIcon, IonSpinner, IonToast
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  sparklesOutline, imageOutline, cameraOutline,
  cloudUploadOutline, hardwareChipOutline
} from 'ionicons/icons';

import { PostService, CreatePostRequest } from '../services/post';

@Component({
  selector: 'app-tab3',
  templateUrl: 'tab3.page.html',
  styleUrls: ['tab3.page.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    IonHeader, IonToolbar, IonTitle, IonContent,
    IonItem, IonLabel, IonTextarea, IonSelect,
    IonSelectOption, IonButton, IonIcon, IonSpinner, IonToast
  ],
})
export class Tab3Page {

  selectedFile: File | null = null;
  previewUrl: string | null = null;
  caption: string = '';
  selectedFilter: string = 'laplaciano';

  isLoading: boolean = false;
  toastOpen: boolean = false;
  toastMessage: string = '';
  toastColor: string = 'success';

  constructor(private postService: PostService) {
    addIcons({
      sparklesOutline, imageOutline, cameraOutline,
      cloudUploadOutline, hardwareChipOutline
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.selectedFile = file;

    const reader = new FileReader();
    reader.onload = () => {
      this.previewUrl = reader.result as string;
    };
    reader.readAsDataURL(file);
  }

  publicarPost(): void {
    if (!this.selectedFile) {
      this.mostrarToast('Por favor selecciona una imagen.', 'warning');
      return;
    }

    this.isLoading = true;

    const postData: CreatePostRequest = {
      caption: this.caption,
      filterName: this.selectedFilter,
      userId: 1
    };

    this.postService.crearPost(this.selectedFile, postData).subscribe({
      next: (res) => {
        console.log('✅ Post creado:', res);
        this.mostrarToast('¡Publicación creada con éxito! 🎉', 'success');
        this.resetForm();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('❌ Error:', err);
        this.mostrarToast(`Error al publicar: ${err.message}`, 'danger');
        this.isLoading = false;
      }
    });
  }

  private resetForm(): void {
    this.selectedFile = null;
    this.previewUrl = null;
    this.caption = '';
    this.selectedFilter = 'laplaciano';
  }

  private mostrarToast(mensaje: string, color: string): void {
    this.toastMessage = mensaje;
    this.toastColor = color;
    this.toastOpen = true;
  }
}