(() => {
    window.AppViews = window.AppViews || {};

    let activeTab = 'customers';
    let searchTerm = '';
    let filterCountryId = '';

    async function renderCustomersTab(bodyEl) {
        const [countries, docTypes, customers] = await Promise.all([
            api.get('/api/countries'),
            api.get('/api/document-types'),
            filterCountryId
                ? api.get(`/api/customers?countryId=${encodeURIComponent(filterCountryId)}`)
                : api.get('/api/customers')
        ]);

        const q = searchTerm.trim().toLowerCase();
        const filtered = q
            ? customers.filter(c =>
                (c.id || '').toLowerCase().includes(q) ||
                (c.firstName || '').toLowerCase().includes(q) ||
                (c.lastName || '').toLowerCase().includes(q) ||
                (c.email || '').toLowerCase().includes(q)
            )
            : customers;

        bodyEl.innerHTML = `
            <div class="card">
                <div class="card-header">
                    <div class="toolbar">
                        <input id="cust-search" class="form-control" type="search" placeholder="Buscar por documento o apellido..." value="${escapeHtml(searchTerm)}">
                        <select id="cust-country-filter" class="form-control">
                            <option value="">Todos los países</option>
                            ${countries.map(co => `<option value="${co.id}" ${String(co.id) === String(filterCountryId) ? 'selected' : ''}>${escapeHtml(co.name)}</option>`).join('')}
                        </select>
                    </div>
                    <button id="btn-new-customer" class="btn btn-primary">+ Nuevo Cliente</button>
                </div>
                <div class="table-wrap">
                    ${filtered.length === 0 ? `<div class="empty-state">No hay clientes para mostrar.</div>` : `
                        <table>
                            <thead>
                                <tr>
                                    <th>Documento</th>
                                    <th>Tipo</th>
                                    <th>Nombre Completo</th>
                                    <th>Teléfono</th>
                                    <th>Correo</th>
                                    <th>País</th>
                                    <th>Dirección</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${filtered.map(c => `
                                    <tr>
                                        <td><strong>${escapeHtml(c.id)}</strong></td>
                                        <td><span class="badge badge-sky">${escapeHtml(c.documentType?.id || '')}</span></td>
                                        <td>${escapeHtml(c.fullName || `${c.firstName} ${c.lastName}`)}</td>
                                        <td>${escapeHtml(c.telephoneNumber)}</td>
                                        <td>${escapeHtml(c.email || '-')}</td>
                                        <td>${escapeHtml(c.country?.name || '')}</td>
                                        <td>${escapeHtml(c.address || '-')}</td>
                                        <td>
                                            <div class="toolbar">
                                                <button class="btn btn-secondary btn-sm" data-edit-cust="${escapeHtml(c.id)}">Editar</button>
                                                <button class="btn btn-danger btn-sm" data-del-cust="${escapeHtml(c.id)}">Eliminar</button>
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

        bodyEl.querySelector('#cust-search').addEventListener('input', (e) => {
            searchTerm = e.target.value;
            renderCustomersTab(bodyEl);
        });

        bodyEl.querySelector('#cust-country-filter').addEventListener('change', (e) => {
            filterCountryId = e.target.value;
            renderCustomersTab(bodyEl);
        });

        bodyEl.querySelector('#btn-new-customer').addEventListener('click', () => {
            openCustomerModal(null, countries, docTypes, () => renderCustomersTab(bodyEl));
        });

        bodyEl.querySelectorAll('[data-edit-cust]').forEach(btn => {
            btn.addEventListener('click', () => {
                const id = btn.getAttribute('data-edit-cust');
                const found = customers.find(c => c.id === id);
                if (found) {
                    openCustomerModal(found, countries, docTypes, () => renderCustomersTab(bodyEl));
                }
            });
        });

        bodyEl.querySelectorAll('[data-del-cust]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-del-cust');
                if (!confirm(`¿Seguro que deseas eliminar al cliente ${id}?`)) return;
                await api.delete(`/api/customers/${encodeURIComponent(id)}`);
                toast.success('Cliente eliminado correctamente.');
                renderCustomersTab(bodyEl);
            });
        });
    }

    function openCustomerModal(customer, countries, docTypes, onSaved) {
        const isEdit = Boolean(customer);
        const title = isEdit ? `Editar Cliente (${customer.id})` : 'Registrar Nuevo Cliente';

        modal.open(title, `
            <form id="customer-form">
                <div class="form-grid">
                    <div class="form-group">
                        <label>Documento / ID *</label>
                        <input class="form-control" name="id" required maxlength="20" value="${escapeHtml(customer?.id || '')}" ${isEdit ? 'readonly' : ''}>
                    </div>
                    <div class="form-group">
                        <label>Tipo de Documento *</label>
                        <select class="form-control" name="documentTypeId" required>
                            ${docTypes.map(d => `<option value="${escapeHtml(d.id)}" ${d.id === customer?.documentType?.id ? 'selected' : ''}>[${escapeHtml(d.id)}] ${escapeHtml(d.description)}</option>`).join('')}
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Nombre *</label>
                        <input class="form-control" name="firstName" required maxlength="30" value="${escapeHtml(customer?.firstName || '')}">
                    </div>
                    <div class="form-group">
                        <label>Apellido *</label>
                        <input class="form-control" name="lastName" required maxlength="30" value="${escapeHtml(customer?.lastName || '')}">
                    </div>
                    <div class="form-group">
                        <label>Teléfono *</label>
                        <input class="form-control" name="telephoneNumber" required maxlength="15" value="${escapeHtml(customer?.telephoneNumber || '')}">
                    </div>
                    <div class="form-group">
                        <label>País *</label>
                        <select class="form-control" name="countryId" required>
                            ${countries.map(co => `<option value="${co.id}" ${co.id === customer?.country?.id ? 'selected' : ''}>${escapeHtml(co.name)}</option>`).join('')}
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Correo Electrónico</label>
                        <input class="form-control" name="email" type="email" maxlength="80" value="${escapeHtml(customer?.email || '')}">
                    </div>
                    <div class="form-group">
                        <label>Dirección</label>
                        <input class="form-control" name="address" maxlength="40" value="${escapeHtml(customer?.address || '')}">
                    </div>
                </div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" id="cust-cancel">Cancelar</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Guardar Cambios' : 'Registrar Cliente'}</button>
                </div>
            </form>
        `, (root) => {
            root.querySelector('#cust-cancel').addEventListener('click', modal.close);
            root.querySelector('#customer-form').addEventListener('submit', async (e) => {
                e.preventDefault();
                const fd = new FormData(e.target);
                const payload = {
                    id: fd.get('id'),
                    firstName: fd.get('firstName'),
                    lastName: fd.get('lastName'),
                    documentTypeId: fd.get('documentTypeId'),
                    telephoneNumber: fd.get('telephoneNumber'),
                    countryId: Number(fd.get('countryId')),
                    email: fd.get('email') || null,
                    address: fd.get('address') || null
                };
                if (isEdit) {
                    await api.put(`/api/customers/${encodeURIComponent(customer.id)}`, payload);
                    toast.success('Cliente actualizado.');
                } else {
                    await api.post('/api/customers', payload);
                    toast.success('Cliente registrado.');
                }
                modal.close();
                onSaved();
            });
        });
    }

    async function renderCountriesTab(bodyEl) {
        const countries = await api.get('/api/countries');
        bodyEl.innerHTML = `
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">Catálogo de Países</h2>
                    <button id="btn-new-country" class="btn btn-primary">+ Nuevo País</button>
                </div>
                <div class="table-wrap">
                    ${countries.length === 0 ? `<div class="empty-state">No hay países registrados.</div>` : `
                        <table>
                            <thead>
                                <tr><th>ID</th><th>Nombre</th><th>Acciones</th></tr>
                            </thead>
                            <tbody>
                                ${countries.map(co => `
                                    <tr>
                                        <td><strong>#${co.id}</strong></td>
                                        <td>${escapeHtml(co.name)}</td>
                                        <td>
                                            <div class="toolbar">
                                                <button class="btn btn-secondary btn-sm" data-edit-co="${co.id}" data-name="${escapeHtml(co.name)}">Editar</button>
                                                <button class="btn btn-danger btn-sm" data-del-co="${co.id}">Eliminar</button>
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

        bodyEl.querySelector('#btn-new-country').addEventListener('click', () => {
            openCountryModal(null, () => renderCountriesTab(bodyEl));
        });

        bodyEl.querySelectorAll('[data-edit-co]').forEach(btn => {
            btn.addEventListener('click', () => {
                openCountryModal({ id: Number(btn.getAttribute('data-edit-co')), name: btn.getAttribute('data-name') }, () => renderCountriesTab(bodyEl));
            });
        });

        bodyEl.querySelectorAll('[data-del-co]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-del-co');
                if (!confirm(`¿Eliminar el país #${id}?`)) return;
                await api.delete(`/api/countries/${encodeURIComponent(id)}`);
                toast.success('País eliminado.');
                renderCountriesTab(bodyEl);
            });
        });
    }

    function openCountryModal(country, onSaved) {
        const isEdit = Boolean(country);
        modal.open(isEdit ? `Editar País #${country.id}` : 'Registrar País', `
            <form id="country-form">
                <div class="form-grid">
                    <div class="form-group">
                        <label>ID numérico (ej. 57) *</label>
                        <input class="form-control" name="id" type="number" min="1" max="32767" required value="${country?.id ?? ''}" ${isEdit ? 'readonly' : ''}>
                    </div>
                    <div class="form-group">
                        <label>Nombre del País *</label>
                        <input class="form-control" name="name" required maxlength="20" value="${escapeHtml(country?.name || '')}">
                    </div>
                </div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" id="co-cancel">Cancelar</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Actualizar' : 'Guardar'}</button>
                </div>
            </form>
        `, (root) => {
            root.querySelector('#co-cancel').addEventListener('click', modal.close);
            root.querySelector('#country-form').addEventListener('submit', async (e) => {
                e.preventDefault();
                const fd = new FormData(e.target);
                const payload = { id: Number(fd.get('id')), name: fd.get('name') };
                if (isEdit) {
                    await api.put(`/api/countries/${country.id}`, payload);
                    toast.success('País actualizado.');
                } else {
                    await api.post('/api/countries', payload);
                    toast.success('País registrado.');
                }
                modal.close();
                onSaved();
            });
        });
    }

    async function renderDocTypesTab(bodyEl) {
        const docs = await api.get('/api/document-types');
        bodyEl.innerHTML = `
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">Tipos de Documento</h2>
                    <button id="btn-new-doc" class="btn btn-primary">+ Nuevo Tipo</button>
                </div>
                <div class="table-wrap">
                    ${docs.length === 0 ? `<div class="empty-state">No hay tipos de documento registrados.</div>` : `
                        <table>
                            <thead>
                                <tr><th>Código</th><th>Descripción</th><th>Acciones</th></tr>
                            </thead>
                            <tbody>
                                ${docs.map(d => `
                                    <tr>
                                        <td><span class="badge badge-sky">${escapeHtml(d.id)}</span></td>
                                        <td>${escapeHtml(d.description)}</td>
                                        <td>
                                            <div class="toolbar">
                                                <button class="btn btn-secondary btn-sm" data-edit-doc="${escapeHtml(d.id)}" data-desc="${escapeHtml(d.description)}">Editar</button>
                                                <button class="btn btn-danger btn-sm" data-del-doc="${escapeHtml(d.id)}">Eliminar</button>
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

        bodyEl.querySelector('#btn-new-doc').addEventListener('click', () => {
            openDocModal(null, () => renderDocTypesTab(bodyEl));
        });

        bodyEl.querySelectorAll('[data-edit-doc]').forEach(btn => {
            btn.addEventListener('click', () => {
                openDocModal({ id: btn.getAttribute('data-edit-doc'), description: btn.getAttribute('data-desc') }, () => renderDocTypesTab(bodyEl));
            });
        });

        bodyEl.querySelectorAll('[data-del-doc]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-del-doc');
                if (!confirm(`¿Eliminar el tipo de documento ${id}?`)) return;
                await api.delete(`/api/document-types/${encodeURIComponent(id)}`);
                toast.success('Tipo de documento eliminado.');
                renderDocTypesTab(bodyEl);
            });
        });
    }

    function openDocModal(doc, onSaved) {
        const isEdit = Boolean(doc);
        modal.open(isEdit ? `Editar Tipo (${doc.id})` : 'Registrar Tipo de Documento', `
            <form id="doc-form">
                <div class="form-grid">
                    <div class="form-group">
                        <label>Código (2 letras, ej. CC) *</label>
                        <input class="form-control" name="id" maxlength="2" required value="${escapeHtml(doc?.id || '')}" ${isEdit ? 'readonly' : ''}>
                    </div>
                    <div class="form-group">
                        <label>Descripción *</label>
                        <input class="form-control" name="description" maxlength="20" required value="${escapeHtml(doc?.description || '')}">
                    </div>
                </div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" id="doc-cancel">Cancelar</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Actualizar' : 'Guardar'}</button>
                </div>
            </form>
        `, (root) => {
            root.querySelector('#doc-cancel').addEventListener('click', modal.close);
            root.querySelector('#doc-form').addEventListener('submit', async (e) => {
                e.preventDefault();
                const fd = new FormData(e.target);
                const payload = { id: fd.get('id'), description: fd.get('description') };
                if (isEdit) {
                    await api.put(`/api/document-types/${encodeURIComponent(doc.id)}`, payload);
                    toast.success('Tipo de documento actualizado.');
                } else {
                    await api.post('/api/document-types', payload);
                    toast.success('Tipo de documento registrado.');
                }
                modal.close();
                onSaved();
            });
        });
    }

    window.AppViews.customers = {
        title: 'Módulo 1 · Clientes y Catálogos',
        subtitle: 'Gestión de clientes, países y tipos de documento',
        async render(container) {
            container.innerHTML = `
                <div class="subtabs">
                    <button class="subtab ${activeTab === 'customers' ? 'active' : ''}" data-tab="customers">👥 Clientes</button>
                    <button class="subtab ${activeTab === 'countries' ? 'active' : ''}" data-tab="countries">🌎 Países</button>
                    <button class="subtab ${activeTab === 'docTypes' ? 'active' : ''}" data-tab="docTypes">🪪 Tipos de Documento</button>
                </div>
                <div id="cust-tab-body"></div>
            `;

            const bodyEl = container.querySelector('#cust-tab-body');

            container.querySelectorAll('[data-tab]').forEach(btn => {
                btn.addEventListener('click', () => {
                    activeTab = btn.getAttribute('data-tab');
                    window.AppViews.customers.render(container);
                });
            });

            if (activeTab === 'customers') await renderCustomersTab(bodyEl);
            else if (activeTab === 'countries') await renderCountriesTab(bodyEl);
            else await renderDocTypesTab(bodyEl);
        }
    };
})();
