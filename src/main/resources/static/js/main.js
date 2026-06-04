import { DEFAULT_PROFILE, DEFAULT_POSTS } from './data.js';
import { ACCESS_TOKEN_KEY, THEME_KEY, clearLocalAuth, setDocumentBranding } from './auth-session.js';
import { formatDateTime, formatChatTime, escapeHtml } from './format.js';
import { initPostImageModal, bindPostImageClicks, renderPostImagesHtml } from './post-media.js';
import {
  initMobileChatLayout,
  openMobileChatPanel,
  resetAllMobileChatPanels
} from './mobile-chat.js';
import {
  initPostComments,
  renderPostCommentsBlock,
  bindPostComments,
  clearCommentsCache
} from './post-comments.js';

const ANONYMOUS_AVATAR = '/images/anonymous.svg';

const state = {
  accessToken: localStorage.getItem(ACCESS_TOKEN_KEY),
  activeRoomId: null,
  activeRoomType: null,
  rooms: [],
  roomSubscription: null,
  activeAiConversationId: null,
  aiConversations: [],
  currentUser: null,
  pendingPostFiles: [],
  friends: [],
  discover: { incoming: [], outgoing: [], users: [] },
  previousTab: 'community',
  activeDmFriendId: null,
  viewingProfile: null,
  messagesByRoom: {},
  roomSubscriptions: {},
  friendRoomByUserId: {}
};

let stompClient = null;
let wsConnected = false;
let aiSending = false;
let activeRoomPollTimer = null;

const disconnectWebSocket = () => {
  Object.values(state.roomSubscriptions || {}).forEach((sub) => {
    try {
      sub.unsubscribe();
    } catch {
      /* ignore */
    }
  });
  state.roomSubscriptions = {};
  if (stompClient) {
    try {
      stompClient.deactivate();
    } catch {
      /* ignore */
    }
  }
  stompClient = null;
  wsConnected = false;
  stopActiveRoomPoll();
};

const stopActiveRoomPoll = () => {
  if (activeRoomPollTimer) {
    clearInterval(activeRoomPollTimer);
    activeRoomPollTimer = null;
  }
};

const startActiveRoomPoll = () => {
  stopActiveRoomPoll();
  activeRoomPollTimer = setInterval(() => {
    if (!state.activeRoomId || wsConnected) return;
    loadRoomMessages(state.activeRoomId);
  }, 5000);
};

const logoutUser = async () => {
  disconnectWebSocket();
  try {
    const headers = await getAuthHeaders();
    await fetch('/api/auth/logout', {
      method: 'POST',
      credentials: 'include',
      headers: { ...headers, Accept: 'application/json' }
    });
  } catch {
    /* clear client state even if server rejects */
  }
  clearLocalAuth();
  clearCommentsCache();
  state.accessToken = null;
  state.currentUser = null;
  state.messagesByRoom = {};
  state.rooms = [];
  window.location.href = '/login';
};

const refreshAccessToken = async () => {
  try {
    const res = await fetch('/api/auth/refresh', {
      method: 'POST',
      credentials: 'include'
    });
    if (!res.ok) throw new Error('refresh failed');
    const data = await res.json();
    state.accessToken = data.accessToken;
    localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
    return data.accessToken;
  } catch (err) {
    state.accessToken = null;
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    return null;
  }
};

const getAuthHeaders = async () => {
  if (!state.accessToken) {
    await refreshAccessToken();
  }
  return state.accessToken ? { Authorization: `Bearer ${state.accessToken}` } : {};
};

const apiFetch = async (path, options = {}) => {
  const authHeaders = await getAuthHeaders();
  const init = {
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      ...(options.headers || {}),
      ...authHeaders
    },
    ...options
  };

  if (options.body != null && typeof options.body !== 'string') {
    init.body = JSON.stringify(options.body);
    init.headers['Content-Type'] = 'application/json';
  }

  let response = await fetch(path, init);
  if (response.status === 401 && !options._retried) {
    await refreshAccessToken();
    return apiFetch(path, { ...options, _retried: true });
  }

  if (!response.ok
      && (response.status === 401 || response.status === 403)
      && (path.startsWith('/api/rooms') || path.startsWith('/api/ai'))
      && !window.location.pathname.startsWith('/login')
      && !window.location.pathname.startsWith('/register')) {
    window.location.href = '/login';
    throw new Error(`Unauthorized for ${path}`);
  }

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`${path} ${response.status}: ${text}`);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
};

const apiGet = (path) => apiFetch(path, { method: 'GET' });
const apiPost = (path, body) => apiFetch(path, { method: 'POST', body });
const apiPostEmpty = (path) => apiFetch(path, { method: 'POST' });
const apiPut = (path, body) => apiFetch(path, { method: 'PUT', body });

const uploadMedia = async (file, folder = 'media') => {
  const url = `/api/media/upload?folder=${encodeURIComponent(folder)}`;
  const doUpload = async () => {
    const formData = new FormData();
    formData.append('file', file);
    const authHeaders = await getAuthHeaders();
    return fetch(url, {
      method: 'POST',
      credentials: 'include',
      headers: { ...authHeaders },
      body: formData
    });
  };
  let response = await doUpload();
  if (response.status === 401) {
    await refreshAccessToken();
    response = await doUpload();
  }
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`upload failed ${response.status}: ${text}`);
  }
  return response.json();
};

const setActiveSection = (tab) => {
  resetAllMobileChatPanels();
  const sections = document.querySelectorAll('section[class^="view-"]');
  sections.forEach((section) => {
    section.classList.toggle('hidden', !section.classList.contains(`view-${tab}`));
  });
};

const initTabNavigation = () => {
  const buttons = document.querySelectorAll('.main-nav-label');
  buttons.forEach((button) => {
    const tab = button.dataset.tab;
    button.addEventListener('click', () => {
      setActiveSection(tab);
      buttons.forEach((btn) => btn.classList.remove('bg-sys-accent/10', 'text-sys-text-primary'));
      button.classList.add('bg-sys-accent/10', 'text-sys-text-primary');
      if (tab === 'direct' || tab === 'group') {
        loadRooms().then(() => {
          if (tab === 'direct') loadFriends().then(() => renderDmFriendsList());
        });
      } else if (tab === 'ai') {
        loadAiSessions();
      } else if (tab === 'discover') {
        loadDiscover();
      } else if (tab === 'profile') {
        loadFriends();
        loadProfilePosts();
      } else if (tab === 'direct') {
        loadFriends().then(() => renderDmFriendsList());
      }
    });
  });
  setActiveSection('community');
  const communityButton = document.querySelector('.main-nav-label[data-tab="community"]');
  if (communityButton) {
    communityButton.classList.add('bg-sys-accent/10', 'text-sys-text-primary');
  }
};

const renderProfile = (profile) => {
  state.currentUser = profile;
  const avatarImg = document.querySelector('.self-profile-avatar');
  const usernameInput = document.getElementById('profile-username-input');
  const nameEl = document.querySelector('.self-profile-name');
  const handleEl = document.querySelector('.self-profile-handle');
  const profileNameInput = document.getElementById('profile-name-input');
  const profileBioTextarea = document.getElementById('profile-bio-textarea');
  const profileAvatarInput = document.getElementById('profile-avatar-input');
  const profileSyncBtn = document.getElementById('profile-sync-btn');

  const avatarUrl = profile.avatarUrl || DEFAULT_PROFILE.avatar;
  if (avatarImg) avatarImg.src = avatarUrl;
  const profilePreview = document.getElementById('profile-avatar-preview');
  if (profilePreview) profilePreview.src = avatarUrl;
  if (usernameInput) usernameInput.value = profile.username || '';
  if (nameEl) nameEl.textContent = profile.displayName || profile.username || DEFAULT_PROFILE.name;
  if (handleEl) handleEl.textContent = `@${profile.username || 'anon'}`;
  if (profileNameInput) profileNameInput.value = profile.displayName || profile.username || DEFAULT_PROFILE.name;
  if (profileBioTextarea) profileBioTextarea.value = profile.bio || DEFAULT_PROFILE.bio;
  if (profileAvatarInput) profileAvatarInput.value = profile.avatarUrl || DEFAULT_PROFILE.avatar;
  if (profileSyncBtn) profileSyncBtn.textContent = 'Save Profile';
};

const renderPostCard = (post) => {
  const isAnonymous = !!post.anonymous;
  const authorUsername = post.authorUsername || post.author?.username || null;
  const authorName = isAnonymous
    ? 'Anonymous'
    : (post.authorName || authorUsername || post.author?.displayName || 'Unknown');
  const authorHandle = isAnonymous
    ? '@anonymous'
    : (post.authorHandle || (authorUsername ? `@${authorUsername}` : '@anonymous'));
  const authorAvatar = post.authorAvatar || post.authorAvatarUrl || post.author?.avatarUrl || DEFAULT_PROFILE.avatar;
  const profileAttr = !isAnonymous && authorUsername ? `data-open-profile="${authorUsername}"` : '';
  const profileClass = profileAttr ? 'profile-link' : '';
  const avatarBlock = isAnonymous
    ? `<div class="w-11 h-11 rounded-full overflow-hidden border border-sys-border anonymous-avatar shrink-0">
        <img src="${ANONYMOUS_AVATAR}" alt="Anonymous" class="w-full h-full object-contain p-1.5">
      </div>`
    : `<div class="w-11 h-11 rounded-full overflow-hidden border border-sys-border bg-sys-secondary shrink-0 ${profileClass}" ${profileAttr}>
        <img src="${authorAvatar}" alt="${authorName}" class="w-full h-full object-cover">
      </div>`;
  const visibility = (post.visibility || post.privacy || 'public').toString().toLowerCase();
  const imageUrls = post.imageUrls || post.images || [];
  const postId = post.id;
  const likeCount = post.likeCount ?? post.reactionCount ?? post.likes ?? 0;
  const liked = !!(post.likedByCurrentUser ?? post.likedByUser);
  const likeBtnClass = liked ? 'post-like-btn composer-action-btn is-liked' : 'post-like-btn composer-action-btn';
  return `
    <article class="border border-sys-border bg-sys-bg-primary p-5 rounded-3xl shadow-sm transition-theme" data-post-id="${postId || ''}">
      <div class="flex items-center gap-3 mb-4">
        ${avatarBlock}
        <div class="min-w-0 ${profileClass}" ${profileAttr}>
          <p class="text-[11px] font-bold uppercase tracking-[0.18em] text-sys-text-primary">${authorName}</p>
          <p class="text-[9px] text-sys-text-secondary font-mono">${authorHandle} · ${formatDateTime(post.createdAt)}</p>
        </div>
      </div>
      <p class="text-[11px] leading-relaxed text-sys-text-secondary">${escapeHtml(post.body || post.text || '')}</p>
      ${renderPostImagesHtml(imageUrls, post.body || post.text || '')}
      <div class="mt-4 flex items-center justify-between gap-3 flex-wrap">
        <span class="text-[10px] font-mono uppercase tracking-[0.2em] text-sys-text-secondary">${visibility.includes('friend') ? '🔐 Friends only' : '🌐 Public'}</span>
        ${postId ? `<button type="button" class="${likeBtnClass}" data-like-post="${postId}" aria-pressed="${liked}">
          <span data-like-icon aria-hidden="true">${liked ? '♥' : '♡'}</span> Like · <span data-like-count>${likeCount}</span>
        </button>` : `<span class="text-[10px] font-mono text-sys-text-secondary">${likeCount} likes</span>`}
      </div>
      ${postId ? renderPostCommentsBlock(postId) : ''}
    </article>
  `;
};

const bindPostInteractions = (container) => {
  if (!container) return;
  container.querySelectorAll('[data-open-profile]').forEach((el) => {
    el.addEventListener('click', (e) => {
      e.stopPropagation();
      const username = el.getAttribute('data-open-profile') || el.closest('[data-open-profile]')?.getAttribute('data-open-profile');
      if (username) openUserProfile(username);
    });
  });
  container.querySelectorAll('[data-like-post]').forEach((btn) => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      if (btn.disabled) return;
      const postId = Number(btn.dataset.likePost);
      const countEl = btn.querySelector('[data-like-count]');
      const iconEl = btn.querySelector('[data-like-icon]');
      const wasLiked = btn.classList.contains('is-liked');
      const prevCount = Number(countEl?.textContent) || 0;
      btn.disabled = true;
      btn.classList.toggle('is-liked', !wasLiked);
      btn.setAttribute('aria-pressed', String(!wasLiked));
      if (iconEl) iconEl.textContent = wasLiked ? '♡' : '♥';
      if (countEl) countEl.textContent = String(wasLiked ? Math.max(0, prevCount - 1) : prevCount + 1);
      try {
        const updated = await apiPostEmpty(`/api/posts/${postId}/reactions`);
        if (updated) {
          btn.classList.toggle('is-liked', updated.likedByCurrentUser);
          btn.setAttribute('aria-pressed', String(updated.likedByCurrentUser));
          if (iconEl) iconEl.textContent = updated.likedByCurrentUser ? '♥' : '♡';
          if (countEl) countEl.textContent = String(updated.likeCount ?? 0);
        }
      } catch (err) {
        console.error('like failed', err);
        btn.classList.toggle('is-liked', wasLiked);
        btn.setAttribute('aria-pressed', String(wasLiked));
        if (iconEl) iconEl.textContent = wasLiked ? '♥' : '♡';
        if (countEl) countEl.textContent = String(prevCount);
      } finally {
        btn.disabled = false;
      }
    });
  });
  bindPostImageClicks(container);
  bindPostComments(container, { onProfileClick: openUserProfile });
};

const renderPostsInto = (container, posts, fallback) => {
  if (!container) return;
  const data = posts && posts.length ? posts : (fallback || []);
  container.innerHTML = data.map((post) => renderPostCard(post)).join('');
  bindPostInteractions(container);
};

const renderCommunity = (posts) => {
  renderPostsInto(document.getElementById('feed-posts-container'), posts, DEFAULT_POSTS);
};

const renderProfilePosts = (posts) => {
  const container = document.getElementById('profile-posts-container');
  if (!container) return;
  if (!posts || !posts.length) {
    container.innerHTML = '<p class="text-[10px] text-sys-text-muted font-mono">No profile posts yet.</p>';
    return;
  }
  renderPostsInto(container, posts);
};

const bindToggleButton = (btnId, checkboxId) => {
  const btn = document.getElementById(btnId);
  const checkbox = document.getElementById(checkboxId);
  if (!btn || !checkbox) return;
  const sync = () => {
    btn.classList.toggle('is-active', checkbox.checked);
    btn.setAttribute('aria-pressed', checkbox.checked ? 'true' : 'false');
  };
  btn.addEventListener('click', () => {
    checkbox.checked = !checkbox.checked;
    sync();
  });
  sync();
};

const resetAnonymousToggle = (btnId, checkboxId) => {
  const btn = document.getElementById(btnId);
  const checkbox = document.getElementById(checkboxId);
  if (checkbox) checkbox.checked = false;
  if (btn) {
    btn.classList.remove('is-active');
    btn.setAttribute('aria-pressed', 'false');
  }
};

const renderFriendRow = (friend, extraClass = '') => {
  const name = friend.displayName || friend.username;
  const avatar = friend.avatarUrl || DEFAULT_PROFILE.avatar;
  return `
    <div class="flex items-center gap-2.5 p-2.5 bg-sys-bg-primary/50 border border-sys-border rounded-lg profile-link ${extraClass}" data-open-profile="${friend.username}">
      <div class="w-8 h-8 rounded-full overflow-hidden border border-sys-border shrink-0 bg-sys-secondary">
        <img src="${avatar}" alt="${name}" class="w-full h-full object-cover">
      </div>
      <div class="min-w-0 flex-1">
        <span class="text-[10px] font-bold text-sys-text-primary block truncate">${name}</span>
        <span class="text-[8px] font-mono text-sys-text-secondary">@${friend.username}</span>
      </div>
    </div>
  `;
};

const renderFriendsPreview = (friends) => {
  const preview = document.getElementById('profile-friends-preview');
  const countEl = document.getElementById('profile-friends-count');
  if (countEl) {
    countEl.textContent = `${friends.length} connection${friends.length === 1 ? '' : 's'}`;
  }
  if (!preview) return;
  if (!friends.length) {
    preview.innerHTML = '<p class="text-[10px] text-sys-text-muted">No friends yet. Use Discover to connect.</p>';
    return;
  }
  preview.innerHTML = friends.slice(0, 5).map(renderFriendRow).join('');
  bindProfileLinks(preview);
};

const renderFriendsModal = (friends) => {
  const list = document.getElementById('friends-modal-list');
  if (!list) return;
  if (!friends.length) {
    list.innerHTML = '<p class="text-[10px] text-sys-text-muted">No friends yet.</p>';
    return;
  }
  list.innerHTML = friends.map((f) => renderFriendRow(f)).join('');
  bindProfileLinks(list);
};

const bindProfileLinks = (root = document) => {
  const scope = root instanceof Element ? root : document;
  scope.querySelectorAll('[data-open-profile]').forEach((el) => {
    if (el.dataset.profileBound) return;
    el.dataset.profileBound = '1';
    el.addEventListener('click', (e) => {
      const username = el.getAttribute('data-open-profile');
      if (!username) return;
      e.stopPropagation();
      openUserProfile(username);
    });
  });
};

const openUserProfile = (username) => {
  if (!username) return;
  if (state.currentUser?.username === username) {
    setActiveSection('profile');
    document.querySelectorAll('.main-nav-label').forEach((btn) => {
      btn.classList.toggle('bg-sys-accent/10', btn.dataset.tab === 'profile');
      btn.classList.toggle('text-sys-text-primary', btn.dataset.tab === 'profile');
    });
    return;
  }
  state.previousTab = getActiveViewTab();
  state.viewingProfile = username;
  document.querySelectorAll('section[class^="view-"]').forEach((s) => s.classList.add('hidden'));
  document.querySelector('.view-user-profile')?.classList.remove('hidden');
  loadPublicProfile(username);
};

const closeUserProfile = () => {
  state.viewingProfile = null;
  const tab = state.previousTab || 'community';
  setActiveSection(tab);
  document.querySelectorAll('.main-nav-label').forEach((btn) => {
    const active = btn.dataset.tab === tab;
    btn.classList.toggle('bg-sys-accent/10', active);
    btn.classList.toggle('text-sys-text-primary', active);
  });
};

const renderPublicProfileActions = (profile) => {
  const actions = document.getElementById('public-profile-actions');
  if (!actions) return;
  if (profile.isSelf) {
    actions.innerHTML = '<button type="button" class="composer-action-btn" id="public-go-own-profile">Edit your profile</button>';
    document.getElementById('public-go-own-profile')?.addEventListener('click', () => {
      closeUserProfile();
      setActiveSection('profile');
    });
    return;
  }
  if (profile.isFriend) {
    actions.innerHTML = `
      <span class="text-[10px] font-mono text-emerald-400 uppercase px-3 py-2 border border-emerald-500/30 rounded-lg">Friends</span>
      <button type="button" class="btn-accent text-[10px] font-mono font-bold uppercase tracking-widest px-4 py-2 rounded-lg" id="public-message-btn">Message</button>
    `;
    document.getElementById('public-message-btn')?.addEventListener('click', () => {
      openDmWithFriend(profile);
      setActiveSection('direct');
      document.querySelectorAll('.main-nav-label').forEach((btn) => {
        const active = btn.dataset.tab === 'direct';
        btn.classList.toggle('bg-sys-accent/10', active);
        btn.classList.toggle('text-sys-text-primary', active);
      });
    });
    return;
  }
  if (profile.hasPendingRequest) {
    actions.innerHTML = '<span class="text-[10px] font-mono text-amber-400 uppercase">Respond in Discover</span>';
    return;
  }
  if (profile.requestSent) {
    actions.innerHTML = '<span class="text-[10px] font-mono text-sys-text-muted uppercase">Request pending</span>';
    return;
  }
  actions.innerHTML = `<button type="button" class="btn-accent text-[10px] font-mono font-bold uppercase tracking-widest px-4 py-2 rounded-lg" id="public-add-friend-btn">Add friend</button>`;
  document.getElementById('public-add-friend-btn')?.addEventListener('click', async () => {
    try {
      await apiPost('/api/friends/request', { userId: profile.id });
      profile.requestSent = true;
      renderPublicProfileActions(profile);
    } catch (err) {
      alert('Could not send friend request');
    }
  });
};

const loadPublicProfile = async (username) => {
  const loading = document.getElementById('public-profile-loading');
  const content = document.getElementById('public-profile-content');
  if (loading) loading.classList.remove('hidden');
  if (content) content.classList.add('hidden');
  try {
    const profile = await apiGet(`/api/users/${encodeURIComponent(username)}`);
    if (loading) loading.classList.add('hidden');
    if (content) content.classList.remove('hidden');
    const avatar = profile.avatarUrl || DEFAULT_PROFILE.avatar;
    document.getElementById('public-profile-avatar').src = avatar;
    document.getElementById('public-profile-name').textContent = profile.displayName || profile.username;
    const unameEl = document.getElementById('public-profile-username');
    if (unameEl) {
      unameEl.textContent = `@${profile.username}`;
      unameEl.setAttribute('data-open-profile', profile.username);
    }
    document.getElementById('public-profile-bio').textContent = profile.bio || 'No bio yet.';
    const meta = [`${profile.friendsCount ?? 0} friends`];
    if (profile.isFriend) meta.push('You are friends');
    document.getElementById('public-profile-meta').textContent = meta.join(' · ');
    renderPublicProfileActions(profile);
    const friendsBtn = document.getElementById('public-profile-friends-btn');
    const friendsPreview = document.getElementById('public-profile-friends-preview');
    const canSeeFriends = profile.isSelf || profile.isFriend;
    if (friendsBtn) {
      friendsBtn.classList.toggle('hidden', !canSeeFriends);
      friendsBtn.onclick = async () => {
        try {
          const friends = await apiGet(`/api/friends/user/${profile.id}`);
          renderFriendsModal(friends);
          document.getElementById('friends-modal')?.classList.remove('hidden');
        } catch (err) {
          alert('Friends list is not available');
        }
      };
    }
    if (friendsPreview) {
      if (canSeeFriends) {
        try {
          const friends = await apiGet(`/api/friends/user/${profile.id}`);
          friendsPreview.innerHTML = friends.length
            ? friends.slice(0, 5).map((f) => renderFriendRow(f)).join('')
            : '<p class="text-[10px] text-sys-text-muted">No friends yet.</p>';
          bindProfileLinks(friendsPreview);
        } catch {
          friendsPreview.innerHTML = '<p class="text-[10px] text-sys-text-muted">Friends list is private.</p>';
        }
      } else {
        friendsPreview.innerHTML = '<p class="text-[10px] text-sys-text-muted">Add as a friend to see their connections.</p>';
      }
    }
    document.querySelector('[data-public-profile-avatar-wrap]')?.setAttribute('data-open-profile', profile.username);
    bindProfileLinks(content);
    const posts = await apiGet(`/api/posts/profile/${profile.id}`);
    const postsEl = document.getElementById('public-profile-posts');
    if (postsEl) {
      if (!posts.length) postsEl.innerHTML = '<p class="text-[10px] text-sys-text-muted font-mono">No posts yet.</p>';
      else renderPostsInto(postsEl, posts);
    }
    const dmProfileBtn = document.getElementById('dm-open-profile-btn');
    if (dmProfileBtn) {
      dmProfileBtn.classList.remove('hidden');
      dmProfileBtn.onclick = () => openUserProfile(profile.username);
    }
  } catch (err) {
    console.error('public profile failed', err);
    if (loading) {
      loading.textContent = 'Could not load this profile.';
      loading.classList.remove('hidden');
    }
    if (content) content.classList.add('hidden');
  }
};

const loadFriends = async () => {
  try {
    const friends = await apiGet('/api/friends');
    state.friends = friends;
    renderFriendsPreview(friends);
    renderFriendsModal(friends);
    renderDmFriendsList();
    return friends;
  } catch (err) {
    console.warn('friends load failed', err);
    renderFriendsPreview([]);
    renderDmFriendsList();
    return [];
  }
};

const refreshFriendsState = async () => {
  await loadFriends();
  await loadRooms();
  if (getActiveViewTab() === 'discover') {
    renderDiscoverPendingFromState();
    renderDiscoverUsers(state.discover.users);
  }
};

const loadProfilePosts = async () => {
  if (!state.currentUser?.id) return;
  try {
    const posts = await apiGet(`/api/posts/profile/${state.currentUser.id}`);
    renderProfilePosts(posts);
  } catch (err) {
    console.warn('profile posts load failed', err);
    renderProfilePosts([]);
  }
};

const discoverUserAction = (user) => {
  if (user.isSelf) return '';
  if (user.isFriend) {
    return '<span class="text-[9px] font-mono text-emerald-400 uppercase">Friends</span>';
  }
  if (user.hasPendingRequest) {
    return '<span class="text-[9px] font-mono text-amber-400 uppercase">Incoming request</span>';
  }
  if (user.requestSent) {
    return '<span class="text-[9px] font-mono text-sys-text-muted uppercase">Request sent</span>';
  }
  return `<button type="button" data-friend-request="${user.id}" class="composer-action-btn text-sys-accent">Add friend</button>`;
};

const patchDiscoverUser = (userId, patch) => {
  const user = state.discover.users.find((u) => u.id === userId);
  if (user) Object.assign(user, patch);
};

const renderDiscoverUser = (user) => {
  const name = user.displayName || user.username;
  const avatar = user.avatarUrl || DEFAULT_PROFILE.avatar;
  return `
    <div class="flex items-center justify-between gap-3 p-3 border border-sys-border bg-sys-bg-primary rounded-lg profile-link" data-open-profile="${user.username}">
      <div class="flex items-center gap-3 min-w-0 flex-1">
        <div class="w-10 h-10 rounded-full overflow-hidden border border-sys-border shrink-0 bg-sys-secondary">
          <img src="${avatar}" alt="${name}" class="w-full h-full object-cover">
        </div>
        <div class="min-w-0">
          <p class="text-[11px] font-bold text-sys-text-primary truncate">${name}</p>
          <p class="text-[9px] font-mono text-sys-text-secondary">@${user.username}</p>
        </div>
      </div>
      <div class="shrink-0" data-discover-actions="${user.id}">${discoverUserAction(user)}</div>
    </div>
  `;
};

const renderDiscoverUsers = (users) => {
  state.discover.users = users || [];
  const list = document.getElementById('discover-users-list');
  if (!list) return;
  if (!users.length) {
    list.innerHTML = '<p class="text-[10px] text-sys-text-muted font-mono">No users found.</p>';
    return;
  }
  list.innerHTML = users.map(renderDiscoverUser).join('');
  bindProfileLinks(list);
  list.querySelectorAll('[data-friend-request]').forEach((btn) => {
    btn.addEventListener('click', async (e) => {
      e.stopPropagation();
      const userId = Number(btn.dataset.friendRequest);
      const actions = list.querySelector(`[data-discover-actions="${userId}"]`);
      if (actions) actions.innerHTML = '<span class="text-[9px] font-mono text-sys-text-muted uppercase">Request sent</span>';
      patchDiscoverUser(userId, { requestSent: true, hasPendingRequest: false });
      try {
        await apiPost('/api/friends/request', { userId });
        const outgoing = await apiGet('/api/friends/pending/sent');
        state.discover.outgoing = outgoing;
        renderDiscoverPendingFromState();
      } catch (err) {
        console.error('friend request failed', err);
        patchDiscoverUser(userId, { requestSent: false });
        if (actions) actions.innerHTML = discoverUserAction(state.discover.users.find((u) => u.id === userId));
        alert('Could not send friend request');
      }
    });
  });
};

const renderDiscoverPendingFromState = () => {
  renderDiscoverPending(state.discover.incoming, state.discover.outgoing);
};

const acceptFriendRequest = async (friendshipId) => {
  const req = state.discover.incoming.find((r) => r.id === friendshipId);
  state.discover.incoming = state.discover.incoming.filter((r) => r.id !== friendshipId);
  renderDiscoverPendingFromState();
  if (req) {
    patchDiscoverUser(req.requesterId, { isFriend: true, hasPendingRequest: false, requestSent: false });
    renderDiscoverUsers(state.discover.users);
  }
  try {
    const newFriend = await apiPostEmpty(`/api/friends/${friendshipId}/accept`);
    if (newFriend && !state.friends.find((f) => f.id === newFriend.id)) {
      state.friends.push(newFriend);
    }
    await refreshFriendsState();
  } catch (err) {
    console.error('accept failed', err);
    if (req) state.discover.incoming.push(req);
    renderDiscoverPendingFromState();
    alert('Could not accept request');
  }
};

const rejectFriendRequest = async (friendshipId) => {
  const req = state.discover.incoming.find((r) => r.id === friendshipId);
  state.discover.incoming = state.discover.incoming.filter((r) => r.id !== friendshipId);
  renderDiscoverPendingFromState();
  if (req) {
    patchDiscoverUser(req.requesterId, { hasPendingRequest: false });
    renderDiscoverUsers(state.discover.users);
  }
  try {
    await apiPostEmpty(`/api/friends/${friendshipId}/reject`);
  } catch (err) {
    console.error('reject failed', err);
    if (req) state.discover.incoming.push(req);
    renderDiscoverPendingFromState();
    alert('Could not decline request');
  }
};

const renderDiscoverPending = (incoming, outgoing) => {
  state.discover.incoming = incoming || [];
  state.discover.outgoing = outgoing || [];
  const incomingEl = document.getElementById('discover-pending-incoming');
  const sentEl = document.getElementById('discover-pending-sent');
  if (incomingEl) {
    if (!incoming.length) {
      incomingEl.innerHTML = '<p class="text-[10px] text-sys-text-muted">No incoming requests.</p>';
    } else {
      incomingEl.innerHTML = incoming.map((req) => `
        <div class="flex items-center justify-between gap-2 p-2 border border-sys-border rounded-lg bg-sys-bg-primary" data-pending-id="${req.id}">
          <span class="text-[10px] text-sys-text-primary profile-link cursor-pointer" data-open-profile="${req.requesterUsername}">@${req.requesterUsername}</span>
          <div class="flex gap-1">
            <button type="button" data-accept-friend="${req.id}" class="composer-action-btn text-emerald-400">Accept</button>
            <button type="button" data-reject-friend="${req.id}" class="composer-action-btn">Decline</button>
          </div>
        </div>
      `).join('');
      bindProfileLinks(incomingEl);
      incomingEl.querySelectorAll('[data-accept-friend]').forEach((btn) => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          acceptFriendRequest(Number(btn.dataset.acceptFriend));
        });
      });
      incomingEl.querySelectorAll('[data-reject-friend]').forEach((btn) => {
        btn.addEventListener('click', (e) => {
          e.stopPropagation();
          rejectFriendRequest(Number(btn.dataset.rejectFriend));
        });
      });
    }
  }
  if (sentEl) {
    if (!outgoing.length) {
      sentEl.innerHTML = '<p class="text-[10px] text-sys-text-muted">No sent requests.</p>';
    } else {
      sentEl.innerHTML = outgoing.map((req) => `
        <div class="flex items-center justify-between gap-2 p-2 border border-sys-border rounded-lg bg-sys-bg-primary" data-pending-sent-id="${req.id}">
          <span class="text-[10px] text-sys-text-primary profile-link cursor-pointer" data-open-profile="${req.addresseeUsername}">@${req.addresseeUsername}</span>
          <span class="text-[9px] font-mono text-sys-text-muted uppercase">Pending</span>
        </div>
      `).join('');
      bindProfileLinks(sentEl);
    }
  }
};

const loadDiscover = async () => {
  try {
    const [incoming, outgoing, users] = await Promise.all([
      apiGet('/api/friends/pending'),
      apiGet('/api/friends/pending/sent'),
      apiGet('/api/users/browse')
    ]);
    renderDiscoverPending(incoming, outgoing);
    renderDiscoverUsers(users);
  } catch (err) {
    console.warn('discover load failed', err);
    renderDiscoverPending([], []);
    renderDiscoverUsers([]);
  }
};

const searchDiscoverUsers = async () => {
  const q = document.getElementById('discover-search-input')?.value?.trim();
  if (!q) {
    loadDiscover();
    return;
  }
  try {
    const users = await apiGet(`/api/users/search?q=${encodeURIComponent(q)}`);
    renderDiscoverUsers(users);
  } catch (err) {
    console.error('search failed', err);
    renderDiscoverUsers([]);
  }
};

const buildFriendRoomMap = (rooms) => {
  state.friendRoomByUserId = {};
  (rooms || []).forEach((room) => {
    const type = (room.type || '').toLowerCase();
    if ((type.includes('dm') || type.includes('direct')) && room.dmPartnerId) {
      state.friendRoomByUserId[room.dmPartnerId] = room;
    }
  });
};

const getRoomUnread = (room) => {
  if (!room) return 0;
  return room.unreadCount ?? 0;
};

const renderDmFriendsList = () => {
  const container = document.getElementById('dm-threads-container');
  if (!container) return;
  const friends = state.friends || [];
  if (!friends.length) {
    container.innerHTML = `<div class="p-4 rounded-xl border border-sys-border bg-sys-bg-primary text-[10px] text-sys-text-secondary leading-relaxed">No friends yet. Connect with people in <strong class="text-sys-accent">Discover</strong> to start messaging.</div>`;
    return;
  }
  container.innerHTML = friends.map((friend) => {
    const name = friend.displayName || friend.username;
    const avatar = friend.avatarUrl || DEFAULT_PROFILE.avatar;
    const active = state.activeDmFriendId === friend.id;
    const room = state.friendRoomByUserId[friend.id];
    const unread = getRoomUnread(room);
    const preview = room?.lastMessagePreview || 'Start a conversation';
    return `
      <button type="button" data-friend-id="${friend.id}" data-friend-username="${friend.username}"
        class="dm-friend-selector w-full p-3 text-left rounded-lg border border-transparent hover:bg-sys-secondary/60 transition-theme ${active ? 'is-active border-sys-accent/40' : ''}">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full overflow-hidden border border-sys-border shrink-0 bg-sys-secondary profile-link" data-open-profile="${friend.username}">
            <img src="${avatar}" alt="${name}" class="w-full h-full object-cover">
          </div>
          <div class="min-w-0 flex-1">
            <div class="flex items-center justify-between gap-2">
              <h4 class="text-xs font-bold text-sys-text-primary truncate">${name}</h4>
              ${unread > 0 ? `<span class="unread-badge shrink-0">${unread > 99 ? '99+' : unread}</span>` : ''}
            </div>
            <p class="text-[9px] text-sys-text-secondary truncate mt-0.5">${preview}</p>
          </div>
        </div>
      </button>
    `;
  }).join('');
  bindProfileLinks(container);
  container.querySelectorAll('.dm-friend-selector').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      if (e.target.closest('[data-open-profile]')) return;
      const friend = friends.find((f) => f.id === Number(btn.dataset.friendId));
      if (friend) openDmWithFriend(friend);
    });
  });
};

const openDmWithFriend = async (friend) => {
  if (!friend?.id) return;
  state.activeDmFriendId = friend.id;
  renderDmFriendsList();
  const titleText = document.getElementById('dm-room-title-text');
  const subtitle = document.getElementById('dm-room-subtitle');
  if (titleText) titleText.textContent = friend.displayName || friend.username;
  if (subtitle) subtitle.textContent = `@${friend.username}`;
  const dmProfileBtn = document.getElementById('dm-open-profile-btn');
  if (dmProfileBtn) {
    dmProfileBtn.classList.remove('hidden');
    dmProfileBtn.onclick = () => openUserProfile(friend.username);
  }
  try {
    const room = await apiPost('/api/rooms/dm', { userId: friend.id });
    const existingIdx = state.rooms.findIndex((r) => r.id === room.id);
    if (existingIdx >= 0) state.rooms[existingIdx] = room;
    else state.rooms.push(room);
    buildFriendRoomMap(state.rooms);
    subscribeToRoom(room.id);
    renderChatRooms(state.rooms);
    await selectRoom(room);
  } catch (err) {
    console.error('open dm failed', err);
    alert('Could not open conversation');
  }
};

const renderChatRooms = (rooms) => {
  state.rooms = rooms || state.rooms;
  buildFriendRoomMap(state.rooms);
  renderDmFriendsList();
  const groupContainer = document.getElementById('group-threads-container');
  if (!groupContainer) return;

  const directRooms = rooms.filter((room) => (room.type || '').toString().toLowerCase().includes('dm') || (room.type || '').toString().toLowerCase().includes('direct'));
  const groupRooms = rooms.filter((room) => !directRooms.includes(room));

  if (!groupRooms.length) {
    groupContainer.innerHTML = `<div class="p-4 rounded-3xl border border-sys-border bg-sys-bg-primary text-[10px] text-sys-text-secondary">No study rooms available. Create a group from the form above or join a campus channel.</div>`;
  } else {
    groupContainer.innerHTML = groupRooms.map((room) => {
      const unread = getRoomUnread(room);
      const preview = room.lastMessagePreview || 'No messages yet';
      const title = unread > 0
        ? `${room.name || room.slug || 'Study Room'} (${unread > 99 ? '99+' : unread})`
        : (room.name || room.slug || 'Study Room');
      return `
      <div data-room-id="${room.id}" class="room-thread-selector p-3.5 block text-left cursor-pointer hover:bg-sys-secondary/60 rounded-lg transition-theme ${state.activeRoomId === room.id ? 'is-active border border-sys-accent/30 bg-sys-accent/5' : ''}">
        <div class="flex items-start gap-3">
          <span class="text-lg font-bold text-sys-accent">#</span>
          <div class="min-w-0 flex-1">
            <div class="flex items-center justify-between gap-2">
              <h4 class="text-xs font-bold text-sys-text-primary uppercase tracking-wide leading-none truncate">${title}</h4>
              ${unread > 0 ? `<span class="unread-badge shrink-0">${unread > 99 ? '99+' : unread}</span>` : ''}
            </div>
            <p class="text-[10px] text-sys-text-secondary font-light truncate mt-1.5">${preview}</p>
            <span class="inline-block mt-1.5 font-mono text-[8px] uppercase text-sys-text-muted">${room.onlineCount || 0} online</span>
          </div>
        </div>
      </div>
    `;
    }).join('');
  }

  groupContainer.querySelectorAll('[data-room-id]').forEach((item) => {
    item.addEventListener('click', () => {
      const roomId = Number(item.dataset.roomId);
      const room = rooms.find((r) => r.id === roomId);
      if (room) selectRoom(room);
    });
  });
};

const sortMessages = (messages) => [...messages].sort((a, b) => {
  const ta = new Date(a.createdAt || 0).getTime();
  const tb = new Date(b.createdAt || 0).getTime();
  return ta - tb;
});

const renderMessageBubble = (message) => {
  const isMine = message.senderId === state.currentUser?.id;
  const rowClass = isMine ? 'is-mine' : 'is-theirs';
  const bubbleClass = isMine ? 'chat-bubble-out' : 'chat-bubble-in';
  const senderLabel = isMine ? 'You' : (message.senderUsername || 'User');
  return `
    <div class="chat-message-row ${rowClass}" data-message-id="${message.id || ''}">
      <div class="chat-bubble ${bubbleClass}">
        ${!isMine ? `<div class="text-[9px] font-mono text-sys-text-muted mb-1 profile-link" data-open-profile="${message.senderUsername || ''}">${escapeHtml(senderLabel)}</div>` : ''}
        <div>${escapeHtml(message.body || '')}</div>
        <div class="chat-bubble-meta">${formatChatTime(message.createdAt)}</div>
      </div>
    </div>
  `;
};

const renderChatHistory = (messages, roomId = state.activeRoomId) => {
  const dmStream = document.getElementById('dm-stream-container');
  const groupStream = document.getElementById('group-stream-container');
  const target = state.activeRoomType === 'group' ? groupStream : dmStream;
  const other = state.activeRoomType === 'group' ? dmStream : groupStream;
  if (!target || !other) return;

  other.innerHTML = '';
  const sorted = sortMessages(messages || []);
  if (roomId) state.messagesByRoom[roomId] = sorted;

  if (!sorted.length) {
    target.innerHTML = `<div class="p-4 rounded-xl border border-sys-border bg-sys-bg-primary text-[10px] text-sys-text-secondary">No messages yet. Say hello!</div>`;
    return;
  }

  target.innerHTML = `<div class="chat-messages-list">${sorted.map(renderMessageBubble).join('')}</div>`;
  target.scrollTop = target.scrollHeight;
  bindProfileLinks(target);
};

const appendChatMessage = (message, roomId = state.activeRoomId) => {
  if (!roomId) return;
  let list = [...(state.messagesByRoom[roomId] || [])];
  if (message.id && list.some((m) => m.id === message.id)) {
    if (state.activeRoomId === roomId) renderChatHistory(list, roomId);
    return;
  }
  if (message.id && !String(message.id).startsWith('tmp-')) {
    list = list.filter((m) => !(
      String(m.id).startsWith('tmp-')
      && m.body === message.body
      && m.senderId === message.senderId
    ));
  }
  state.messagesByRoom[roomId] = sortMessages([...list, message]);
  if (state.activeRoomId === roomId) {
    renderChatHistory(state.messagesByRoom[roomId], roomId);
  }
};

const handleIncomingChatMessage = (message) => {
  const roomId = message.roomId;
  if (!roomId) return;

  const room = state.rooms.find((r) => r.id === roomId);
  if (room) {
    room.lastMessagePreview = message.body?.length > 80 ? `${message.body.slice(0, 77)}...` : message.body;
    room.lastMessageAt = message.createdAt;
  }

  const isActive = state.activeRoomId === roomId;
  const isMine = message.senderId === state.currentUser?.id;

  if (isActive) {
    appendChatMessage(message, roomId);
    if (!isMine) markRoomRead(roomId);
  } else if (!isMine) {
    if (room) room.unreadCount = (room.unreadCount || 0) + 1;
  }

  if (room?.dmPartnerId) {
    const mapped = state.friendRoomByUserId[room.dmPartnerId];
    if (mapped) mapped.unreadCount = room.unreadCount;
  }

  renderDmFriendsList();
  renderChatRooms(state.rooms);
};

const markRoomRead = async (roomId) => {
  if (!roomId) return;
  const room = state.rooms.find((r) => r.id === roomId);
  if (room) room.unreadCount = 0;
  if (room?.dmPartnerId && state.friendRoomByUserId[room.dmPartnerId]) {
    state.friendRoomByUserId[room.dmPartnerId].unreadCount = 0;
  }
  try {
    await apiPostEmpty(`/api/rooms/${roomId}/read`);
    await loadRooms();
  } catch (err) {
    console.warn('mark read failed', err);
    renderDmFriendsList();
    renderChatRooms(state.rooms);
  }
};

const subscribeToRoom = (roomId) => {
  if (!stompClient || !wsConnected || state.roomSubscriptions[roomId]) return;
  state.roomSubscriptions[roomId] = stompClient.subscribe(`/topic/room.${roomId}`, (frame) => {
    try {
      const payload = JSON.parse(frame.body);
      handleIncomingChatMessage(payload);
    } catch (err) {
      console.error('invalid chat message payload', err);
    }
  });
};

const subscribeAllRooms = () => {
  (state.rooms || []).forEach((room) => subscribeToRoom(room.id));
};

const selectRoom = async (room) => {
  state.activeRoomId = room.id;
  const lowerType = (room.type || '').toString().toLowerCase();
  state.activeRoomType = (lowerType.includes('dm') || lowerType.includes('direct')) ? 'direct' : 'group';

  document.querySelectorAll('[data-room-id]').forEach((el) => {
    el.classList.toggle('bg-sys-accent/10', Number(el.dataset.roomId) === room.id);
  });

  if (state.activeRoomType === 'group') {
    const titleText = document.getElementById('group-room-title-text');
    if (titleText) titleText.textContent = room.name || room.slug || 'Selected Room';
  }

  const input = document.getElementById(state.activeRoomType === 'group' ? 'group-input' : 'dm-input');
  const button = document.getElementById(state.activeRoomType === 'group' ? 'group-submit-btn' : 'dm-send-btn');
  if (input) {
    input.placeholder = state.activeRoomType === 'group' ? 'Start typing to post in the group channel...' : 'Send a private whisper...';
    input.readOnly = false;
  }
  if (button) {
    button.disabled = false;
    button.classList.remove('cursor-not-allowed', 'bg-sys-accent/45');
  }

  try {
    await apiPost(`/api/rooms/${room.id}/join`, {});
  } catch (err) {
    // join may not be required, ignore failures
  }

  subscribeToRoom(room.id);

  await loadRoomMessages(room.id);
  await markRoomRead(room.id);
  startActiveRoomPoll();
  openMobileChatPanel(state.activeRoomType === 'group' ? 'group' : 'direct');
};

const loadRoomMessages = async (roomId) => {
  try {
    const messages = await apiGet(`/api/rooms/${roomId}/messages?limit=100`);
    renderChatHistory(messages, roomId);
  } catch (err) {
    console.error('failed to load room history', err);
  }
};

const sendRoomMessage = async () => {
  if (!state.activeRoomId) return;
  const input = document.getElementById(state.activeRoomType === 'group' ? 'group-input' : 'dm-input');
  if (!input || !input.value.trim()) return;

  const roomId = state.activeRoomId;
  const body = input.value.trim();
  input.value = '';

  const optimistic = {
    id: `tmp-${Date.now()}`,
    roomId,
    body,
    senderId: state.currentUser?.id,
    senderUsername: state.currentUser?.username,
    senderAvatarUrl: state.currentUser?.avatarUrl,
    createdAt: new Date().toISOString()
  };
  appendChatMessage(optimistic, roomId);

  const payload = { body, imageUrl: null, parentId: null };

  try {
    const saved = await apiPost(`/api/rooms/${roomId}/messages`, payload);
    const list = state.messagesByRoom[roomId] || [];
    state.messagesByRoom[roomId] = sortMessages([
      ...list.filter((m) => m.id !== optimistic.id),
      saved
    ]);
    renderChatHistory(state.messagesByRoom[roomId], roomId);

    const room = state.rooms.find((r) => r.id === roomId);
    if (room) {
      room.lastMessagePreview = body.length > 80 ? `${body.slice(0, 77)}...` : body;
      room.lastMessageAt = saved.createdAt;
    }
    renderDmFriendsList();
    renderChatRooms(state.rooms);
  } catch (err) {
    console.error('send message failed', err);
    state.messagesByRoom[roomId] = (state.messagesByRoom[roomId] || []).filter((m) => m.id !== optimistic.id);
    renderChatHistory(state.messagesByRoom[roomId], roomId);
    input.value = body;
    alert('Could not send message. Check your connection and try again.');
  }
};

const getActiveViewTab = () => {
  const section = Array.from(document.querySelectorAll('section[class^="view-"]')).find((el) => !el.classList.contains('hidden'));
  if (!section) return 'community';
  const match = section.className.match(/view-([a-z]+)/);
  return match ? match[1] : 'community';
};

const loadRooms = async () => {
  try {
    const rooms = await apiGet('/api/rooms');
    state.rooms = rooms;
    buildFriendRoomMap(rooms);
    renderChatRooms(rooms);
    subscribeAllRooms();
  } catch (err) {
    console.error('failed to load rooms', err);
  }
};

const renderAiSessions = (sessions) => {
  state.aiConversations = sessions;
  const container = document.getElementById('ai-sessions-container');
  if (!container) return;

  if (!sessions || sessions.length === 0) {
    container.innerHTML = `<div class="p-4 rounded-3xl border border-sys-border bg-sys-bg-primary text-[10px] text-sys-text-secondary">No advisor sessions found yet. Send a message to start a new AI conversation.</div>`;
    return;
  }

  container.innerHTML = sessions.map((session) => {
    const title = session.title || (session.createdAt ? new Date(session.createdAt).toLocaleString() : `Session ${session.id}`);
    return `
    <div data-session-id="${session.id}" class="ai-session-selector p-3.5 block text-left cursor-pointer hover:bg-sys-secondary/60 rounded-lg transition-theme">
      <div class="flex items-start gap-2.5">
        <span class="font-mono text-xs text-sys-accent font-bold">[${session.id}]</span>
        <div class="min-w-0">
          <h4 class="text-xs font-bold text-sys-text-primary truncate leading-none">${escapeHtml(title)}</h4>
          <span class="text-[8px] font-mono block mt-1.5 text-sys-text-secondary">${session.messagesCount || 0} messages</span>
        </div>
      </div>
    </div>
  `;
  }).join('');

  container.querySelectorAll('[data-session-id]').forEach((item) => {
    item.addEventListener('click', async () => {
      const id = Number(item.dataset.sessionId);
      selectAiSession(id);
    });
  });
};

const renderAiMessages = (messages) => {
  const container = document.getElementById('ai-stream-container');
  if (!container) return;
  if (!messages || !messages.length) {
    container.innerHTML = `<div class="p-4 rounded-3xl border border-sys-border bg-sys-bg-primary text-[10px] text-sys-text-secondary">No conversation history found yet. Ask the advisor a question to begin.</div>`;
    return;
  }
  container.innerHTML = messages.map((msg) => `
    <div class="rounded-3xl p-4 ${msg.role === 'assistant' ? 'bg-sys-secondary/70' : 'bg-sys-accent/10'}">
      <div class="flex justify-between gap-3 mb-2 text-[10px] uppercase tracking-[0.2em] text-sys-text-secondary font-mono">
        <span>${msg.role === 'assistant' ? 'FETCH Assistant' : 'You'}</span>
        <span>${formatDateTime(msg.createdAt)}</span>
      </div>
      <p class="text-[11px] text-sys-text-primary leading-relaxed">${msg.content || ''}</p>
    </div>
  `).join('');
  container.scrollTop = container.scrollHeight;
};

const selectAiSession = async (id) => {
  state.activeAiConversationId = id;
  document.querySelectorAll('[data-session-id]').forEach((el) => {
    el.classList.toggle('bg-sys-accent/10', Number(el.dataset.sessionId) === id);
  });

  const aiInput = document.getElementById('ai-input');
  const aiSubmitBtn = document.getElementById('ai-submit-btn');
  if (aiInput && !aiSending) {
    aiInput.readOnly = false;
    aiInput.placeholder = 'Ask the FETCH assistant...';
  }
  if (aiSubmitBtn && !aiSending) {
    aiSubmitBtn.disabled = false;
    aiSubmitBtn.classList.remove('cursor-not-allowed', 'bg-sys-accent/45', 'opacity-60');
    aiSubmitBtn.classList.add('bg-sys-accent');
  }

  try {
    const details = await apiGet(`/api/ai/conversations/${id}`);
    renderAiMessages(details.messages || []);
  } catch (err) {
    console.error('failed to load AI conversation', err);
  }
  openMobileChatPanel('ai');
};

const setAiComposerBusy = (busy) => {
  const aiInput = document.getElementById('ai-input');
  const aiSubmitBtn = document.getElementById('ai-submit-btn');
  if (aiInput) aiInput.readOnly = busy;
  if (aiSubmitBtn) {
    aiSubmitBtn.disabled = busy;
    aiSubmitBtn.classList.toggle('cursor-not-allowed', busy);
    aiSubmitBtn.classList.toggle('opacity-60', busy);
    aiSubmitBtn.classList.toggle('bg-sys-accent/45', busy);
    aiSubmitBtn.classList.toggle('bg-sys-accent', !busy);
  }
};

const sendAiMessage = async () => {
  const input = document.getElementById('ai-input');
  if (!input || !input.value.trim() || aiSending) return;

  const message = input.value.trim();
  input.value = '';
  aiSending = true;
  setAiComposerBusy(true);

  try {
    const response = await apiPost('/api/ai/chat', { message, conversationId: state.activeAiConversationId });
    state.activeAiConversationId = response.conversationId;
    const createdConversationId = response.conversationId;
    if (createdConversationId && !state.aiConversations.find((c) => c.id === createdConversationId)) {
      state.aiConversations.unshift({ id: createdConversationId, createdAt: new Date().toISOString(), messagesCount: 1 });
      renderAiSessions(state.aiConversations);
    }
    if (createdConversationId) {
      await selectAiSession(createdConversationId);
      // refresh sessions list to pick up any generated titles
      try {
        const sessions = await apiGet('/api/ai/conversations');
        state.aiConversations = sessions || [];
        renderAiSessions(state.aiConversations);
        // keep the created session selected
        document.querySelectorAll('[data-session-id]').forEach((el) => {
          el.classList.toggle('bg-sys-accent/10', Number(el.dataset.sessionId) === createdConversationId);
        });
      } catch (e) {
        console.warn('could not refresh AI sessions', e);
      }
    } else {
      renderAiMessages([
        { role: 'user', content: message, createdAt: new Date().toISOString() },
        { role: 'assistant', content: response.reply, createdAt: new Date().toISOString() }
      ]);
    }
  } catch (err) {
    console.error('failed to send AI message', err);
    if (input && !input.value.trim()) input.value = message;
  } finally {
    aiSending = false;
    setAiComposerBusy(false);
  }
};

const connectWebSocket = async () => {
  try {
    const socket = new SockJS(`${window.location.origin}/ws`);
    const client = StompJs.Stomp.over(socket);
    client.reconnectDelay = 5000;
    stompClient = client;

    const token = await refreshAccessToken();
    client.onConnect = () => {
      wsConnected = true;
      console.log('WebSocket connected');
      subscribeAllRooms();
      if (state.activeRoomId) {
        const room = state.rooms.find((r) => r.id === state.activeRoomId);
        if (room) selectRoom(room);
      }
    };
    client.onStompError = (frame) => {
      console.error('STOMP error', frame.headers['message'], frame.body);
    };
    client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
    client.activate();
  } catch (err) {
    console.error('connectWebSocket failed', err);
  }
};

const renderPendingPostImages = () => {
  const preview = document.getElementById('feed-post-images-preview');
  if (!preview) return;
  preview.innerHTML = state.pendingPostFiles.map((file, index) => {
    const objectUrl = URL.createObjectURL(file);
    return `
      <div class="relative w-20 h-20 rounded-lg overflow-hidden border border-sys-border" data-pending-index="${index}">
        <img src="${objectUrl}" alt="Attachment preview" class="w-full h-full object-cover">
        <button type="button" data-remove-pending="${index}" class="absolute top-0.5 right-0.5 w-5 h-5 bg-black/70 text-white text-xs rounded-full leading-none" aria-label="Remove image">×</button>
      </div>
    `;
  }).join('');
  preview.querySelectorAll('[data-remove-pending]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const idx = Number(btn.getAttribute('data-remove-pending'));
      state.pendingPostFiles.splice(idx, 1);
      renderPendingPostImages();
      updateFeedPublishState();
    });
  });
};

const updateFeedPublishState = () => {
  const feedTextarea = document.getElementById('feed-post-textarea');
  const feedPublishBtn = document.getElementById('feed-publish-btn');
  if (!feedTextarea || !feedPublishBtn) return;
  const enabled = !!feedTextarea.value.trim();
  feedPublishBtn.disabled = !enabled;
  feedPublishBtn.classList.toggle('cursor-not-allowed', !enabled);
  feedPublishBtn.classList.toggle('bg-sys-accent/45', !enabled);
  feedPublishBtn.classList.toggle('bg-sys-accent', enabled);
  feedPublishBtn.classList.toggle('btn-accent', enabled);
  feedPublishBtn.classList.toggle('text-white', enabled);
  feedPublishBtn.classList.toggle('text-sys-text-muted', !enabled);
};

const initActions = () => {
  const dmSendBtn = document.getElementById('dm-send-btn');
  const groupSendBtn = document.getElementById('group-submit-btn');
  const aiSendBtn = document.getElementById('ai-submit-btn');
  const feedPublishBtn = document.getElementById('feed-publish-btn');
  const feedTextarea = document.getElementById('feed-post-textarea');
  const feedImagesInput = document.getElementById('feed-post-images');
  const discoverSearchBtn = document.getElementById('discover-search-btn');
  const discoverSearchInput = document.getElementById('discover-search-input');
  const profilePostPublishBtn = document.getElementById('profile-post-publish-btn');
  const profilePostTextarea = document.getElementById('profile-post-textarea');
  const friendsModal = document.getElementById('friends-modal');
  const friendsModalClose = document.getElementById('friends-modal-close');
  const profileViewFriendsBtn = document.getElementById('profile-view-friends-btn');
  const userProfileBackBtn = document.getElementById('user-profile-back-btn');
  const createGroupBtn = document.getElementById('create-group-btn');
  const profileSyncBtn = document.getElementById('profile-sync-btn');
  const profileLogoutBtn = document.getElementById('profile-logout-btn');
  const profileAvatarFile = document.getElementById('profile-avatar-file');
  const profileAvatarPreview = document.getElementById('profile-avatar-preview');

  if (profileLogoutBtn) {
    profileLogoutBtn.addEventListener('click', logoutUser);
  }

  if (dmSendBtn) dmSendBtn.addEventListener('click', sendRoomMessage);
  if (groupSendBtn) groupSendBtn.addEventListener('click', sendRoomMessage);
  if (aiSendBtn) aiSendBtn.addEventListener('click', sendAiMessage);

  const dmInput = document.getElementById('dm-input');
  const groupInput = document.getElementById('group-input');
  const aiInput = document.getElementById('ai-input');

  if (dmInput) {
    dmInput.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendRoomMessage();
      }
    });
  }

  if (groupInput) {
    groupInput.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendRoomMessage();
      }
    });
  }

  if (aiInput) {
    aiInput.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendAiMessage();
      }
    });
  }

  const aiNewBtn = document.getElementById('ai-new-chat-btn');
  if (aiNewBtn) {
    aiNewBtn.addEventListener('click', async () => {
      aiNewBtn.disabled = true;
      try {
        const created = await apiPost('/api/ai/conversations', {});
        if (created) {
          // add to local state and render
          state.aiConversations = [created, ...(state.aiConversations || [])];
          renderAiSessions(state.aiConversations);
          if (created.id) await selectAiSession(created.id);
        }
      } catch (err) {
        console.error('create ai conversation failed', err);
        alert('Could not create a new chat');
      } finally {
        aiNewBtn.disabled = false;
      }
    });
  }

  bindToggleButton('post-anonymous-btn', 'post-anonymous-toggle');
  bindToggleButton('profile-post-anonymous-btn', 'profile-post-anonymous-toggle');

  document.querySelectorAll('[data-profile-vis]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const isFriends = btn.dataset.profileVis === 'friends';
      const publicRadio = document.getElementById('profile-vis-public');
      const friendsRadio = document.getElementById('profile-vis-friends');
      if (publicRadio) publicRadio.checked = !isFriends;
      if (friendsRadio) friendsRadio.checked = isFriends;
      document.querySelectorAll('.composer-visibility-btn').forEach((b) => {
        b.classList.toggle('is-selected', b === btn);
      });
    });
  });

  if (discoverSearchBtn) discoverSearchBtn.addEventListener('click', searchDiscoverUsers);
  if (discoverSearchInput) {
    discoverSearchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        searchDiscoverUsers();
      }
    });
  }

  if (friendsModalClose && friendsModal) {
    friendsModalClose.addEventListener('click', () => friendsModal.classList.add('hidden'));
    friendsModal.addEventListener('click', (e) => {
      if (e.target === friendsModal) friendsModal.classList.add('hidden');
    });
  }
  if (profileViewFriendsBtn && friendsModal) {
    profileViewFriendsBtn.addEventListener('click', () => {
      renderFriendsModal(state.friends || []);
      friendsModal.classList.remove('hidden');
    });
  }

  const updateProfilePostPublishState = () => {
    if (!profilePostTextarea || !profilePostPublishBtn) return;
    const enabled = !!profilePostTextarea.value.trim();
    profilePostPublishBtn.disabled = !enabled;
    profilePostPublishBtn.classList.toggle('cursor-not-allowed', !enabled);
    profilePostPublishBtn.classList.toggle('bg-sys-accent/45', !enabled);
    profilePostPublishBtn.classList.toggle('bg-sys-accent', enabled);
    profilePostPublishBtn.classList.toggle('btn-accent', enabled);
    profilePostPublishBtn.classList.toggle('text-white', enabled);
    profilePostPublishBtn.classList.toggle('text-sys-text-muted', !enabled);
  };

  if (profilePostTextarea) {
    profilePostTextarea.addEventListener('input', updateProfilePostPublishState);
  }

  if (profilePostPublishBtn) {
    profilePostPublishBtn.addEventListener('click', async () => {
      const body = profilePostTextarea?.value?.trim();
      if (!body || profilePostPublishBtn.disabled) return;
      profilePostPublishBtn.disabled = true;
      try {
        const anonymous = !!document.getElementById('profile-post-anonymous-toggle')?.checked;
        const friendsOnly = document.getElementById('profile-vis-friends')?.checked;
        const visibility = friendsOnly ? 'FRIENDS_ONLY' : 'PUBLIC_FEED';
        await apiPost('/api/posts', { body, anonymous, visibility, imageUrls: [] });
        profilePostTextarea.value = '';
        resetAnonymousToggle('profile-post-anonymous-btn', 'profile-post-anonymous-toggle');
        updateProfilePostPublishState();
        await loadProfilePosts();
        if (visibility === 'PUBLIC_FEED') {
          const community = await apiGet('/api/posts');
          renderCommunity(community);
        }
      } catch (err) {
        console.error('profile post failed', err);
        alert('Could not publish profile post.');
        updateProfilePostPublishState();
      }
    });
  }

  if (feedTextarea) {
    feedTextarea.addEventListener('input', updateFeedPublishState);
  }

  if (feedImagesInput) {
    feedImagesInput.addEventListener('change', () => {
      const files = Array.from(feedImagesInput.files || []);
      if (files.length) {
        state.pendingPostFiles.push(...files);
        renderPendingPostImages();
      }
      feedImagesInput.value = '';
    });
  }

  if (profileAvatarFile && profileAvatarPreview) {
    profileAvatarFile.addEventListener('change', () => {
      const file = profileAvatarFile.files?.[0];
      if (!file) return;
      profileAvatarPreview.src = URL.createObjectURL(file);
    });
  }

  if (feedPublishBtn) {
    feedPublishBtn.addEventListener('click', async () => {
      const body = feedTextarea?.value?.trim();
      if (!body || feedPublishBtn.disabled) return;
      feedPublishBtn.disabled = true;
      try {
        const anonymous = !!document.getElementById('post-anonymous-toggle')?.checked;
        const imageUrls = [];
        for (const file of state.pendingPostFiles) {
          const uploaded = await uploadMedia(file, 'posts');
          if (uploaded?.url) imageUrls.push(uploaded.url);
        }
        await apiPost('/api/posts', { body, anonymous, visibility: 'PUBLIC_FEED', imageUrls });
        feedTextarea.value = '';
        state.pendingPostFiles = [];
        renderPendingPostImages();
        resetAnonymousToggle('post-anonymous-btn', 'post-anonymous-toggle');
        updateFeedPublishState();
        const fresh = await apiGet('/api/posts');
        renderCommunity(fresh);
      } catch (err) {
        console.error('publish failed', err);
        alert('Could not publish post. Sign in and try again.');
        updateFeedPublishState();
      }
    });
  }

  if (userProfileBackBtn) {
    userProfileBackBtn.addEventListener('click', closeUserProfile);
  }

  if (createGroupBtn) {
    createGroupBtn.addEventListener('click', async () => {
      const nameInput = document.getElementById('new-group-name-input');
      const membersInput = document.getElementById('new-group-members-input');
      const groupName = nameInput?.value?.trim();
      const usernames = membersInput?.value?.split(',').map((v) => v.trim()).filter(Boolean) || [];
      if (!groupName) {
        alert('Group name is required');
        return;
      }
      try {
        const memberIds = [];
        for (const uname of usernames) {
          const users = await apiGet(`/api/users/search?q=${encodeURIComponent(uname)}`);
          const exact = users.find((u) => (u.username || '').toLowerCase() === uname.toLowerCase()) || users[0];
          if (exact) memberIds.push(exact.id);
        }
        const room = await apiPost('/api/rooms/group', { name: groupName, memberIds });
        if (nameInput) nameInput.value = '';
        if (membersInput) membersInput.value = '';
        await loadRooms();
        subscribeToRoom(room.id);
        setActiveSection('group');
        document.querySelectorAll('.main-nav-label').forEach((btn) => {
          const active = btn.dataset.tab === 'group';
          btn.classList.toggle('bg-sys-accent/10', active);
          btn.classList.toggle('text-sys-text-primary', active);
        });
        await selectRoom(room);
      } catch (err) {
        console.error('failed to create group', err);
        alert('Could not create group');
      }
    });
  }

  if (profileSyncBtn) {
    profileSyncBtn.addEventListener('click', async () => {
      const username = document.getElementById('profile-username-input')?.value?.trim() || null;
      const displayName = document.getElementById('profile-name-input')?.value?.trim() || null;
      const bio = document.getElementById('profile-bio-textarea')?.value || null;
      let avatarUrl = document.getElementById('profile-avatar-input')?.value?.trim() || null;
      const avatarFile = document.getElementById('profile-avatar-file')?.files?.[0];
      profileSyncBtn.disabled = true;
      try {
        profileSyncBtn.textContent = 'Saving...';
        if (avatarFile) {
          const uploaded = await uploadMedia(avatarFile, 'avatars');
          avatarUrl = uploaded?.url || avatarUrl;
        }
        const profile = await apiPut('/api/users/me', { username, displayName, bio, avatarUrl });
        if (profileAvatarFile) profileAvatarFile.value = '';
        renderProfile(profile);
        profileSyncBtn.textContent = 'Saved';
        setTimeout(() => {
          profileSyncBtn.textContent = 'Save Profile';
        }, 2000);
      } catch (err) {
        console.error('failed to save profile', err);
        alert('Could not save profile');
        profileSyncBtn.textContent = 'Save Profile';
      } finally {
        profileSyncBtn.disabled = false;
      }
    });
  }

  updateFeedPublishState();
  updateProfilePostPublishState();
};

const enableAiComposer = () => {
  if (aiSending) return;
  const aiInput = document.getElementById('ai-input');
  const aiSubmitBtn = document.getElementById('ai-submit-btn');
  if (aiInput) {
    aiInput.readOnly = false;
    aiInput.placeholder = 'Ask the FETCH assistant...';
  }
  if (aiSubmitBtn) {
    aiSubmitBtn.disabled = false;
    aiSubmitBtn.classList.remove('cursor-not-allowed', 'bg-sys-accent/45', 'opacity-60');
    aiSubmitBtn.classList.add('bg-sys-accent');
  }
};

const loadAiSessions = async () => {
  try {
    const sessions = await apiGet('/api/ai/conversations');
    state.aiConversations = sessions || [];
    renderAiSessions(sessions || []);
    if (sessions && sessions.length) {
      // preserve current selection if present, otherwise open newest
      if (state.activeAiConversationId) {
        // ensure selected session remains highlighted
        document.querySelectorAll('[data-session-id]').forEach((el) => {
          el.classList.toggle('bg-sys-accent/10', Number(el.dataset.sessionId) === state.activeAiConversationId);
        });
      } else {
        await selectAiSession(sessions[0].id);
      }
    } else {
      state.activeAiConversationId = null;
      enableAiComposer();
    }
  } catch (err) {
    console.error('failed to load AI sessions', err);
    enableAiComposer();
  }
};

const showAuthHint = () => {
  let hint = document.getElementById('auth-hint-banner');
  if (hint) return;
  hint = document.createElement('div');
  hint.id = 'auth-hint-banner';
  hint.className = 'fixed bottom-4 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-full border border-sys-border bg-sys-secondary text-[10px] font-mono uppercase tracking-wider text-sys-text-secondary shadow-lg';
  hint.innerHTML = 'Sign in to use FETCH chats and assistant. <a href="/login" class="text-sys-accent font-bold ml-1 hover:underline">Log in</a> · <a href="/register" class="text-sys-accent font-bold hover:underline">Register</a>';
  document.body.appendChild(hint);
};

const loadProfile = async () => {
  try {
    const profile = await apiGet('/api/users/me');
    renderProfile(profile);
    const hint = document.getElementById('auth-hint-banner');
    if (hint) hint.remove();
    await loadFriends();
    await loadProfilePosts();
  } catch (err) {
    console.warn('profile load failed', err);
    renderProfile(DEFAULT_PROFILE);
    showAuthHint();
  }
};

const loadCommunity = async () => {
  try {
    const posts = await apiGet('/api/posts');
    renderCommunity(posts);
  } catch (err) {
    renderCommunity(DEFAULT_POSTS);
  }
};

const initTheme = () => {
  const body = document.body;
  const button = document.getElementById('theme-toggle-btn');
  if (!body || !button) return;

  const applyTheme = (isLight) => {
    body.classList.toggle('light-theme', isLight);
    button.innerHTML = isLight
      ? '🌙 <span class="text-[9px] font-bold tracking-wider hidden sm:inline">DARK MODE</span>'
      : '🌞 <span class="text-[9px] font-bold tracking-wider hidden sm:inline">LIGHT MODE</span>';
  };

  const savedTheme = localStorage.getItem(THEME_KEY);
  applyTheme(savedTheme === 'light');

  button.addEventListener('click', () => {
    const isLight = !body.classList.contains('light-theme');
    localStorage.setItem(THEME_KEY, isLight ? 'light' : 'dark');
    applyTheme(isLight);
  });
};

const initApp = async () => {
  setDocumentBranding();
  initPostImageModal();
  initMobileChatLayout();
  initPostComments({
    apiGet,
    apiPost,
    getCurrentUser: () => state.currentUser
  });
  initTabNavigation();
  initActions();
  initTheme();
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      loadRooms();
      if (state.activeRoomId) loadRoomMessages(state.activeRoomId);
    }
  });
  await loadProfile();
  await loadCommunity();
  await loadRooms();
  await connectWebSocket();
};

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  initApp();
}
