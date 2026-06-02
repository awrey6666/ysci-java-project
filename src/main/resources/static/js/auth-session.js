/** Client-side auth session helpers */

export const ACCESS_TOKEN_KEY = 'fetch.access-token';
export const THEME_KEY = 'fetch.theme';

export const clearLocalAuth = () => {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
};

export const setDocumentBranding = () => {
  document.title = 'FETCH';
  let link = document.querySelector('link[rel="icon"]');
  if (!link) {
    link = document.createElement('link');
    link.rel = 'icon';
    document.head.appendChild(link);
  }
  link.type = 'image/svg+xml';
  link.href = '/images/favicon.svg';
};
