async function searchUsers(e) {
    e.preventDefault();
    const q = document.getElementById('search-q').value;
    const res = await apiFetch('/api/users/search?q=' + encodeURIComponent(q));
    const users = await res.json();
    const list = document.getElementById('search-results');
    list.innerHTML = '';
    users.forEach(u => {
        const li = document.createElement('li');
        li.innerHTML = `<strong>${u.username}</strong> — ${u.displayName || ''}
            <button data-id="${u.id}">Add friend</button>
            <button data-dm="${u.id}">Message</button>`;
        li.querySelector('[data-id]').addEventListener('click', () => sendFriendRequest(u.id));
        li.querySelector('[data-dm]').addEventListener('click', () => openDm(u.id));
        list.appendChild(li);
    });
}

async function sendFriendRequest(userId) {
    await apiFetch('/api/friends/request', {
        method: 'POST',
        body: JSON.stringify({ userId })
    });
    alert('Friend request sent');
}

async function openDm(userId) {
    const res = await apiFetch('/api/rooms/dm', {
        method: 'POST',
        body: JSON.stringify({ userId })
    });
    const room = await res.json();
    window.location.href = '/chats?room=' + room.id;
}

document.addEventListener('DOMContentLoaded', () => {
    if (!getToken()) {
        window.location.href = '/login';
        return;
    }
    document.getElementById('search-form').addEventListener('submit', searchUsers);
});
