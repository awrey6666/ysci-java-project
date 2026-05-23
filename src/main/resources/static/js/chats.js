let stompClient = null;
let currentRoomId = null;

function connectWs(onConnect) {
    const token = getToken();
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = () => {};
    stompClient.connect({ Authorization: 'Bearer ' + token }, onConnect);
}

async function loadRooms() {
    const res = await apiFetch('/api/rooms');
    const rooms = await res.json();
    const list = document.getElementById('room-list');
    list.innerHTML = '';
    rooms.forEach(r => {
        const li = document.createElement('li');
        li.textContent = `${r.name} (${r.onlineCount} online)`;
        li.addEventListener('click', () => selectRoom(r.id, r.name));
        list.appendChild(li);
    });
}

function selectRoom(roomId, name) {
    currentRoomId = roomId;
    document.getElementById('chat-title').textContent = name;
    apiFetch(`/api/rooms/${roomId}/join`, { method: 'POST' });
    loadHistory(roomId);
    if (stompClient && stompClient.connected) {
        stompClient.subscribe(`/topic/room.${roomId}`, msg => appendMessage(JSON.parse(msg.body)));
    } else {
        connectWs(() => {
            stompClient.subscribe(`/topic/room.${roomId}`, msg => appendMessage(JSON.parse(msg.body)));
        });
    }
}

async function loadHistory(roomId) {
    const res = await apiFetch(`/api/rooms/${roomId}/messages`);
    const messages = await res.json();
    const box = document.getElementById('chat-messages');
    box.innerHTML = '';
    messages.forEach(appendMessage);
}

function appendMessage(m) {
    const box = document.getElementById('chat-messages');
    const div = document.createElement('div');
    div.className = 'chat-msg';
    const reply = m.parentId ? `<em>reply</em> ` : '';
    const img = m.imageUrl ? `<img src="${m.imageUrl}" class="chat-img">` : '';
    div.innerHTML = `${reply}<strong>${m.senderUsername}</strong>: ${m.body || ''} ${img}`;
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
}

function sendMessage(e) {
    e.preventDefault();
    const body = document.getElementById('chat-input').value;
    const imageUrl = document.getElementById('chat-image-url').value || null;
    stompClient.send('/app/chat.send', {}, JSON.stringify({
        roomId: currentRoomId,
        body,
        imageUrl,
        parentId: null
    }));
    document.getElementById('chat-input').value = '';
}

async function uploadChatImage(input) {
    if (!input.files[0]) return;
    const fd = new FormData();
    fd.append('file', input.files[0]);
    fd.append('folder', 'chats');
    const res = await apiFetch('/api/media/upload', { method: 'POST', body: fd });
    const data = await res.json();
    document.getElementById('chat-image-url').value = data.url;
}

document.addEventListener('DOMContentLoaded', async () => {
    if (!(await ensureToken())) {
        window.location.href = '/login';
        return;
    }
    loadRooms();
    connectWs(() => {});
    document.getElementById('chat-form').addEventListener('submit', sendMessage);
});
