import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

const BACKEND_URL = 'http://localhost:8080/api/posts';

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
      'data',  // ← era 'post', el controlador espera 'data'
      new Blob([JSON.stringify(postData)], { type: 'application/json' })
    );
    return this.http.post(BACKEND_URL, formData, { headers });
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
    return this.http.get<any[]>('http://localhost:8080/api/processing/history', { headers });
  }

  deletePost(postId: string): Observable<void> {
    const token = localStorage.getItem('token');
    let headers = new HttpHeaders();
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return this.http.delete<void>(`${BACKEND_URL}/${postId}`, { headers });
  }
}