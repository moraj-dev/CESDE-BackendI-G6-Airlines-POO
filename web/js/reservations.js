(() => {
    window.AppViews = window.AppViews || {};
    window.AppContext = window.AppContext || {};

    let activeTab = 'list';
    let filterCustomerId = '';
    let filterFlightId = '';

    const CABIN_ROWS = 30;
    const SEAT_LETTERS = { 1: 'A', 2: 'B', 3: 'C', 4: 'D', 5: 'E', 6: 'F' };
    const LEFT_COLS = [1, 2, 3];
    const RIGHT_COLS = [4, 5, 6];

    function seatKey(row, col) {
        return `${String(row ?? '').trim().toUpperCase()}-${Number(col)}`;
    }

    function formatSeatLabel(row, col) {
        const cleanRow = String(row ?? '').trim().toUpperCase();
        const num = Number(col);
        const letter = SEAT_LETTERS[num];
        return letter ? `${cleanRow}${letter}` : `${cleanRow}-${num}`;
    }

    function formatFlightRoute(flightObj) {
        if (!flightObj || typeof flightObj !== 'object') {
            return { title: `Vuelo #${flightObj ?? '-'}`, route: 'Ruta no especificada', schedule: '' };
        }
        const dep = flightObj.departure?.city?.name || flightObj.departure?.name || 'Origen';
        const arr = flightObj.arrival?.city?.name || flightObj.arrival?.name || 'Destino';
        const schedule = flightObj.dateDeparture
            ? `${flightObj.dateDeparture} · ${flightObj.hourDeparture || ''}`
            : '';
        return {
            title: `#${flightObj.flightId ?? flightObj.id} · ${flightObj.flightName || ''}`,
            route: `${dep} → ${arr}`,
            schedule,
            airline: flightObj.airlineName || 'Airlines-poo'
        };
    }

    function renderCabinMapHtml(occupiedKeys, selectedKeys) {
        let rowsHtml = '';
        for (let r = 1; r <= CABIN_ROWS; r++) {
            const renderBtn = (c) => {
                const key = seatKey(r, c);
                const label = formatSeatLabel(r, c);
                const isOccupied = occupiedKeys.has(key);
                const isSelected = selectedKeys.has(key);
                const cls = isOccupied ? 'occupied' : (isSelected ? 'selected' : 'available');
                return `
                    <button type="button"
                            class="cabin-seat ${cls}"
                            data-seat-row="${r}"
                            data-seat-col="${c}"
                            ${isOccupied ? `disabled title="Silla ${label} ocupada"` : `title="Silla ${label} (Columna ${c})"`}>
                        ${label}
                    </button>
                `;
            };

            rowsHtml += `
                <div class="cabin-row">
                    <span class="cabin-row-label">${r}</span>
                    <div class="cabin-side">${LEFT_COLS.map(renderBtn).join('')}</div>
                    <div class="cabin-aisle" aria-hidden="true"></div>
                    <div class="cabin-side">${RIGHT_COLS.map(renderBtn).join('')}</div>
                </div>
            `;
        }

        return `
            <div class="cabin-legend">
                <span class="legend-item"><span class="legend-dot dot-available"></span> Libre</span>
                <span class="legend-item"><span class="legend-dot dot-selected"></span> Seleccionada</span>
                <span class="legend-item"><span class="legend-dot dot-occupied"></span> Ocupada</span>
            </div>
            <div class="cabin-fuselage cabin-scroll">
                <div class="cabin-cockpit">✈ FUSELAJE DE CABINA (Filas 1–${CABIN_ROWS} · A–F)</div>
                <div class="cabin-col-headers">
                    <span></span>
                    <div class="cabin-side"><span>A</span><span>B</span><span>C</span></div>
                    <div class="cabin-aisle"></div>
                    <div class="cabin-side"><span>D</span><span>E</span><span>F</span></div>
                </div>
                ${rowsHtml}
            </div>
        `;
    }

    // ------------------------------------------------------------------
    // Tab 1: Listado estilo Boarding Pass y gestión
    // ------------------------------------------------------------------

    async function renderListTab(bodyEl, customers, flights) {
        let query = '';
        if (filterCustomerId) {
            query = `?customerId=${encodeURIComponent(filterCustomerId)}`;
        } else if (filterFlightId) {
            query = `?flightId=${encodeURIComponent(filterFlightId)}`;
        }

        const reservations = await api.get(`/api/reservations${query}`);

        bodyEl.innerHTML = `
            <div class="card">
                <div class="card-header">
                    <div class="toolbar">
                        <select id="res-filter-cust" class="form-control">
                            <option value="">Todos los clientes</option>
                            ${customers.map(c => `<option value="${escapeHtml(c.id)}" ${c.id === filterCustomerId ? 'selected' : ''}>${escapeHtml(c.fullName)} (${escapeHtml(c.id)})</option>`).join('')}
                        </select>
                        <select id="res-filter-flight" class="form-control">
                            <option value="">Todos los vuelos</option>
                            ${flights.map(f => `<option value="${f.flightId}" ${String(f.flightId) === String(filterFlightId) ? 'selected' : ''}>#${f.flightId} · ${escapeHtml(f.flightName)}</option>`).join('')}
                        </select>
                    </div>
                    <button id="btn-go-new-res" class="btn btn-primary">+ Nueva Reserva</button>
                </div>
                <div class="card-body">
                    ${reservations.length === 0 ? `<div class="empty-state">No hay reservas registradas con estos filtros.</div>` : `
                        <div class="boarding-list">
                            ${reservations.map(res => {
                                const flightIdNum = typeof res.flight === 'object' ? (res.flight?.flightId ?? res.flight?.id) : res.flight;
                                const fInfo = formatFlightRoute(res.flight);
                                const details = Array.isArray(res.details) ? res.details : [];
                                return `
                                    <article class="boarding-pass">
                                        <div class="bp-main">
                                            <div class="bp-banner">
                                                <div>
                                                    <span class="bp-airline">${escapeHtml(fInfo.airline || 'AIRLINES-POO')}</span>
                                                    <strong class="bp-code">BOARDING PASS · RESERVA #${res.id}</strong>
                                                </div>
                                                <span class="badge badge-sky">${escapeHtml(fInfo.title)}</span>
                                            </div>
                                            <div class="bp-route-bar">
                                                <div>
                                                    <div class="bp-label">Titular</div>
                                                    <div class="bp-value">${escapeHtml(res.customer?.fullName || '')} (${escapeHtml(res.customer?.id || '')})</div>
                                                </div>
                                                <div>
                                                    <div class="bp-label">Ruta</div>
                                                    <div class="bp-value">${escapeHtml(fInfo.route)}</div>
                                                </div>
                                                <div>
                                                    <div class="bp-label">Salida</div>
                                                    <div class="bp-value">${escapeHtml(fInfo.schedule || 'Por definir')}</div>
                                                </div>
                                            </div>
                                            <div class="table-wrap">
                                                <table>
                                                    <thead>
                                                        <tr>
                                                            <th>Detalle</th>
                                                            <th>Pasajero</th>
                                                            <th>Documento</th>
                                                            <th>Silla</th>
                                                            <th>Acciones</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        ${details.map(d => {
                                                            const label = formatSeatLabel(d.seat?.rowNumber, d.seat?.seatNumber);
                                                            return `
                                                                <tr>
                                                                    <td>#${d.id}</td>
                                                                    <td><strong>${escapeHtml(d.passengerFullName)}</strong></td>
                                                                    <td>${escapeHtml(d.passengerDocument)}</td>
                                                                    <td><span class="badge badge-success">${escapeHtml(label)}</span></td>
                                                                    <td>
                                                                        <div class="toolbar">
                                                                            <button class="btn btn-secondary btn-sm"
                                                                                    data-change-seat="${res.id}"
                                                                                    data-detail-id="${d.id}"
                                                                                    data-flight-id="${flightIdNum}"
                                                                                    data-pax-name="${escapeHtml(d.passengerFullName)}"
                                                                                    data-pax-doc="${escapeHtml(d.passengerDocument)}"
                                                                                    data-seat-row="${escapeHtml(d.seat?.rowNumber || '')}"
                                                                                    data-seat-col="${d.seat?.seatNumber || ''}">
                                                                                ✏️ Editar / Silla
                                                                            </button>
                                                                            <button class="btn btn-danger btn-sm"
                                                                                    data-remove-pax="${res.id}"
                                                                                    data-pax-doc="${escapeHtml(d.passengerDocument)}">
                                                                                Quitar
                                                                            </button>
                                                                        </div>
                                                                    </td>
                                                                </tr>
                                                            `;
                                                        }).join('')}
                                                    </tbody>
                                                </table>
                                            </div>
                                        </div>

                                        <div class="bp-stub">
                                            <div class="bp-stub-head">
                                                <div class="bp-label">Reserva</div>
                                                <div class="bp-stub-id">#${res.id}</div>
                                                <span class="badge badge-sky">${details.length}/9 pasajero(s)</span>
                                            </div>
                                            <div class="bp-stub-actions">
                                                <button class="btn btn-primary btn-sm" data-add-pax="${res.id}" data-flight-id="${flightIdNum}">+ Agregar pasajero</button>
                                                <button class="btn btn-secondary btn-sm" data-change-flight="${res.id}" data-flight-id="${flightIdNum}">✈ Cambiar vuelo</button>
                                                <button class="btn btn-danger btn-sm" data-cancel-res="${res.id}">Cancelar reserva</button>
                                            </div>
                                        </div>
                                    </article>
                                `;
                            }).join('')}
                        </div>
                    `}
                </div>
            </div>
        `;

        bodyEl.querySelector('#res-filter-cust').addEventListener('change', (e) => {
            filterCustomerId = e.target.value;
            if (filterCustomerId) filterFlightId = '';
            renderListTab(bodyEl, customers, flights);
        });

        bodyEl.querySelector('#res-filter-flight').addEventListener('change', (e) => {
            filterFlightId = e.target.value;
            if (filterFlightId) filterCustomerId = '';
            renderListTab(bodyEl, customers, flights);
        });

        bodyEl.querySelector('#btn-go-new-res').addEventListener('click', () => {
            activeTab = 'new';
            window.AppViews.reservations.render(document.getElementById('view-container'));
        });

        bodyEl.querySelectorAll('[data-cancel-res]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = btn.getAttribute('data-cancel-res');
                if (!confirm(`¿Seguro que deseas cancelar la Reserva #${id}? Sus sillas quedarán libres.`)) return;
                await api.delete(`/api/reservations/${encodeURIComponent(id)}`);
                toast.success(`Reserva #${id} cancelada y sillas liberadas.`);
                renderListTab(bodyEl, customers, flights);
            });
        });

        bodyEl.querySelectorAll('[data-remove-pax]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const resId = btn.getAttribute('data-remove-pax');
                const doc = btn.getAttribute('data-pax-doc');
                if (!confirm(`¿Quitar al pasajero ${doc} de la Reserva #${resId}?`)) return;
                await api.delete(`/api/reservations/${encodeURIComponent(resId)}/passengers/${encodeURIComponent(doc)}`);
                toast.success('Pasajero retirado de la reserva.');
                renderListTab(bodyEl, customers, flights);
            });
        });

        bodyEl.querySelectorAll('[data-change-flight]').forEach(btn => {
            btn.addEventListener('click', () => {
                const resId = Number(btn.getAttribute('data-change-flight'));
                const currFlightId = Number(btn.getAttribute('data-flight-id'));
                openChangeFlightModal(resId, currFlightId, flights, () => renderListTab(bodyEl, customers, flights));
            });
        });

        bodyEl.querySelectorAll('[data-add-pax]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const resId = Number(btn.getAttribute('data-add-pax'));
                const flightId = Number(btn.getAttribute('data-flight-id'));
                await openSeatModal('Agregar Pasajero a Reserva #' + resId, flightId, null, async (formValues) => {
                    await api.post(`/api/reservations/${resId}/passengers`, formValues);
                    toast.success('Pasajero agregado a la reserva.');
                    renderListTab(bodyEl, customers, flights);
                });
            });
        });

        bodyEl.querySelectorAll('[data-change-seat]').forEach(btn => {
            btn.addEventListener('click', async () => {
                const resId = Number(btn.getAttribute('data-change-seat'));
                const detailId = Number(btn.getAttribute('data-detail-id'));
                const flightId = Number(btn.getAttribute('data-flight-id'));
                const existing = {
                    fullName: btn.getAttribute('data-pax-name'),
                    document: btn.getAttribute('data-pax-doc'),
                    rowNumber: btn.getAttribute('data-seat-row'),
                    seatNumber: Number(btn.getAttribute('data-seat-col'))
                };
                await openSeatModal(`Editar Pasajero / Silla · ${existing.fullName}`, flightId, existing, async (formValues) => {
                    await api.put(`/api/reservations/${resId}/passengers/${detailId}`, formValues);
                    toast.success('Pasajero y silla actualizados.');
                    renderListTab(bodyEl, customers, flights);
                });
            });
        });
    }

    function openChangeFlightModal(resId, currentFlightId, flights, onSaved) {
        modal.open(`Cambiar Vuelo · Reserva #${resId}`, `
            <form id="change-flight-form">
                <div class="form-group">
                    <label>Nuevo Vuelo *</label>
                    <select class="form-control" name="flightId" required>
                        ${flights.map(f => {
                            const info = formatFlightRoute(f);
                            return `<option value="${f.flightId}" ${f.flightId === currentFlightId ? 'selected' : ''}>${escapeHtml(info.title)} · ${escapeHtml(info.route)}</option>`;
                        }).join('')}
                    </select>
                </div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" id="cf-cancel">Cancelar</button>
                    <button type="submit" class="btn btn-primary">Confirmar Cambio</button>
                </div>
            </form>
        `, (root) => {
            root.querySelector('#cf-cancel').addEventListener('click', modal.close);
            root.querySelector('#change-flight-form').addEventListener('submit', async (e) => {
                e.preventDefault();
                const fd = new FormData(e.target);
                await api.put(`/api/reservations/${resId}/flight`, { flightId: Number(fd.get('flightId')) });
                toast.success('Vuelo de la reserva actualizado.');
                modal.close();
                onSaved();
            });
        });
    }

    async function openSeatModal(title, flightId, existingPax, onSubmit) {
        const occupiedSeats = await api.get(`/api/flights/${encodeURIComponent(flightId)}/seats`);
        const occupiedKeys = new Set(occupiedSeats.map(s => seatKey(s.row ?? s.rowNumber, s.column ?? s.seatNumber)));
        let selectedRow = existingPax ? existingPax.rowNumber : '1';
        let selectedCol = existingPax ? existingPax.seatNumber : 1;

        modal.open(title, `
            <form id="pax-seat-form">
                <div class="form-grid">
                    <div class="form-group">
                        <label>Nombre completo *</label>
                        <input class="form-control" name="fullName" required maxlength="50" value="${escapeHtml(existingPax?.fullName || '')}">
                    </div>
                    <div class="form-group">
                        <label>Documento *</label>
                        <input class="form-control" name="document" required maxlength="20" value="${escapeHtml(existingPax?.document || '')}">
                    </div>
                    <div class="form-group">
                        <label>Fila (1–30 o A–Z) *</label>
                        <input class="form-control" id="modal-row-input" name="rowNumber" required maxlength="2" value="${escapeHtml(selectedRow)}">
                    </div>
                    <div class="form-group">
                        <label>Columna (1=A .. 6=F, máx 77) *</label>
                        <input class="form-control" id="modal-col-input" name="seatNumber" type="number" min="1" max="77" required value="${selectedCol}">
                    </div>
                </div>
                <div id="modal-cabin-wrap" style="margin-top: 14px;"></div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" id="ps-cancel">Cancelar</button>
                    <button type="submit" class="btn btn-primary">Guardar</button>
                </div>
            </form>
        `, (root) => {
            const rowInput = root.querySelector('#modal-row-input');
            const colInput = root.querySelector('#modal-col-input');
            const cabinWrap = root.querySelector('#modal-cabin-wrap');

            function refreshModalCabin() {
                const selectedSet = new Set([seatKey(rowInput.value, colInput.value)]);
                cabinWrap.innerHTML = renderCabinMapHtml(occupiedKeys, selectedSet);
                cabinWrap.querySelectorAll('.cabin-seat:not([disabled])').forEach(seatBtn => {
                    seatBtn.addEventListener('click', () => {
                        rowInput.value = seatBtn.getAttribute('data-seat-row');
                        colInput.value = seatBtn.getAttribute('data-seat-col');
                        refreshModalCabin();
                    });
                });
            }

            refreshModalCabin();
            rowInput.addEventListener('input', refreshModalCabin);
            colInput.addEventListener('input', refreshModalCabin);
            root.querySelector('#ps-cancel').addEventListener('click', modal.close);

            root.querySelector('#pax-seat-form').addEventListener('submit', async (e) => {
                e.preventDefault();
                const fd = new FormData(e.target);
                await onSubmit({
                    fullName: fd.get('fullName'),
                    document: fd.get('document'),
                    rowNumber: fd.get('rowNumber'),
                    seatNumber: Number(fd.get('seatNumber'))
                });
                modal.close();
            });
        });
    }

    // ------------------------------------------------------------------
    // Tab 2: Asistente de Nueva Reserva + Mapa Interactivo de Cabina
    // ------------------------------------------------------------------

    async function renderNewReservationTab(bodyEl, customers, flights) {
        if (customers.length === 0 || flights.length === 0) {
            bodyEl.innerHTML = `
                <div class="card"><div class="card-body empty-state">
                    Necesitas al menos 1 cliente y 1 vuelo registrados para crear una reserva.
                </div></div>
            `;
            return;
        }

        let selectedFlightId = window.AppContext.selectedFlightId
            && flights.some(f => f.flightId === Number(window.AppContext.selectedFlightId))
            ? Number(window.AppContext.selectedFlightId)
            : flights[0].flightId;
        window.AppContext.selectedFlightId = null;

        let passengers = [{ fullName: '', document: '', rowNumber: '1', seatNumber: 1 }];
        let activePaxIndex = 0;
        let occupiedKeys = new Set();

        async function loadOccupiedSeats(flightId) {
            const seats = await api.get(`/api/flights/${encodeURIComponent(flightId)}/seats`);
            occupiedKeys = new Set(seats.map(s => seatKey(s.row ?? s.rowNumber, s.column ?? s.seatNumber)));
        }

        await loadOccupiedSeats(selectedFlightId);

        bodyEl.innerHTML = `
            <div class="wizard-grid">
                <section class="card">
                    <div class="card-header">
                        <h2 class="card-title">Asistente de Reserva (1 a 9 Pasajeros)</h2>
                    </div>
                    <div class="card-body">
                        <form id="new-res-form">
                            <div class="form-grid">
                                <div class="form-group">
                                    <label>Cliente Titular *</label>
                                    <select class="form-control" id="new-res-customer" required>
                                        ${customers.map(c => `<option value="${escapeHtml(c.id)}">${escapeHtml(c.fullName)} (${escapeHtml(c.id)})</option>`).join('')}
                                    </select>
                                </div>
                                <div class="form-group">
                                    <label>Vuelo *</label>
                                    <select class="form-control" id="new-res-flight" required>
                                        ${flights.map(f => {
                                            const info = formatFlightRoute(f);
                                            return `<option value="${f.flightId}" ${f.flightId === selectedFlightId ? 'selected' : ''}>${escapeHtml(info.title)} · ${escapeHtml(info.route)}</option>`;
                                        }).join('')}
                                    </select>
                                </div>
                            </div>

                            <div class="card-header" style="padding: 14px 0 8px; border-bottom: none;">
                                <strong id="pax-counter-label">Pasajeros (1/9)</strong>
                                <button type="button" id="btn-add-pax-row" class="btn btn-secondary btn-sm">+ Agregar pasajero</button>
                            </div>
                            <p class="kpi-hint" style="margin-bottom: 10px;">
                                Activa un pasajero y haz clic en una silla libre del fuselaje (filas 1–30, A–F) para asignársela.
                            </p>

                            <div id="pax-rows-wrap" class="pax-rows-wrap"></div>

                            <div class="form-actions">
                                <button type="submit" class="btn btn-primary">Confirmar Reserva</button>
                            </div>
                        </form>
                    </div>
                </section>

                <section class="card">
                    <div class="card-header">
                        <h2 class="card-title">Cabina del Avión · Sillas en Vivo</h2>
                        <span id="cabin-flight-badge" class="badge badge-sky">Vuelo #${selectedFlightId}</span>
                    </div>
                    <div id="cabin-map-container" class="card-body"></div>
                </section>
            </div>
        `;

        const paxWrap = bodyEl.querySelector('#pax-rows-wrap');
        const cabinWrap = bodyEl.querySelector('#cabin-map-container');
        const flightSelect = bodyEl.querySelector('#new-res-flight');
        const cabinBadge = bodyEl.querySelector('#cabin-flight-badge');
        const counterLabel = bodyEl.querySelector('#pax-counter-label');

        function renderPaxRows() {
            counterLabel.textContent = `Pasajeros (${passengers.length}/9)`;
            paxWrap.innerHTML = passengers.map((p, idx) => `
                <div class="pax-row ${idx === activePaxIndex ? 'active-pax' : ''}" data-pax-idx="${idx}">
                    <div class="pax-row-head">
                        <span>
                            <strong>Pasajero #${idx + 1}</strong>
                            <span class="badge badge-success">${escapeHtml(formatSeatLabel(p.rowNumber, p.seatNumber))}</span>
                            ${idx === activePaxIndex ? '<span class="badge badge-sky">Activo en mapa</span>' : ''}
                        </span>
                        ${passengers.length > 1 ? `<button type="button" class="btn btn-danger btn-sm" data-del-row="${idx}">Quitar</button>` : ''}
                    </div>
                    <div class="form-grid">
                        <div class="form-group">
                            <label>Nombre completo *</label>
                            <input class="form-control" data-field="fullName" data-idx="${idx}" required maxlength="50" value="${escapeHtml(p.fullName)}" placeholder="Nombre y apellido">
                        </div>
                        <div class="form-group">
                            <label>Documento *</label>
                            <input class="form-control" data-field="document" data-idx="${idx}" required maxlength="20" value="${escapeHtml(p.document)}" placeholder="Solo letras y números">
                        </div>
                        <div class="form-group">
                            <label>Fila *</label>
                            <input class="form-control" data-field="rowNumber" data-idx="${idx}" required maxlength="2" value="${escapeHtml(p.rowNumber)}">
                        </div>
                        <div class="form-group">
                            <label>Columna (1=A .. 6=F) *</label>
                            <input class="form-control" data-field="seatNumber" data-idx="${idx}" type="number" min="1" max="77" required value="${p.seatNumber}">
                        </div>
                    </div>
                </div>
            `).join('');

            paxWrap.querySelectorAll('.pax-row').forEach(rowEl => {
                rowEl.addEventListener('click', () => {
                    const idx = Number(rowEl.getAttribute('data-pax-idx'));
                    if (activePaxIndex !== idx) {
                        activePaxIndex = idx;
                        renderPaxRows();
                    }
                });
            });

            paxWrap.querySelectorAll('[data-field]').forEach(inputEl => {
                inputEl.addEventListener('input', () => {
                    const idx = Number(inputEl.getAttribute('data-idx'));
                    const field = inputEl.getAttribute('data-field');
                    passengers[idx][field] = field === 'seatNumber' ? Number(inputEl.value) : inputEl.value;
                    if (field === 'rowNumber' || field === 'seatNumber') {
                        renderCabin();
                    }
                });
            });

            paxWrap.querySelectorAll('[data-del-row]').forEach(delBtn => {
                delBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const idx = Number(delBtn.getAttribute('data-del-row'));
                    passengers.splice(idx, 1);
                    if (activePaxIndex >= passengers.length) activePaxIndex = passengers.length - 1;
                    renderPaxRows();
                    renderCabin();
                });
            });
        }

        function renderCabin() {
            const selectedSet = new Set(passengers.map(p => seatKey(p.rowNumber, p.seatNumber)));
            cabinWrap.innerHTML = renderCabinMapHtml(occupiedKeys, selectedSet);
            cabinWrap.querySelectorAll('.cabin-seat:not([disabled])').forEach(btn => {
                btn.addEventListener('click', () => {
                    const r = btn.getAttribute('data-seat-row');
                    const c = Number(btn.getAttribute('data-seat-col'));
                    passengers[activePaxIndex].rowNumber = r;
                    passengers[activePaxIndex].seatNumber = c;
                    renderPaxRows();
                    renderCabin();
                });
            });
        }

        renderPaxRows();
        renderCabin();

        bodyEl.querySelector('#btn-add-pax-row').addEventListener('click', () => {
            if (passengers.length >= 9) {
                toast.error('Una reserva no puede tener más de 9 pasajeros.');
                return;
            }
            passengers.push({ fullName: '', document: '', rowNumber: '1', seatNumber: passengers.length + 1 });
            activePaxIndex = passengers.length - 1;
            renderPaxRows();
            renderCabin();
        });

        flightSelect.addEventListener('change', async () => {
            selectedFlightId = Number(flightSelect.value);
            cabinBadge.textContent = `Vuelo #${selectedFlightId}`;
            await loadOccupiedSeats(selectedFlightId);
            renderCabin();
        });

        bodyEl.querySelector('#new-res-form').addEventListener('submit', async (e) => {
            e.preventDefault();
            const customerId = bodyEl.querySelector('#new-res-customer').value;
            const created = await api.post('/api/reservations', {
                customerId,
                flightId: selectedFlightId,
                passengers
            });
            toast.success(`¡Reserva #${created.id} registrada con éxito!`);
            activeTab = 'list';
            window.AppViews.reservations.render(document.getElementById('view-container'));
        });
    }

    // ------------------------------------------------------------------
    // Tab 3: Manifiesto de Vuelo y Vista Imprimible
    // ------------------------------------------------------------------

    async function renderManifestTab(bodyEl, flights) {
        if (flights.length === 0) {
            bodyEl.innerHTML = `<div class="card"><div class="card-body empty-state">No hay vuelos registrados.</div></div>`;
            return;
        }

        bodyEl.innerHTML = `
            <div class="card">
                <div class="card-header no-print">
                    <div class="toolbar">
                        <select id="manifest-flight-select" class="form-control">
                            ${flights.map(f => {
                                const info = formatFlightRoute(f);
                                return `<option value="${f.flightId}">${escapeHtml(info.title)} · ${escapeHtml(info.route)}</option>`;
                            }).join('')}
                        </select>
                        <button id="btn-load-manifest" class="btn btn-primary">Ver Manifiesto</button>
                    </div>
                    <button id="btn-print-manifest" class="btn btn-secondary">🖨️ Imprimir / Guardar PDF</button>
                </div>
                <div id="manifest-output" class="card-body"></div>
            </div>
        `;

        const selectEl = bodyEl.querySelector('#manifest-flight-select');
        const outEl = bodyEl.querySelector('#manifest-output');

        async function loadManifest() {
            const selectedFlightId = Number(selectEl.value);
            const flight = flights.find(f => f.flightId === selectedFlightId);
            const info = formatFlightRoute(flight);
            const entries = await api.get(`/api/flights/${encodeURIComponent(selectedFlightId)}/manifest`);

            outEl.innerHTML = `
                <div class="manifest-header">
                    <h3>MANIFIESTO OFICIAL DE PASAJEROS · ${escapeHtml(info.title)}</h3>
                    <p>${escapeHtml(info.route)} ${info.schedule ? '· ' + escapeHtml(info.schedule) : ''} · Total confirmados: <strong>${entries.length}</strong></p>
                </div>
                <div class="table-wrap" style="margin-top: 12px;">
                    ${entries.length === 0 ? `<div class="empty-state">No hay pasajeros confirmados en este vuelo.</div>` : `
                        <table>
                            <thead>
                                <tr>
                                    <th>Orden</th>
                                    <th>Asiento</th>
                                    <th>#Reserva</th>
                                    <th>Pasajero</th>
                                    <th>Documento</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${entries.map((entry, idx) => `
                                    <tr>
                                        <td>${idx + 1}</td>
                                        <td><span class="badge badge-success">${escapeHtml(formatSeatLabel(entry.seat?.rowNumber, entry.seat?.seatNumber))}</span></td>
                                        <td>#${entry.reservationId}</td>
                                        <td><strong>${escapeHtml(entry.passengerFullName)}</strong></td>
                                        <td>${escapeHtml(entry.passengerDocument)}</td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    `}
                </div>
            `;
        }

        await loadManifest();
        bodyEl.querySelector('#btn-load-manifest').addEventListener('click', loadManifest);
        selectEl.addEventListener('change', loadManifest);
        bodyEl.querySelector('#btn-print-manifest').addEventListener('click', () => window.print());
    }

    // ------------------------------------------------------------------
    // Vista Principal del Módulo 5
    // ------------------------------------------------------------------

    window.AppViews.reservations = {
        title: 'Módulo 5 · Reservas y Pasajeros',
        subtitle: 'Mapa interactivo de cabina, Boarding Pass y manifiesto imprimible',
        async render(container) {
            if (window.AppContext.selectedFlightId) {
                activeTab = 'new';
            }

            const [customers, flights] = await Promise.all([
                api.get('/api/customers'),
                api.get('/api/flights')
            ]);

            container.innerHTML = `
                <div class="subtabs no-print">
                    <button class="subtab ${activeTab === 'list' ? 'active' : ''}" data-tab="list">🎫 Boarding Pass / Reservas</button>
                    <button class="subtab ${activeTab === 'new' ? 'active' : ''}" data-tab="new">✈️ Nueva Reserva & Cabina</button>
                    <button class="subtab ${activeTab === 'manifest' ? 'active' : ''}" data-tab="manifest">📋 Manifiesto de Vuelo</button>
                </div>
                <div id="res-tab-body"></div>
            `;

            const bodyEl = container.querySelector('#res-tab-body');

            container.querySelectorAll('[data-tab]').forEach(btn => {
                btn.addEventListener('click', () => {
                    activeTab = btn.getAttribute('data-tab');
                    window.AppViews.reservations.render(container);
                });
            });

            if (activeTab === 'list') await renderListTab(bodyEl, customers, flights);
            else if (activeTab === 'new') await renderNewReservationTab(bodyEl, customers, flights);
            else await renderManifestTab(bodyEl, flights);
        }
    };
})();
