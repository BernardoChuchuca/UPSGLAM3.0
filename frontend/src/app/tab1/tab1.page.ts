import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { PostService } from '../services/post';
import { addIcons } from 'ionicons';
import { heartOutline, imagesOutline } from 'ionicons/icons';

@Component({
  selector: 'app-tab1',
  templateUrl: 'tab1.page.html',
  styleUrls: ['tab1.page.scss'],
  standalone: true,
  imports: [IonicModule, CommonModule, HttpClientModule]
})
export class Tab1Page implements OnInit {
  posts: any[] = [];
  selectedFile: File | null = null;
  captionText: string = '';

  constructor(private postService: PostService, private http: HttpClient) {
    addIcons({ heartOutline, imagesOutline }); // ← aquí va
  }

  ngOnInit() {
    this.cargarFeed();
  }

  cargarFeed() {
    this.postService.getFeed().subscribe({
      next: (data: any[]) => {
        this.posts = data;
        console.log('¡Datos recibidos del backend!:', data);
      },
      error: (err: any) => {
        console.error('Error al conectar con el backend:', err);
      }
    });
  }

  onFileSelected(event: any) {
    if (event.target.files && event.target.files.length > 0) {
      this.selectedFile = event.target.files[0];
    }
  }

  onCaptionChange(event: any) {
    this.captionText = event.target.value || '';
  }

  enviarPostConFiltro() {
    if (!this.selectedFile) return;

    const formData = new FormData();
    const dataJson = {
      filterName: 'Laplacian',
      caption: this.captionText
    };

    formData.append('data', new Blob([JSON.stringify(dataJson)], { type: 'application/json' }));
    formData.append('image', this.selectedFile);

    console.log('Enviando imagen al backend...');

    this.http.post('http://localhost:8080/api/posts', formData).subscribe({
      next: (res: any) => {
        console.log('¡Procesamiento exitoso en GPU y publicado!', res);
        this.selectedFile = null;
        this.captionText = '';
        this.cargarFeed();
      },
      error: (err: any) => {
        console.error('Error al procesar la imagen en el backend:', err);
      }
    });
  }
}