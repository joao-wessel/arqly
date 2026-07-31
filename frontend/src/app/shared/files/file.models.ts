export type FileOwnerType = 'BRIEFING' | 'PROPOSAL' | 'PROJECT' | 'PROJECT_STAGE' | 'DOCUMENT' | 'TENANT';
export type FileStatus = 'ACTIVE' | 'ARCHIVED' | 'DELETED';
export type FileVisibility = 'INTERNAL' | 'CLIENT_VISIBLE';

export interface FileFolder {
  id: string;
  parentId?: string | null;
  name: string;
  ownerType: FileOwnerType;
  ownerId: string;
  createdAt: string;
}

export interface FileItem {
  id: string;
  ownerType: FileOwnerType;
  ownerId: string;
  ownerLabel: string;
  folderId?: string | null;
  folderName?: string | null;
  name: string;
  originalName: string;
  extension: string;
  mimeType: string;
  size: number;
  checksum: string;
  version: number;
  visibility: FileVisibility;
  status: FileStatus;
  uploadedById: string;
  uploadedByName: string;
  tags: string[];
  previewAvailable: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface FileVersion {
  id: string;
  versionNumber: number;
  checksum: string;
  size: number;
  authorId: string;
  authorName: string;
  revisionComment?: string | null;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  number: number;
  totalPages: number;
  totalElements: number;
}
