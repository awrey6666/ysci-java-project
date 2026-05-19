async function sendAiMessage(e) {
    e.preventDefault();
    const input = document.getElementById('ai-input');
    const message = input.value.trim();
    if (!message) return;
    appendBubble('user', message);
    input.value = '';
    const res = await apiFetch('/api/ai/chat', {
        method: 'POST',
        body: JSON.stringify({ message })
    });
    if (!res.ok) {
        appendBubble('error', 'AI unavailable. Check OPENROUTER_API_KEY.');
        return;
    }
    const data = await res.json();
    appendBubble('assistant', data.reply);
}

function appendBubble(role, text) {
    const box = document.getElementById('ai-messages');
    const div = document.createElement('div');
    div.className = 'ai-bubble ai-' + role;
    div.textContent = text;
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
}

document.addEventListener('DOMContentLoaded', () => {
    if (!getToken()) {
        window.location.href = '/login';
        return;
    }
    document.getElementById('ai-form').addEventListener('submit', sendAiMessage);
});
