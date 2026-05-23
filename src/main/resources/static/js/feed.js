async function loadFeed() {
    const res = await apiFetch('/api/posts');
    const posts = await res.json();
    const container = document.getElementById('feed-list');
    container.innerHTML = '';
    posts.forEach(p => {
        const div = document.createElement('article');
        div.className = 'post-card';
        const author = p.anonymous ? 'Anonymous' : (p.authorUsername || 'Unknown');
        const images = (p.imageUrls || []).map(u => `<img src="${u}" alt="post" class="post-img">`).join('');
        div.innerHTML = `
            <header><strong>${author}</strong> <small>${new Date(p.createdAt).toLocaleString()}</small></header>
            <p>${escapeHtml(p.body)}</p>
            ${images}
            <footer>
                <button type="button" data-like="${p.id}">${p.likedByCurrentUser ? 'Unlike' : 'Like'} (${p.likeCount})</button>
                <button type="button" data-comments="${p.id}">Comments</button>
            </footer>
            <div id="comments-${p.id}" class="comments hidden"></div>`;
        container.appendChild(div);
    });
    container.querySelectorAll('[data-like]').forEach(btn => {
        btn.addEventListener('click', () => toggleLike(btn.dataset.like));
    });
    container.querySelectorAll('[data-comments]').forEach(btn => {
        btn.addEventListener('click', () => toggleComments(btn.dataset.comments));
    });
}

async function createPost(e) {
    e.preventDefault();
    const body = document.getElementById('post-body').value;
    const anonymous = document.getElementById('post-anonymous').checked;
    const imageUrl = document.getElementById('post-image-url').value;
    const imageUrls = imageUrl ? [imageUrl] : [];
    await apiFetch('/api/posts', {
        method: 'POST',
        body: JSON.stringify({ body, anonymous, visibility: 'PUBLIC_FEED', imageUrls })
    });
    e.target.reset();
    loadFeed();
}

async function toggleLike(postId) {
    await apiFetch(`/api/posts/${postId}/reactions`, { method: 'POST' });
    loadFeed();
}

async function toggleComments(postId) {
    const el = document.getElementById(`comments-${postId}`);
    el.classList.toggle('hidden');
    if (!el.classList.contains('hidden')) {
        const res = await apiFetch(`/api/posts/${postId}/comments`);
        const comments = await res.json();
        el.innerHTML = comments.map(c =>
            `<div class="comment"><strong>${c.authorUsername}</strong>: ${escapeHtml(c.body)}</div>`
        ).join('') + `<form class="comment-form" data-post="${postId}">
            <input name="body" placeholder="Comment... @username to mention" required>
            <button type="submit">Send</button>
        </form>`;
        el.querySelector('form').addEventListener('submit', e => addComment(e, postId));
    }
}

async function addComment(e, postId) {
    e.preventDefault();
    const body = e.target.body.value;
    await apiFetch(`/api/posts/${postId}/comments`, {
        method: 'POST',
        body: JSON.stringify({ body })
    });
    await toggleComments(postId);
    await toggleComments(postId);
}

async function uploadPostImage(input) {
    if (!input.files[0]) return;
    const fd = new FormData();
    fd.append('file', input.files[0]);
    fd.append('folder', 'posts');
    const res = await apiFetch('/api/media/upload', { method: 'POST', body: fd });
    const data = await res.json();
    document.getElementById('post-image-url').value = data.url;
}

function escapeHtml(text) {
    const d = document.createElement('div');
    d.textContent = text;
    return d.innerHTML;
}

document.addEventListener('DOMContentLoaded', async () => {
    if (!(await ensureToken())) {
        window.location.href = '/login';
        return;
    }
    const form = document.getElementById('create-post-form');
    if (form) form.addEventListener('submit', createPost);
    loadFeed();
});
