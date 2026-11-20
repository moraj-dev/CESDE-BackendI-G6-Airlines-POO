(() => {
    const toastContainer = document.getElementById('toast-container');

    function showToast(message, variant = 'info', duration = 4200) {
        if (!toastContainer) return;
        const el = document.createElement('div');
        el.className = `toast toast-${variant}`;
        el.textContent = message;
        toastContainer.appendChild(el);
        setTimeout(() => {
            el.remove();
        }, duration);
    }

    window.toast = {
        info: (msg) => showToast(msg, 'info'),
        success: (msg) => showToast(msg, 'success'),
        error: (msg) => showToast(msg, 'error', 5500)
    };

    const modalRoot = document.getElementById('modal-root');
    const modalTitle = document.getElementById('modal-title');
    const modalBody = document.getElementById('modal-body');
    const modalClose = document.getElementById('modal-close');

    function closeModal() {
        modalRoot.classList.add('hidden');
        modalBody.innerHTML = '';
    }

    modalClose?.addEventListener('click', closeModal);
    modalRoot?.querySelector('.modal-backdrop')?.addEventListener('click', closeModal);

    window.modal = {
        open(title, htmlContent, onBind) {
            modalTitle.textContent = title;
            modalBody.innerHTML = htmlContent;
            modalRoot.classList.remove('hidden');
            if (typeof onBind === 'function') {
                onBind(modalBody);
            }
        },
        close: closeModal
    };

    window.escapeHtml = function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    };

    const API_BASE = (window.location.port === '8080' && window.location.protocol.startsWith('http'))
        ? ''
        : 'http://localhost:8080';
    window.API_BASE = API_BASE;

    async function request(method, path, body) {
        const targetUrl = path.startsWith('/api') ? (API_BASE + path) : path;
        const options = {
            method,
            headers: { 'Content-Type': 'application/json' }
        };
        if (body !== undefined) {
            options.body = JSON.stringify(body);
        }

        let response;
        try {
            response = await fetch(targetUrl, options);
        } catch (networkErr) {
            const msg = 'No se pudo conectar con el servidor Java en http://localhost:8080.';
            window.toast.error(msg);
            throw new Error(msg);
        }

        const text = await response.text();
        let data = null;
        if (text) {
            try {
                data = JSON.parse(text);
            } catch {
                data = { error: text };
            }
        }

        if (!response.ok) {
            const serverMsg = (data && data.error) ? data.error : `Error HTTP ${response.status}`;
            window.toast.error(serverMsg);
            const err = new Error(serverMsg);
            err.status = response.status;
            err.type = data && data.type ? data.type : 'HttpError';
            throw err;
        }

        return data;
    }

    window.api = {
        get: (path) => request('GET', path),
        post: (path, body) => request('POST', path, body),
        put: (path, body) => request('PUT', path, body),
        delete: (path) => request('DELETE', path)
    };
})();
