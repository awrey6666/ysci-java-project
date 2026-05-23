async function loadProfile() {
    const res = await apiFetch('/api/users/me');
    const p = await res.json();
    document.getElementById('display-name').value = p.displayName || '';
    document.getElementById('bio').value = p.bio || '';
    document.getElementById('avatar-url').value = p.avatarUrl || '';
    document.getElementById('profile-username').textContent = p.username;
}

async function saveProfile(e) {
    e.preventDefault();
    await apiFetch('/api/users/me', {
        method: 'PUT',
        body: JSON.stringify({
            displayName: document.getElementById('display-name').value,
            bio: document.getElementById('bio').value,
            avatarUrl: document.getElementById('avatar-url').value
        })
    });
    alert('Profile saved');
}

async function createProfilePost(e) {
    e.preventDefault();
    const body = document.getElementById('profile-post-body').value;
    await apiFetch('/api/posts', {
        method: 'POST',
        body: JSON.stringify({ body, anonymous: false, visibility: 'FRIENDS_ONLY', imageUrls: [] })
    });
    e.target.reset();
}

document.addEventListener('DOMContentLoaded', async () => {
    if (!(await ensureToken())) {
        window.location.href = '/login';
        return;
    }
    document.getElementById('profile-form').addEventListener('submit', saveProfile);
    document.getElementById('profile-post-form').addEventListener('submit', createProfilePost);
    loadProfile();
});
