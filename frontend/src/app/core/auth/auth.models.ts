export type AuthScope = 'platform' | 'tenant';

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  userId: string;
  tenantId: string | null;
  name: string;
  email: string;
  roles: string[];
  themeMode: 'light' | 'dark';
  colorPalette: 'arqly' | 'terracotta' | 'indigo';
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string | null;
  timestamp: string;
}
