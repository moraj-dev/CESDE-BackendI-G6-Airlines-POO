(() => {
    window.AppViews = window.AppViews || {};
    window.AppContext = window.AppContext || {};

    const TOTAL_AIRCRAFT_CAPACITY = 180;
    const MONTHS = [
        'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
        'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
    ];

    function renderAirportPoint(airport, fallbackLabel) {
        if (!airport) {
            return `
                <div class="route-point">
                    <div class="route-city">Sin asignar</div>
                    <div class="route-airport">${escapeHtml(fallbackLabel)}</div>
                </div>
            `;
        }
        return `
            <div class="route-point">
                <div class="route-city">${escapeHtml(airport.city?.name || 'Ciudad')}</div>
                <div class="route-airport">${escapeHtml(airport.name)}</div>
            </div>
        `;
    }

    function renderFlightCard(f) {
        const occupied = Number(f.occupiedSeatsCount ?? 0);
        const pct = Math.min(100, Math.round((occupied / TOTAL_AIRCRAFT_CAPACITY) * 100));

        return `
            <article class="flight-card">
                <div class="flight-card-top">
                    <div>
                        <span class="flight-code">${escapeHtml(f.flightName || 'SIN CÓDIGO')}</span>
                        <span class="flight-id-tag">#${f.flightId}</span>
                    </div>
                    <span class="badge badge-sky">${escapeHtml(f.airlineName || 'Sin aerolínea')}</span>
                </div>

                <div class="flight-route">
                    ${renderAirportPoint(f.departure, 'Aeropuerto origen')}
                    <div class="route-connector" aria-hidden="true">
                        <span class="route-line"></span>
                        <span class="route-plane">✈</span>
                        <span class="route-line"></span>
                    </div>
                    ${renderAirportPoint(f.arrival, 'Aeropuerto destino')}
                </div>

                <div class="flight-times">
                    <div>
                        <span class="time-label">Salida</span>
                        <strong>${escapeHtml(f.dateDeparture || '-')} · ${escapeHtml(f.hourDeparture || '-')}</strong>
                    </div>
                    <div>
                        <span class="time-label">Llegada</span>
                        <strong>${escapeHtml(f.dateArrival || '-')} · ${escapeHtml(f.hourArrival || '-')}</strong>
                    </div>
                </div>

                <div class="occupancy-box">
                    <div class="occupancy-head">
                        <span>Ocupación de sillas</span>
                        <strong>${occupied} / ${TOTAL_AIRCRAFT_CAPACITY} (${pct}%)</strong>
                    </div>
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: ${pct}%"></div>
                    </div>
                </div>

                <div class="flight-card-actions">
                    <button class="btn btn-primary btn-sm" data-reserve-flight="${f.flightId}">
                        🪑 Ver Mapa / Reservar
                    </button>
                    <button class="btn btn-danger btn-sm" data-del-flight="${f.flightId}">
                        Eliminar vuelo
                    </button>
                </div>
            </article>
        `;
    }

    function openScheduleModal(airports, airlines, onSaved) {
        if (airports.length < 2) {
            toast.error('Debes registrar al menos 2 aeropuertos en Ubicaciones antes de programar un vuelo.');
            return;
        }
        if (airlines.length === 0) {
            toast.error('Debes registrar al menos 1 aerolínea antes de programar un vuelo.');
            return;
        }

        const today = new Date().toISOString().slice(0, 10);

        modal.open('Programar Nuevo Vuelo', `
            <form id="flight-form">
                <div class="form-grid">
                    <div class="form-group">
                        <label>Código del Vuelo (máx. 8) *</label>
                        <input class="form-control" name="flightName" required maxlength="8" placeholder="Ej. AV-101">
                    </div>
                    <div class="form-group">
                        <label>Aerolínea *</label>
                        <select class="form-control" name="airlineId" required>
                            ${airlines.map(a => `<option value="${a.id}">[${a.id}] ${escapeHtml(a.name)}</option>`).join('')}
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Aeropuerto de Salida *</label>
                        <select class="form-control" name="departureId" required>
                            ${airports.map(ap => `<option value="${escapeHtml(ap.id)}">${escapeHtml(ap.name)} (${escapeHtml(ap.city?.name || '')})</option>`).join('')}
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Aeropuerto de Llegada *</label>
                        <select class="form-control" name="arrivalId" required>
                            ${airports.map((ap, i) => `<option value="${escapeHtml(ap.id)}" ${i === 1 ? 'selected' : ''}>${escapeHtml(ap.name)} (${escapeHtml(ap.city?.name || '')})</option>`).join('')}
                        </select>
                    </div>
                    <div class="form-group">
                        <label>Fecha de Salida *</label>
                        <input class="form-control" type="date" name="dateDeparture" required value="${today}">
                    </div>
                    <div class="form-group">
                        <label>Hora de Salida *</label>
                        <input class="form-control" type="time" name="hourDeparture" required value="08:00">
                    </div>
                    <div class="form-group">
                        <label>Fecha de Llegada *</label>
                        <input class="form-control" type="date" name="dateArrival" required value="${today}">
                    </div>
                    <div class="form-group">
                        <label>Hora de Llegada *</label>
                        <input class="form-control" type="time" name="hourArrival" required value="09:30">
                    </div>
                </div>
                <div class="form-actions">
                    <button type="button" class="btn btn-secondary" id="fl-cancel">Cancelar</button>
                    <button type="submit" class="btn btn-primary">Programar Vuelo</button>
                </div>
            </form>
        `, (root) => {
            root.querySelector('#fl-cancel').addEventListener('click', modal.close);
            root.querySelector('#flight-form').addEventListener('submit', async (e) => {
                e.preventDefault();
                const fd = new FormData(e.target);
                const payload = {
                    flightName: fd.get('flightName'),
                    airlineId: Number(fd.get('airlineId')),
                    departureId: fd.get('departureId'),
                    arrivalId: fd.get('arrivalId'),
                    dateDeparture: fd.get('dateDeparture'),
                    hourDeparture: fd.get('hourDeparture'),
                    dateArrival: fd.get('dateArrival'),
                    hourArrival: fd.get('hourArrival')
                };
                await api.post('/api/flights', payload);
                toast.success('Vuelo programado correctamente.');
                modal.close();
                onSaved();
            });
        });
    }

    window.AppViews.flights = {
        title: 'Módulo 4 · Vuelos',
        subtitle: 'Itinerarios activos, programación y reporte mensual por aerolínea',
        async render(container) {
            const [flights, airports, airlines] = await Promise.all([
                api.get('/api/flights'),
                api.get('/api/airports'),
                api.get('/api/airlines')
            ]);

            const currentYear = new Date().getFullYear();
            const currentMonth = new Date().getMonth() + 1;

            container.innerHTML = `
                <section class="card">
                    <div class="card-header">
                        <div class="toolbar">
                            <h2 class="card-title">Itinerario de Vuelos</h2>
                            <span class="badge badge-sky">${flights.length} vuelo(s)</span>
                        </div>
                        <button id="btn-new-flight" class="btn btn-primary">+ Programar Vuelo</button>
                    </div>
                    <div class="card-body">
                        ${flights.length === 0 ? `<div class="empty-state">No hay vuelos programados todavía.</div>` : `
                            <div class="flight-grid">
                                ${flights.map(renderFlightCard).join('')}
                            </div>
                        `}
                    </div>
                </section>

                <section class="card">
                    <div class="card-header">
                        <h2 class="card-title">📊 Reporte Mensual de Vuelos por Aerolínea</h2>
                    </div>
                    <div class="card-body">
                        <form id="flight-report-form" class="toolbar">
                            <select class="form-control" name="airlineId" required>
                                ${airlines.map(a => `<option value="${a.id}">[${a.id}] ${escapeHtml(a.name)}</option>`).join('')}
                            </select>
                            <input class="form-control" name="year" type="number" min="2000" max="2100" value="${currentYear}" style="width: 110px;">
                            <select class="form-control" name="month">
                                ${MONTHS.map((m, idx) => `<option value="${idx + 1}" ${idx + 1 === currentMonth ? 'selected' : ''}>${idx + 1} - ${m}</option>`).join('')}
                            </select>
                            <button type="submit" class="btn btn-primary">Consultar Reporte</button>
                        </form>
                        <div id="flight-report-result" style="margin-top: 16px;">
                            <div class="empty-state">Selecciona aerolínea, año y mes para generar el reporte.</div>
                        </div>
                    </div>
                </section>
            `;

            container.querySelector('#btn-new-flight').addEventListener('click', () => {
                openScheduleModal(airports, airlines, () => window.AppViews.flights.render(container));
            });

            container.querySelectorAll('[data-reserve-flight]').forEach(btn => {
                btn.addEventListener('click', () => {
                    window.AppContext.selectedFlightId = Number(btn.getAttribute('data-reserve-flight'));
                    window.navigateTo('reservations');
                });
            });

            container.querySelectorAll('[data-del-flight]').forEach(btn => {
                btn.addEventListener('click', async () => {
                    const id = btn.getAttribute('data-del-flight');
                    if (!confirm(`¿Seguro que deseas eliminar el vuelo #${id}?`)) return;
                    await api.delete(`/api/flights/${encodeURIComponent(id)}`);
                    toast.success('Vuelo eliminado correctamente.');
                    window.AppViews.flights.render(container);
                });
            });

            container.querySelector('#flight-report-form').addEventListener('submit', async (e) => {
                e.preventDefault();
                if (airlines.length === 0) {
                    toast.error('No hay aerolíneas registradas para consultar.');
                    return;
                }
                const fd = new FormData(e.target);
                const airlineId = fd.get('airlineId');
                const year = fd.get('year');
                const month = fd.get('month');
                const resultBox = container.querySelector('#flight-report-result');

                const rows = await api.get(`/api/flights/report?airlineId=${encodeURIComponent(airlineId)}&year=${encodeURIComponent(year)}&month=${encodeURIComponent(month)}`);
                const airlineName = airlines.find(a => String(a.id) === String(airlineId))?.name || `#${airlineId}`;

                if (rows.length === 0) {
                    resultBox.innerHTML = `<div class="empty-state">No se encontraron vuelos de <strong>${escapeHtml(airlineName)}</strong> en ${escapeHtml(MONTHS[Number(month) - 1])} ${escapeHtml(year)}.</div>`;
                    return;
                }

                const totalPassengers = rows.reduce((sum, r) => sum + Number(r.occupiedSeatsCount ?? 0), 0);
                resultBox.innerHTML = `
                    <div class="toolbar" style="margin-bottom: 12px;">
                        <span class="badge badge-sky">Total vuelos: ${rows.length}</span>
                        <span class="badge badge-success">Total pasajeros: ${totalPassengers}</span>
                    </div>
                    <div class="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Código</th>
                                    <th>Origen</th>
                                    <th>Destino</th>
                                    <th>Salida</th>
                                    <th>Llegada</th>
                                    <th>Pasajeros</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${rows.map(r => `
                                    <tr>
                                        <td>#${r.flightId}</td>
                                        <td><strong>${escapeHtml(r.flightName)}</strong></td>
                                        <td>${escapeHtml(r.departure?.name || 'Sin origen')}</td>
                                        <td>${escapeHtml(r.arrival?.name || 'Sin destino')}</td>
                                        <td>${escapeHtml(r.dateDeparture)} ${escapeHtml(r.hourDeparture)}</td>
                                        <td>${escapeHtml(r.dateArrival)} ${escapeHtml(r.hourArrival)}</td>
                                        <td><span class="badge badge-sky">${r.occupiedSeatsCount ?? 0}</span></td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                `;
            });
        }
    };
})();
