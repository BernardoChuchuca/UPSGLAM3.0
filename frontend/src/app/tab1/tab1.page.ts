import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonHeader, IonToolbar, IonTitle, IonContent,
  IonList, IonCard, IonCardHeader, IonLabel,
  IonCardContent, IonButton, IonIcon, IonModal,
  IonButtons, IonFooter, IonItem, IonInput
} from '@ionic/angular/standalone';
import { HttpClient, HttpClientModule, HttpHeaders } from '@angular/common/http';
import { PostService } from '../services/post';
import { AuthService } from '../services/auth.service';
import { addIcons } from 'ionicons';
import { heartOutline, heart, chatbubbleOutline, trashOutline, sendOutline, imagesOutline } from 'ionicons/icons';

@Component({
  selector: 'app-tab1',
  templateUrl: 'tab1.page.html',
  styleUrls: ['tab1.page.scss'],
  standalone: true,
  imports: [
    CommonModule, FormsModule, HttpClientModule,
    IonHeader, IonToolbar, IonTitle, IonContent,
    IonList, IonCard, IonCardHeader, IonLabel,
    IonCardContent, IonButton, IonIcon, IonModal,
    IonButtons, IonFooter, IonItem, IonInput
  ]
})
export class Tab1Page implements OnInit {
  posts: any[] = [];
  selectedFile: File | null = null;
  captionText: string = '';
  
  currentUserProfile: any = null;
  selectedPostForComments: any = null;
  comments: any[] = [];
  newCommentText: string = '';
  isCommentsModalOpen: boolean = false;

  constructor(
    private postService: PostService,
    private authService: AuthService,
    private http: HttpClient
  ) {
    addIcons({ heartOutline, heart, chatbubbleOutline, trashOutline, sendOutline, imagesOutline });
  }

  ngOnInit() {
    this.currentUserProfile = this.authService.getProfile();
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

  toggleLike(post: any) {
    this.postService.toggleLike(post.id).subscribe({
      next: () => {
        post.likedByMe = !post.likedByMe;
        post.likeCount = post.likedByMe ? (post.likeCount + 1) : Math.max(0, post.likeCount - 1);
      },
      error: (err) => {
        console.error('Error al cambiar like:', err);
      }
    });
  }

  abrirComentarios(post: any) {
    this.selectedPostForComments = post;
    this.comments = [];
    this.newCommentText = '';
    this.cargarComentarios(post.id);
    this.isCommentsModalOpen = true;
  }

  cargarComentarios(postId: string) {
    this.postService.getComments(postId).subscribe({
      next: (data: any[]) => {
        this.comments = data;
      },
      error: (err) => {
        console.error('Error al cargar comentarios:', err);
      }
    });
  }

  agregarComentario() {
    if (!this.newCommentText.trim() || !this.selectedPostForComments) return;
    
    this.postService.addComment(this.selectedPostForComments.id, this.newCommentText).subscribe({
      next: (res) => {
        this.newCommentText = '';
        this.cargarComentarios(this.selectedPostForComments.id);
      },
      error: (err) => {
        console.error('Error al agregar comentario:', err);
      }
    });
  }

  borrarComentario(commentId: string) {
    if (!this.selectedPostForComments) return;

    this.postService.deleteComment(this.selectedPostForComments.id, commentId).subscribe({
      next: () => {
        this.cargarComentarios(this.selectedPostForComments.id);
      },
      error: (err) => {
        console.error('Error al eliminar comentario:', err);
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

    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    const formData = new FormData();
    const dataJson = {
      filterName: 'Laplacian',
      caption: this.captionText
    };

    formData.append('data', new Blob([JSON.stringify(dataJson)], { type: 'application/json' }));
    formData.append('image', this.selectedFile);

    console.log('Enviando imagen al backend...');

    this.http.post('http://localhost:8080/api/posts', formData, { headers }).subscribe({
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