import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  IonHeader, IonToolbar, IonTitle, IonButtons, IonButton,
  IonIcon, IonContent, IonSegment, IonSegmentButton, IonLabel,
  IonCard, IonCardHeader, IonChip, IonCardContent, AlertController,
  IonModal, IonList, IonItem, IonAvatar
} from '@ionic/angular/standalone';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { PostService } from '../services/post';
import { addIcons } from 'ionicons';
import { 
  logOutOutline, personOutline, imagesOutline, hardwareChipOutline,
  checkmarkCircleOutline, alertCircleOutline, timeOutline, gridOutline, cubeOutline,
  trashOutline, repeatOutline, repeat, peopleOutline, people
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
    IonCard, IonCardHeader, IonChip, IonCardContent, IonModal,
    IonList, IonItem, IonAvatar
  ]
})
export class Tab2Page implements OnInit {
  profile: any = null;
  posts: any[] = [];
  reposts: any[] = [];
  history: any[] = [];
  activeTab: string = 'posts';
  selectedPost: any = null;

  // Listas flotantes
  isFollowersModalOpen: boolean = false;
  isFollowingModalOpen: boolean = false;
  followersList: any[] = [];
  followingList: any[] = [];

  constructor(
    private authService: AuthService,
    private postService: PostService,
    private router: Router,
    private alertController: AlertController
  ) {
    addIcons({ 
      logOutOutline, personOutline, imagesOutline, hardwareChipOutline,
      checkmarkCircleOutline, alertCircleOutline, timeOutline, gridOutline, cubeOutline,
      trashOutline, repeatOutline, repeat, peopleOutline, people
    });
  }

  ngOnInit() {
    this.cargarDatosPerfil();
  }

  ionViewWillEnter() {
    this.cargarDatosPerfil();
  }

  cargarDatosPerfil() {
    this.postService.getMyProfile().subscribe({
      next: (profileData) => {
        this.profile = profileData;
        if (this.profile && this.profile.id) {
          // Cargar publicaciones propias
          this.postService.getUserPosts(this.profile.id).subscribe({
            next: (data) => {
              this.posts = data;
            },
            error: (err) => {
              console.error('Error al cargar posts del usuario:', err);
            }
          });
          // Cargar publicaciones reposteadas
          this.postService.getUserReposts(this.profile.id).subscribe({
            next: (data) => {
              this.reposts = data;
            },
            error: (err) => {
              console.error('Error al cargar reposts del usuario:', err);
            }
          });
          this.cargarHistorialProcesamiento();
        }
      },
      error: (err) => {
        console.error('Error al cargar perfil del backend:', err);
        // Fallback al perfil local
        this.profile = this.authService.getProfile();
      }
    });
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

  abrirSeguidores() {
    if (!this.profile) return;
    this.postService.getFollowers(this.profile.id).subscribe({
      next: (data) => {
        this.followersList = data;
        this.isFollowersModalOpen = true;
      },
      error: (err) => {
        console.error('Error al obtener seguidores:', err);
      }
    });
  }

  abrirSeguidos() {
    if (!this.profile) return;
    this.postService.getFollowing(this.profile.id).subscribe({
      next: (data) => {
        this.followingList = data;
        this.isFollowingModalOpen = true;
      },
      error: (err) => {
        console.error('Error al obtener seguidos:', err);
      }
    });
  }

  toggleFollowUserInList(user: any) {
    const isFollowing = user.followedByMe;
    const action = isFollowing
      ? this.postService.unfollowUser(user.id)
      : this.postService.followUser(user.id);

    action.subscribe({
      next: () => {
        user.followedByMe = !isFollowing;
        this.cargarDatosPerfil();
      },
      error: (err) => {
        console.error('Error al alternar seguimiento en modal:', err);
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

  async confirmarQuitarRepost(postId: string) {
    const alert = await this.alertController.create({
      header: 'Quitar Repost',
      message: '¿Quieres quitar esta publicación de tus reposteos?',
      buttons: [
        {
          text: 'Cancelar',
          role: 'cancel'
        },
        {
          text: 'Quitar',
          role: 'destructive',
          handler: () => {
            this.quitarRepost(postId);
          }
        }
      ]
    });
    await alert.present();
  }

  quitarRepost(postId: string) {
    this.postService.toggleRepost(postId).subscribe({
      next: () => {
        console.log('Repost quitado:', postId);
        this.selectedPost = null;
        this.cargarDatosPerfil();
      },
      error: (err) => {
        console.error('Error al quitar repost:', err);
      }
    });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
