const TOKEN_KEY = 'afetch_access_token';

function saveToken(data) {
    sessionStorage.setItem(TOKEN_KEY, data.accessToken);
}

function getToken() {
    return sessionStorage.getItem(TOKEN_KEY);
}

function clearToken() {
    sessionStorage.removeItem(TOKEN_KEY);
}

async function apiFetch(url, options = {}) {
    const headers = options.headers || {};
    const token = getToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    if (options.body && !(options.body instanceof FormData)) {
        headers['Content-Type'] = 'application/json';
    }
    let response = await fetch(url, { ...options, headers, credentials: 'include' });

    if (response.status === 401 && !url.includes('/api/auth/')) {
        const refreshed = await fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' });
        if (refreshed.ok) {
            const data = await refreshed.json();
            saveToken(data);
            headers['Authorization'] = 'Bearer ' + data.accessToken;
            response = await fetch(url, { ...options, headers, credentials: 'include' });
        }
    }
    return response;
}

async function registerUser(username, email, password) {
    const res = await fetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ username, email, password })
    });
    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();
    saveToken(data);
    return data;
}

async function loginUser(login, password) {
    const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ login, password })
    });
    if (!res.ok) throw new Error(await res.text());
    const data = await res.json();
    saveToken(data);
    return data;
}

async function logoutUser() {
    await apiFetch('/api/auth/logout', { method: 'POST' });
    clearToken();
    window.location.href = '/login';
}
