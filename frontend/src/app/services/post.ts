import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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
    const formData = new FormData();
    formData.append('image', imagen, imagen.name);
    formData.append(
      'data',  // ← era 'post', el controlador espera 'data'
      new Blob([JSON.stringify(postData)], { type: 'application/json' })
    );
    return this.http.post(BACKEND_URL, formData);
  }

  obtenerPosts(): Observable<any[]> {
    return this.http.get<any[]>(`${BACKEND_URL}/feed`); // ← era /api/posts, debe ser /api/posts/feed
  }

  getFeed(): Observable<any[]> {
    return this.http.get<any[]>(`${BACKEND_URL}/feed`); // ← mismo fix
  }
}