import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonHeader, IonToolbar, IonTitle, IonButtons, IonButton,
  IonIcon, IonContent, IonSegment, IonSegmentButton, IonLabel,
  IonCard, IonCardHeader, IonChip, IonCardContent, AlertController,
  IonModal
} from '@ionic/angular/standalone';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { PostService } from '../services/post';
import { addIcons } from 'ionicons';
import { 
  logOutOutline, personOutline, imagesOutline, hardwareChipOutline,
  checkmarkCircleOutline, alertCircleOutline, timeOutline, gridOutline, cubeOutline,
  trashOutline
} from 'ionicons/icons';

@Component({
  selector: 'app-tab2',
  templateUrl: 'tab2.page.html',
  styleUrls: ['tab2.page.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    IonHeader, IonToolbar, IonTitle, IonButtons, IonButton,
    IonIcon, IonContent, IonSegment, IonSegmentButton, IonLabel,
    IonCard, IonCardHeader, IonChip, IonCardContent, IonModal
  ]
})
export class Tab2Page implements OnInit {
  profile: any = null;
  posts: any[] = [];
  history: any[] = [];
  activeTab: string = 'posts';
  selectedPost: any = null;

  constructor(
    private authService: AuthService,
    private postService: PostService,
    private router: Router,
    private alertController: AlertController
  ) {
    addIcons({ 
      logOutOutline, personOutline, imagesOutline, hardwareChipOutline,
      checkmarkCircleOutline, alertCircleOutline, timeOutline, gridOutline, cubeOutline,
      trashOutline
    });
  }

  ngOnInit() {
    this.cargarDatosPerfil();
  }

  ionViewWillEnter() {
    this.cargarDatosPerfil();
  }

  cargarDatosPerfil() {
    this.profile = this.authService.getProfile();
    if (this.profile && this.profile.id) {
      this.postService.getUserPosts(this.profile.id).subscribe({
        next: (data) => {
          this.posts = data;
        },
        error: (err) => {
          console.error('Error al cargar posts del usuario:', err);
        }
      });
      this.cargarHistorialProcesamiento();
    }
  }

  cargarHistorialProcesamiento() {
    this.postService.getProcessingHistory().subscribe({
      next: (data) => {
        this.history = data;
        console.log('¡Historial GPU cargado!:', data);
      },
      error: (err) => {
        console.error('Error al cargar historial GPU:', err);
      }
    });
  }

  async confirmarBorrarPost(postId: string) {
    const alert = await this.alertController.create({
      header: 'Eliminar publicación',
      message: '¿Estás seguro de que deseas borrar esta publicación? Esta acción no se puede deshacer.',
      buttons: [
        {
          text: 'Cancelar',
          role: 'cancel'
        },
        {
          text: 'Eliminar',
          role: 'destructive',
          handler: () => {
            this.borrarPost(postId);
          }
        }
      ]
    });
    await alert.present();
  }

  borrarPost(postId: string) {
    this.postService.deletePost(postId).subscribe({
      next: () => {
        console.log('Post eliminado:', postId);
        this.selectedPost = null;
        this.cargarDatosPerfil();
      },
      error: (err) => {
        console.error('Error al eliminar post:', err);
      }
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
