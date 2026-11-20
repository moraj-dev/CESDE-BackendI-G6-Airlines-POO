(() => {
    window.AppViews = window.AppViews || {};

    let activeTab = 'airports';
    let cityFilterId = '';
    let searchTerm = '';

    async function renderAirportsTab(bodyEl) {
        const [cities, airports] = await Promise.all([
            api.get('/api/cities'),
            cityFilterId
                ? api.get(`/api/airports?cityId=${encodeURIComponent(cityFilterId)}`)
                : api.get('/api/airports')
        ]);

        const q = searchTerm.trim().toLowerCase();
        const filtered = q
            ? airports.filter(a =>
                (a.name || '').toLowerCase().includes(q) ||
                (a.id || '').toLowerCase().includes(q) ||
                (a.city?.name || '').toLowerCase().includes(q)
            )
            : airports;

        bodyEl.innerHTML = `
            <div class="card">
                <div class="card-header">
                    <div class="toolbar">
                        <input id="ap-search" class="form-control" type="search" placeholder="Buscar aeropuerto o UUID..." value="${escapeHtml(searchTerm)}">
                        <select id="ap-city-filter" class="form-control">
                            <option value="">Todas las ciudades</option>
                            ${cities.map(c => `<option value="${c.id}" ${String(c.id) === String(cityFilterId) ? 'selected' : ''}>${escapeHtml(c.name)}</option>`).join('')}
                        </select>
                    </div>
                    <button id="btn-new-airport" class="btn btn-primary">+ Nuevo Aeropuerto</button>
                </div>
                <div class="table-wrap">
                    ${filtered.length === 0 ? `<div class="empty-state">No hay aeropuertos registrados.</div>` : `
                        <table>
                            <thead>
                                <tr>
                                    <th>Aeropuerto</th>
                                    <th>Ciudad</th>
                                    <th>UUID</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${filtered.map(a => `
                                    <tr>
                                        <td><strong>${escapeHtml(a.name)}</strong></td>
                                        <td><span class="badge badge-sky">${escapeHtml(a.city?.name || '')}</span></td>
                                        <td><code>${escapeHtml(a.id)}</code></td>
                                        <td>
                                            <div class="toolbar">
                                                <button class="btn btn-secondary btn-sm" data-edit-ap="${escapeHtml(a.id)}">Editar</button>
                                                <button class="btn btn-danger btn-sm" data-del-ap="${escapeHtml(a.id)}">Eliminar</button>
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

        bodyEl.querySelector('#ap-search').addEventListener('input', (e) => {
            searchTerm = e.target.value;
            renderAirportsTab(bodyEl);
        });

        bodyEl.querySelector('#ap-city-filter').addEventListener('change', (e) => {
            cityFilterId = e.target.value;
            renderAirportsTab(bodyEl);
        });

        bodyEl.querySelector('#btn-new-airport').addEventListener('click', () => {
            openAirportModal(null, cities, () => renderAirportsTab(bodyEl));
        });

        bodyEl.querySelectorAll('[data-edit-ap]').forEach(btn => {
            btn.addEventListener('click', () => {
                const id = btn.getAttribute('data-edit-ap');
                const found = airports.find(a => a.id === id);
                if (found) openAirportModal(found, cities, () => renderAirportsTab(bodyEl));
            });
        });

        bodyEl.querySelectorAll('[data-del-ap]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-del-ap');
                if (!confirm('¿Seguro que deseas eliminar este aeropuerto?')) return;
                await api.delete(`/api/airports/${encodeURIComponent(id)}`);
                toast.success('Aeropuerto eliminado.');
                renderAirportsTab(bodyEl);
            });
        });
    }

    function openAirportModal(airport, cities, onSaved) {
        const isEdit = Boolean(airport);
        modal.open(isEdit ? 'Editar Aeropuerto' : 'Registrar Nuevo Aeropuerto', `
            <form id="airport-form">
                <div class="form-grid">
                    <div class="form-group">
                        <label>Nombre del Aeropuerto *</label>
                        <input class="form-control" name="name" required maxlength="50" value="${escapeHtml(airport?.name || '')}">
                    </div>
                    <div class="form-group">
                        <label>Ciudad *</label>
                        <select class="form-control" name="cityId" required>
                            ${cities.map(c => `<option value="${c.id}" ${c.id === airport?.city?.id ? 'selected' : ''}>${escapeHtml(c.name)}</option>`).join('')}
                        </select>
                    </div>
                </div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" id="ap-cancel">Cancelar</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Guardar Cambios' : 'Registrar Aeropuerto'}</button>
                </div>
            </form>
        `, (root) => {
            root.querySelector('#ap-cancel').addEventListener('click', modal.close);
            root.querySelector('#airport-form').addEventListener('submit', async (e) => {
                e.preventDefault();
                const fd = new FormData(e.target);
                const payload = { name: fd.get('name'), cityId: Number(fd.get('cityId')) };
                if (isEdit) {
                    await api.put(`/api/airports/${encodeURIComponent(airport.id)}`, payload);
                    toast.success('Aeropuerto actualizado.');
                } else {
                    await api.post('/api/airports', payload);
                    toast.success('Aeropuerto registrado.');
                }
                modal.close();
                onSaved();
            });
        });
    }

    async function renderCitiesTab(bodyEl) {
        const cities = await api.get('/api/cities');
        const q = searchTerm.trim().toLowerCase();
        const filtered = q ? cities.filter(c => (c.name || '').toLowerCase().includes(q)) : cities;

        bodyEl.innerHTML = `
            <div class="card">
                <div class="card-header">
                    <div class="toolbar">
                        <input id="city-search" class="form-control" type="search" placeholder="Buscar ciudad..." value="${escapeHtml(searchTerm)}">
                    </div>
                    <button id="btn-new-city" class="btn btn-primary">+ Nueva Ciudad</button>
                </div>
                <div class="table-wrap">
                    ${filtered.length === 0 ? `<div class="empty-state">No hay ciudades registradas.</div>` : `
                        <table>
                            <thead>
                                <tr><th>ID</th><th>Nombre</th><th>Acciones</th></tr>
                            </thead>
                            <tbody>
                                ${filtered.map(c => `
                                    <tr>
                                        <td><strong>#${c.id}</strong></td>
                                        <td>${escapeHtml(c.name)}</td>
                                        <td>
                                            <div class="toolbar">
                                                <button class="btn btn-secondary btn-sm" data-edit-city="${c.id}" data-name="${escapeHtml(c.name)}">Editar</button>
                                                <button class="btn btn-danger btn-sm" data-del-city="${c.id}">Eliminar</button>
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

        bodyEl.querySelector('#city-search').addEventListener('input', (e) => {
            searchTerm = e.target.value;
            renderCitiesTab(bodyEl);
        });

        bodyEl.querySelector('#btn-new-city').addEventListener('click', () => {
            openCityModal(null, () => renderCitiesTab(bodyEl));
        });

        bodyEl.querySelectorAll('[data-edit-city]').forEach(btn => {
            btn.addEventListener('click', () => {
                openCityModal({ id: Number(btn.getAttribute('data-edit-city')), name: btn.getAttribute('data-name') }, () => renderCitiesTab(bodyEl));
            });
        });

        bodyEl.querySelectorAll('[data-del-city]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-del-city');
                if (!confirm(`¿Seguro que deseas eliminar la ciudad #${id}?`)) return;
                await api.delete(`/api/cities/${encodeURIComponent(id)}`);
                toast.success('Ciudad eliminada.');
                renderCitiesTab(bodyEl);
            });
        });
    }

    function openCityModal(city, onSaved) {
        const isEdit = Boolean(city);
        modal.open(isEdit ? `Editar Ciudad #${city.id}` : 'Registrar Nueva Ciudad', `
            <form id="city-form">
                <div class="form-group">
                    <label>Nombre de la Ciudad *</label>
                    <input class="form-control" name="name" required maxlength="20" value="${escapeHtml(city?.name || '')}">
                </div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" id="city-cancel">Cancelar</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Guardar Cambios' : 'Registrar Ciudad'}</button>
                </div>
            </form>
        `, (root) => {
            root.querySelector('#city-cancel').addEventListener('click', modal.close);
            root.querySelector('#city-form').addEventListener('submit', async (e) => {
                e.preventDefault();
                const fd = new FormData(e.target);
                const payload = { name: fd.get('name') };
                if (isEdit) {
                    await api.put(`/api/cities/${city.id}`, payload);
                    toast.success('Ciudad actualizada.');
                } else {
                    await api.post('/api/cities', payload);
                    toast.success('Ciudad registrada.');
                }
                modal.close();
                onSaved();
            });
        });
    }

    window.AppViews.locations = {
        title: 'Módulo 2 · Ubicaciones',
        subtitle: 'Ciudades y aeropuertos conectados a SQL Server',
        async render(container) {
            container.innerHTML = `
                <div class="subtabs">
                    <button class="subtab ${activeTab === 'airports' ? 'active' : ''}" data-tab="airports">🛫 Aeropuertos</button>
                    <button class="subtab ${activeTab === 'cities' ? 'active' : ''}" data-tab="cities">🏙️ Ciudades</button>
                </div>
                <div id="loc-tab-body"></div>
            `;

            const bodyEl = container.querySelector('#loc-tab-body');

            container.querySelectorAll('[data-tab]').forEach(btn => {
                btn.addEventListener('click', () => {
                    activeTab = btn.getAttribute('data-tab');
                    searchTerm = '';
                    window.AppViews.locations.render(container);
                });
            });

            if (activeTab === 'airports') await renderAirportsTab(bodyEl);
            else await renderCitiesTab(bodyEl);
        }
    };
})();
