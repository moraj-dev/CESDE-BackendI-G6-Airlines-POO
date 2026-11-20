(() => {
    window.AppViews = window.AppViews || {};

    const viewContainer = document.getElementById('view-container');
    const pageTitle = document.getElementById('page-title');
    const pageSubtitle = document.getElementById('page-subtitle');
    const connectionPill = document.getElementById('connection-pill');
    const connectionText = document.getElementById('connection-text');
    const sidebar = document.getElementById('sidebar');
    const backdrop = document.getElementById('sidebar-backdrop');
    const menuToggle = document.getElementById('menu-toggle');
    const refreshBtn = document.getElementById('refresh-btn');

    let currentView = 'dashboard';

    function setSidebarOpen(open) {
        sidebar.classList.toggle('open', open);
        backdrop.classList.toggle('open', open);
    }

    menuToggle?.addEventListener('click', () => setSidebarOpen(!sidebar.classList.contains('open')));
    backdrop?.addEventListener('click', () => setSidebarOpen(false));

    function setConnectionStatus(online) {
        connectionPill.classList.remove('status-checking', 'status-online', 'status-offline');
        if (online) {
            connectionPill.classList.add('status-online');
            connectionText.textContent = 'Conectado · AerolinesPoo';
        } else {
            connectionPill.classList.add('status-offline');
            connectionText.textContent = 'Sin conexión';
        }
    }

    async function checkBackendHealth() {
        try {
            const res = await fetch((window.API_BASE || '') + '/api/dashboard');
            setConnectionStatus(res.ok);
        } catch {
            setConnectionStatus(false);
        }
    }

    async function renderCurrentView() {
        const viewDef = window.AppViews[currentView];
        if (!viewDef) {
            pageTitle.textContent = currentView.toUpperCase();
            pageSubtitle.textContent = 'Módulo pendiente de implementación en los siguientes prompts';
            viewContainer.innerHTML = `
                <div class="card">
                    <div class="card-body empty-state">
                        La vista del módulo <strong>${escapeHtml(currentView)}</strong> se agregará en el siguiente paso.
                    </div>
                </div>
            `;
            return;
        }

        pageTitle.textContent = viewDef.title;
        pageSubtitle.textContent = viewDef.subtitle || '';
        try {
            await viewDef.render(viewContainer);
            setConnectionStatus(true);
        } catch {
            setConnectionStatus(false);
        }
    }

    window.navigateTo = function navigateTo(viewName) {
        currentView = viewName;
        document.querySelectorAll('.nav-item').forEach(btn => {
            btn.classList.toggle('active', btn.getAttribute('data-view') === viewName);
        });
        setSidebarOpen(false);
        renderCurrentView();
    };

    document.querySelectorAll('.nav-item').forEach(btn => {
        btn.addEventListener('click', () => {
            window.navigateTo(btn.getAttribute('data-view'));
        });
    });

    refreshBtn?.addEventListener('click', renderCurrentView);

    window.addEventListener('DOMContentLoaded', () => {
        checkBackendHealth();
        window.navigateTo('dashboard');
    });
})();
