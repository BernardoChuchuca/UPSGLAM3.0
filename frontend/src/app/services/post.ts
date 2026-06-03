import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

const BACKEND_URL = `${environment.apiUrl}/posts`;
const USERS_URL = `${environment.apiUrl}/users`;

export interface CreatePostRequest {
  caption: string;
  filterName: string;
  userId: number;
}

@Injectable({
  providedIn: 'root'
})
export class PostService {

  constructor(private http: HttpClient) {}

  crearPost(imagen: File, postData: CreatePostRequest): Observable<any> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    const formData = new FormData();
    formData.append('image', imagen, imagen.name);
    formData.append(
      'data',
      new Blob([JSON.stringify(postData)], { type: 'application/json' })
    );
    return this.http.post(BACKEND_URL, formData, { headers });
  }

  obtenerPrevisualizacion(imagen: File, filterName: string): Observable<any> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }

    const formData = new FormData();
    formData.append('image', imagen, imagen.name);
    return this.http.post(`${BACKEND_URL}/preview?filterName=${filterName}`, formData, { headers });
  }

  obtenerPosts(): Observable<any[]> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.get<any[]>(`${BACKEND_URL}/feed`, { headers });
  }

  getFeed(): Observable<any[]> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.get<any[]>(`${BACKEND_URL}/feed`, { headers });
  }

  getUserPosts(profileId: string): Observable<any[]> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.get<any[]>(`${BACKEND_URL}/user/${profileId}`, { headers });
  }

  toggleLike(postId: string): Observable<void> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.post<void>(`${BACKEND_URL}/${postId}/likes`, {}, { headers });
  }

  getComments(postId: string): Observable<any[]> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.get<any[]>(`${BACKEND_URL}/${postId}/comments`, { headers });
  }

  addComment(postId: string, content: string): Observable<any> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.post<any>(`${BACKEND_URL}/${postId}/comments`, { content }, { headers });
  }

  deleteComment(postId: string, commentId: string): Observable<void> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.delete<void>(`${BACKEND_URL}/${postId}/comments/${commentId}`, { headers });
  }

  getProcessingHistory(): Observable<any[]> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.get<any[]>(`${environment.apiUrl}/processing/history`, { headers });
  }

  deletePost(postId: string): Observable<void> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.delete<void>(`${BACKEND_URL}/${postId}`, { headers });
  }

  getMyProfile(): Observable<any> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.get<any>(`${USERS_URL}/me`, { headers });
  }


  // ─── MÉTODOS DE REPOSTS ────────────────────────────────────────

  toggleRepost(postId: string): Observable<void> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.post<void>(`${BACKEND_URL}/${postId}/repost`, {}, { headers });
  }

  getUserReposts(profileId: string): Observable<any[]> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.get<any[]>(`${BACKEND_URL}/reposts/user/${profileId}`, { headers });
  }

  // ─── MÉTODOS DE SEGUIMIENTO ────────────────────────────────────

  followUser(profileId: string): Observable<void> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.post<void>(`${USERS_URL}/${profileId}/follow`, {}, { headers });
  }

  unfollowUser(profileId: string): Observable<void> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.delete<void>(`${USERS_URL}/${profileId}/follow`, { headers });
  }

  getFollowers(profileId: string): Observable<any[]> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.get<any[]>(`${USERS_URL}/${profileId}/followers`, { headers });
  }

  getFollowing(profileId: string): Observable<any[]> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.get<any[]>(`${USERS_URL}/${profileId}/following`, { headers });
  }
}