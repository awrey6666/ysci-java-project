import { escapeHtml } from './format.js';

let modalElements = null;

const getModal = () => {
  if (modalElements) return modalElements;
  modalElements = {
    root: document.getElementById('post-image-modal'),
    img: document.getElementById('post-image-modal-img'),
    caption: document.getElementById('post-image-modal-caption'),
    closeBtn: document.getElementById('post-image-modal-close')
  };
  return modalElements;
};

export const openPostImageModal = (imageUrl, postBody = '') => {
  const { root, img, caption } = getModal();
  if (!root || !img) return;
  img.src = imageUrl;
  img.alt = 'Post image';
  if (caption) caption.textContent = postBody || '';
  root.classList.remove('hidden');
  root.setAttribute('aria-hidden', 'false');
  document.body.classList.add('overflow-hidden');
};

export const closePostImageModal = () => {
  const { root, img } = getModal();
  if (!root) return;
  root.classList.add('hidden');
  root.setAttribute('aria-hidden', 'true');
  if (img) img.src = '';
  document.body.classList.remove('overflow-hidden');
};

export const initPostImageModal = () => {
  const { root, closeBtn } = getModal();
  if (!root) return;

  closeBtn?.addEventListener('click', closePostImageModal);
  root.addEventListener('click', (e) => {
    if (e.target === root || e.target.classList.contains('post-image-modal-backdrop')) {
      closePostImageModal();
    }
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !root.classList.contains('hidden')) {
      closePostImageModal();
    }
  });
};

export const bindPostImageClicks = (container) => {
  if (!container) return;
  container.querySelectorAll('[data-post-image]').forEach((el) => {
    el.addEventListener('click', (e) => {
      e.stopPropagation();
      const url = el.getAttribute('data-post-image');
      const caption = el.getAttribute('data-post-caption') || '';
      if (url) openPostImageModal(url, caption);
    });
  });
};

export const renderPostImagesHtml = (imageUrls, postBody = '') => {
  if (!imageUrls?.length) return '';
  const caption = escapeHtml(postBody);
  const items = imageUrls.map((url) => `
    <button type="button" class="post-image-thumb block w-full rounded-xl overflow-hidden border border-sys-border focus:outline-none focus:border-sys-accent transition-theme"
      data-post-image="${escapeHtml(url)}" data-post-caption="${caption}">
      <img src="${escapeHtml(url)}" alt="Post attachment" class="w-full max-h-72 object-cover pointer-events-none" loading="lazy">
    </button>
  `).join('');
  return `<div class="mt-3 grid gap-2 ${imageUrls.length > 1 ? 'sm:grid-cols-2' : ''}">${items}</div>`;
};
