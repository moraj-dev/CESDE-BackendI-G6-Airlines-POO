(() => {
    window.AppViews = window.AppViews || {};

    let searchTerm = '';

    function openAirlineModal(airline, onSaved) {
        const isEdit = Boolean(airline);
        modal.open(isEdit ? `Renombrar Aerolínea #${airline.id}` : 'Registrar Nueva Aerolínea', `
            <form id="airline-form">
                <div class="form-group">
                    <label>Nombre de la Aerolínea *</label>
                    <input class="form-control" name="name" required maxlength="50" value="${escapeHtml(airline?.name || '')}">
                </div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" id="air-cancel">Cancelar</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Guardar Nombre' : 'Registrar Aerolínea'}</button>
                </div>
            </form>
        `, (root) => {
            root.querySelector('#air-cancel').addEventListener('click', modal.close);
            root.querySelector('#airline-form').addEventListener('submit', async (e) => {
                e.preventDefault();
                const fd = new FormData(e.target);
                const payload = { name: fd.get('name') };
                if (isEdit) {
                    await api.put(`/api/airlines/${airline.id}`, payload);
                    toast.success('Aerolínea renombrada.');
                } else {
                    await api.post('/api/airlines', payload);
                    toast.success('Aerolínea registrada.');
                }
                modal.close();
                onSaved();
            });
        });
    }

    window.AppViews.airlines = {
        title: 'Módulo 3 · Aerolíneas',
        subtitle: 'Catálogo de compañías aéreas (IDs 100, 110, 120...)',
        async render(container) {
            const airlines = await api.get('/api/airlines');
            const q = searchTerm.trim().toLowerCase();
            const filtered = q
                ? airlines.filter(a => (a.name || '').toLowerCase().includes(q) || String(a.id).includes(q))
                : airlines;

            container.innerHTML = `
                <div class="card">
                    <div class="card-header">
                        <div class="toolbar">
                            <input id="air-search" class="form-control" type="search" placeholder="Buscar por nombre o ID..." value="${escapeHtml(searchTerm)}">
                            <span class="badge badge-sky">${filtered.length} aerolínea(s)</span>
                        </div>
                        <button id="btn-new-airline" class="btn btn-primary">+ Nueva Aerolínea</button>
                    </div>
                    <div class="table-wrap">
                        ${filtered.length === 0 ? `<div class="empty-state">No hay aerolíneas registradas.</div>` : `
                            <table>
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Nombre de la Aerolínea</th>
                                        <th>Acciones</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${filtered.map(a => `
                                        <tr>
                                            <td><span class="badge badge-sky">#${a.id}</span></td>
                                            <td><strong>${escapeHtml(a.name)}</strong></td>
                                            <td>
                                                <div class="toolbar">
                                                    <button class="btn btn-secondary btn-sm" data-edit-air="${a.id}" data-name="${escapeHtml(a.name)}">Renombrar</button>
                                                    <button class="btn btn-danger btn-sm" data-del-air="${a.id}">Eliminar</button>
                                                </div>
                                            </td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        `}
                    </div>
                </div>
            `;

            container.querySelector('#air-search').addEventListener('input', (e) => {
                searchTerm = e.target.value;
                window.AppViews.airlines.render(container);
            });

            container.querySelector('#btn-new-airline').addEventListener('click', () => {
                openAirlineModal(null, () => window.AppViews.airlines.render(container));
            });

            container.querySelectorAll('[data-edit-air]').forEach(btn => {
                btn.addEventListener('click', () => {
                    openAirlineModal({
                        id: Number(btn.getAttribute('data-edit-air')),
                        name: btn.getAttribute('data-name')
                    }, () => window.AppViews.airlines.render(container));
                });
            });

            container.querySelectorAll('[data-del-air]').forEach(btn => {
                btn.addEventListener('click', async () => {
                    const id = btn.getAttribute('data-del-air');
                    if (!confirm(`¿Seguro que deseas eliminar la aerolínea #${id}?`)) return;
                    await api.delete(`/api/airlines/${encodeURIComponent(id)}`);
                    toast.success('Aerolínea eliminada.');
                    window.AppViews.airlines.render(container);
                });
            });
        }
    };
})();
