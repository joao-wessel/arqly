import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { ApiResponse } from '../../core/auth/auth.models';
import { FileFolder, FileItem, FileOwnerType, FileStatus, FileVersion, Page } from './file.models';

@Injectable({ providedIn: 'root' })
export class FileApiService {
  private readonly http = inject(HttpClient);
  readonly baseUrl = 'http://localhost:8080/api/tenant/files';

  list(options: {
    ownerType?: FileOwnerType; ownerId?: string; folderId?: string | null; rootOnly?: boolean;
    search?: string; extension?: string; author?: string; tag?: string; status?: FileStatus;
    page?: number; size?: number;
  }) {
    let params = new HttpParams()
      .set('page', String(options.page || 0))
      .set('size', String(options.size || 20))
      .set('sort', 'updatedAt,desc');
    Object.entries(options).forEach(([key, value]) => {
      if (!['page', 'size'].includes(key) && value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http.get<ApiResponse<Page<FileItem>>>(this.baseUrl, { params });
  }

  folders(ownerType: FileOwnerType, ownerId: string) {
    return this.http.get<ApiResponse<FileFolder[]>>(`${this.baseUrl}/folders`, {
      params: { ownerType, ownerId }
    });
  }

  createFolder(ownerType: FileOwnerType, ownerId: string, name: string, parentId?: string | null) {
    return this.http.post<ApiResponse<FileFolder>>(`${this.baseUrl}/folders`, { ownerType, ownerId, parentId, name });
  }

  deleteFolder(id: string) {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/folders/${id}`);
  }

  versions(id: string) {
    return this.http.get<ApiResponse<FileVersion[]>>(`${this.baseUrl}/${id}/versions`);
  }

  update(id: string, body: { name?: string; folderId?: string | null; visibility?: string; tags?: string[] }) {
    return this.http.put<ApiResponse<FileItem>>(`${this.baseUrl}/${id}`, body);
  }

  archive(id: string) {
    return this.http.patch<ApiResponse<FileItem>>(`${this.baseUrl}/${id}/archive`, {});
  }

  restore(id: string) {
    return this.http.patch<ApiResponse<FileItem>>(`${this.baseUrl}/${id}/restore`, {});
  }

  delete(id: string) {
    return this.http.delete<ApiResponse<FileItem>>(`${this.baseUrl}/${id}`);
  }

  downloadUrl(id: string, versionId?: string) {
    return `${this.baseUrl}/${id}/download${versionId ? `?versionId=${versionId}` : ''}`;
  }

  download(id: string, versionId?: string) {
    return this.http.get(this.downloadUrl(id, versionId), { responseType: 'blob' });
  }

  preview(id: string) {
    return this.http.get(`${this.baseUrl}/${id}/preview`, { responseType: 'blob' });
  }
}
