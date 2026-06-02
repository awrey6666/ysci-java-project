import { formatDateTime, escapeHtml } from './format.js';

const commentsCache = new Map();

let apiGetRef = null;
let apiPostRef = null;
let getCurrentUserRef = null;

export const initPostComments = ({ apiGet, apiPost, getCurrentUser }) => {
  apiGetRef = apiGet;
  apiPostRef = apiPost;
  getCurrentUserRef = getCurrentUser;
};

const getCache = (postId) => {
  if (!commentsCache.has(postId)) {
    commentsCache.set(postId, { loaded: false, loading: false, items: [] });
  }
  return commentsCache.get(postId);
};

export const renderPostCommentsBlock = (postId) => `
  <div class="post-comments mt-4 border-t border-sys-border pt-3" data-post-comments="${postId}">
    <button type="button" class="post-comments-toggle text-[10px] font-mono uppercase tracking-wider text-sys-text-secondary hover:text-sys-accent mb-2 flex items-center gap-1"
      data-toggle-comments="${postId}" aria-expanded="false">
      <span data-comments-toggle-label>Show comments</span>
    </button>
    <ul class="post-comments-list hidden space-y-2.5 mb-3 max-h-64 overflow-y-auto overflow-x-hidden pr-1"
      data-comments-list="${postId}" role="list"></ul>
    <p class="post-comments-empty hidden text-[10px] text-sys-text-muted font-mono mb-2" data-comments-empty="${postId}">No comments yet.</p>
    <p class="post-comments-loading hidden text-[10px] text-sys-text-muted font-mono mb-2" data-comments-loading="${postId}">Loading comments…</p>
    <p class="post-comments-error hidden text-[10px] text-red-400 font-mono mb-2" data-comments-error="${postId}"></p>
    <form class="post-comments-form flex gap-2 items-stretch" data-comments-form="${postId}">
      <input type="text" name="comment" autocomplete="off"
        class="post-comment-input flex-1 min-w-0 h-9 px-3 rounded-lg border border-sys-border bg-sys-input text-sys-text-primary text-xs placeholder:text-sys-text-muted focus:outline-none focus:border-sys-accent/50"
        placeholder="Write a comment…" maxlength="2000" data-comment-input="${postId}">
      <button type="submit" class="post-comment-submit h-9 px-3 bg-sys-accent text-white text-[10px] font-mono font-bold uppercase tracking-widest rounded-lg shrink-0 hover:brightness-105 disabled:opacity-50 disabled:cursor-not-allowed">
        Post
      </button>
    </form>
  </div>
`;

const renderCommentItem = (comment) => {
  const username = comment.authorUsername || 'User';
  const profileBtn = username && username !== 'User'
    ? `<button type="button" class="profile-link text-[10px] font-bold text-sys-accent uppercase tracking-wide" data-open-profile="${escapeHtml(username)}">${escapeHtml(username)}</button>`
    : `<span class="text-[10px] font-bold text-sys-text-primary uppercase tracking-wide">${escapeHtml(username)}</span>`;
  return `
    <li class="post-comment-item border-l-2 border-sys-border pl-2.5" data-comment-id="${comment.id || ''}">
      <div class="flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
        ${profileBtn}
        <time class="text-[9px] font-mono text-sys-text-muted">${formatDateTime(comment.createdAt)}</time>
      </div>
      <p class="text-[11px] text-sys-text-secondary leading-relaxed mt-0.5 break-words">${escapeHtml(comment.body || '')}</p>
    </li>
  `;
};

const getPostCommentEls = (postId, root) => {
  const scope = root || document;
  const block = scope.querySelector(`[data-post-comments="${postId}"]`);
  if (!block) return null;
  return {
    block,
    toggle: block.querySelector(`[data-toggle-comments="${postId}"]`),
    toggleLabel: block.querySelector('[data-comments-toggle-label]'),
    list: block.querySelector(`[data-comments-list="${postId}"]`),
    empty: block.querySelector(`[data-comments-empty="${postId}"]`),
    loading: block.querySelector(`[data-comments-loading="${postId}"]`),
    error: block.querySelector(`[data-comments-error="${postId}"]`),
    form: block.querySelector(`[data-comments-form="${postId}"]`),
    input: block.querySelector(`[data-comment-input="${postId}"]`),
    submit: block.querySelector('.post-comment-submit')
  };
};

const updateToggleLabel = (els, count, expanded) => {
  if (!els?.toggleLabel) return;
  const n = count ?? 0;
  if (expanded) {
    els.toggleLabel.textContent = n === 1 ? '1 comment · Hide' : `${n} comments · Hide`;
  } else {
    els.toggleLabel.textContent = n === 0 ? 'Show comments' : (n === 1 ? '1 comment · Show' : `${n} comments · Show`);
  }
};

const paintComments = (postId, root) => {
  const els = getPostCommentEls(postId, root);
  if (!els) return;
  const { items } = getCache(postId);
  const expanded = els.toggle?.getAttribute('aria-expanded') === 'true';

  if (els.loading) els.loading.classList.add('hidden');
  if (els.error) {
    els.error.classList.add('hidden');
    els.error.textContent = '';
  }

  if (!items.length) {
    if (els.list) {
      els.list.innerHTML = '';
      els.list.classList.add('hidden');
    }
    if (expanded && els.empty) els.empty.classList.remove('hidden');
    else if (els.empty) els.empty.classList.add('hidden');
  } else {
    if (els.empty) els.empty.classList.add('hidden');
    if (els.list) {
      els.list.innerHTML = items.map(renderCommentItem).join('');
      els.list.classList.toggle('hidden', !expanded);
    }
  }
  updateToggleLabel(els, items.length, expanded);
};

export const loadPostComments = async (postId, root) => {
  const els = getPostCommentEls(postId, root);
  const cache = getCache(postId);
  if (cache.loaded || cache.loading) {
    paintComments(postId, root);
    return cache.items;
  }

  cache.loading = true;
  if (els?.loading) els.loading.classList.remove('hidden');
  if (els?.error) els.error.classList.add('hidden');

  try {
    const items = await apiGetRef(`/api/posts/${postId}/comments`);
    cache.items = Array.isArray(items) ? items : [];
    cache.loaded = true;
    paintComments(postId, root);
    return cache.items;
  } catch (err) {
    console.error('load comments failed', err);
    if (els?.error) {
      els.error.textContent = 'Could not load comments.';
      els.error.classList.remove('hidden');
    }
    return [];
  } finally {
    cache.loading = false;
    if (els?.loading) els.loading.classList.add('hidden');
  }
};

const setCommentsExpanded = async (postId, expanded, root) => {
  const els = getPostCommentEls(postId, root);
  if (!els?.toggle) return;

  els.toggle.setAttribute('aria-expanded', String(expanded));
  const cache = getCache(postId);

  if (expanded && !cache.loaded) {
    await loadPostComments(postId, root);
  } else {
    paintComments(postId, root);
  }

  if (expanded && els.list && cache.items.length) {
    els.list.classList.remove('hidden');
    els.list.scrollTop = els.list.scrollHeight;
  }
};

const appendCommentToCache = (postId, comment) => {
  const cache = getCache(postId);
  const exists = comment.id && cache.items.some((c) => c.id === comment.id);
  if (exists) return;
  cache.items = [...cache.items.filter((c) => !String(c.id).startsWith('tmp-') || c.body !== comment.body), comment]
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  cache.loaded = true;
};

export const bindPostComments = (container, { onProfileClick } = {}) => {
  if (!container) return;

  if (onProfileClick && !container.dataset.commentsProfileBound) {
    container.dataset.commentsProfileBound = '1';
    container.addEventListener('click', (e) => {
      const link = e.target.closest('.post-comment-item [data-open-profile]');
      if (!link || !container.contains(link)) return;
      e.stopPropagation();
      const username = link.getAttribute('data-open-profile');
      if (username) onProfileClick(username);
    });
  }

  container.querySelectorAll('[data-toggle-comments]').forEach((btn) => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      const postId = Number(btn.dataset.toggleComments);
      const expanded = btn.getAttribute('aria-expanded') !== 'true';
      await setCommentsExpanded(postId, expanded, container);
    });
  });

  container.querySelectorAll('[data-comments-form]').forEach((form) => {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      e.stopPropagation();
      const postId = Number(form.dataset.commentsForm);
      const els = getPostCommentEls(postId, container);
      const text = els?.input?.value?.trim();
      if (!text || !els) return;

      const user = getCurrentUserRef?.() || {};
      const optimistic = {
        id: `tmp-${Date.now()}`,
        postId,
        authorId: user.id,
        authorUsername: user.username || 'You',
        body: text,
        createdAt: new Date().toISOString()
      };

      els.input.value = '';
      if (els.submit) els.submit.disabled = true;

      appendCommentToCache(postId, optimistic);
      await setCommentsExpanded(postId, true, container);
      paintComments(postId, container);
      if (els.list) els.list.scrollTop = els.list.scrollHeight;

      try {
        const saved = await apiPostRef(`/api/posts/${postId}/comments`, { body: text });
        const cache = getCache(postId);
        cache.items = cache.items
          .filter((c) => c.id !== optimistic.id)
          .concat(saved)
          .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
        paintComments(postId, container);
        if (els.list) els.list.scrollTop = els.list.scrollHeight;
      } catch (err) {
        console.error('post comment failed', err);
        const cache = getCache(postId);
        cache.items = cache.items.filter((c) => c.id !== optimistic.id);
        paintComments(postId, container);
        els.input.value = text;
        if (els.error) {
          els.error.textContent = 'Could not post comment. Try again.';
          els.error.classList.remove('hidden');
        }
      } finally {
        if (els.submit) els.submit.disabled = false;
        els.input?.focus();
      }
    });
  });
};

export const clearCommentsCache = () => commentsCache.clear();
