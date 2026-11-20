(() => {
    window.AppViews = window.AppViews || {};

    function animateCounter(el, target) {
        const end = Number(target) || 0;
        const duration = 350;
        const startTime = performance.now();

        function step(now) {
            const progress = Math.min((now - startTime) / duration, 1);
            el.textContent = Math.round(end * progress);
            if (progress < 1) {
                requestAnimationFrame(step);
            } else {
                el.textContent = end;
            }
        }
        requestAnimationFrame(step);
    }

    function formatAirport(airport) {
        if (!airport) return '<span class="badge badge-warning">Sin asignar</span>';
        const city = airport.city ? ` (${escapeHtml(airport.city.name)})` : '';
        return `${escapeHtml(airport.name)}${city}`;
    }

    window.AppViews.dashboard = {
        title: 'Dashboard Ejecutivo',
        subtitle: 'Resumen operativo en tiempo real desde SQL Server',
        async render(container) {
            container.innerHTML = `<div class="card"><div class="card-body">Cargando métricas en vivo...</div></div>`;
            const data = await api.get('/api/dashboard');

            const flights = Array.isArray(data.upcomingFlights) ? data.upcomingFlights : [];

            container.innerHTML = `
                <section class="kpi-grid">
                    <div class="kpi-card">
                        <div class="kpi-label"><span>Clientes</span><span>👥</span></div>
                        <div class="kpi-value" data-kpi="${data.totalCustomers ?? 0}">0</div>
                        <div class="kpi-hint">${data.totalCountries ?? 0} países · ${data.totalDocumentTypes ?? 0} tipos doc.</div>
                    </div>
                    <div class="kpi-card">
                        <div class="kpi-label"><span>Aeropuertos</span><span>📍</span></div>
                        <div class="kpi-value" data-kpi="${data.totalAirports ?? 0}">0</div>
                        <div class="kpi-hint">${data.totalCities ?? 0} ciudades activas</div>
                    </div>
                    <div class="kpi-card">
                        <div class="kpi-label"><span>Aerolíneas</span><span>🏢</span></div>
                        <div class="kpi-value" data-kpi="${data.totalAirlines ?? 0}">0</div>
                        <div class="kpi-hint">Operadoras registradas</div>
                    </div>
                    <div class="kpi-card">
                        <div class="kpi-label"><span>Vuelos Programados</span><span>🛫</span></div>
                        <div class="kpi-value" data-kpi="${data.totalFlights ?? 0}">0</div>
                        <div class="kpi-hint">Itinerario en SQL Server</div>
                    </div>
                    <div class="kpi-card">
                        <div class="kpi-label"><span>Reservas Activas</span><span>🎫</span></div>
                        <div class="kpi-value" data-kpi="${data.totalReservations ?? 0}">0</div>
                        <div class="kpi-hint">Con pasajeros confirmados</div>
                    </div>
                </section>

                <section class="card">
                    <div class="card-header">
                        <h2 class="card-title">Accesos Rápidos</h2>
                    </div>
                    <div class="card-body quick-actions">
                        <button class="btn btn-primary" data-goto="customers">👥 Ir a Clientes</button>
                        <button class="btn btn-primary" data-goto="flights">🛫 Programar Vuelo</button>
                        <button class="btn btn-primary" data-goto="reservations">🎫 Nueva Reserva</button>
                        <button class="btn btn-secondary" data-goto="locations">📍 Ciudades y Aeropuertos</button>
                        <button class="btn btn-secondary" data-goto="airlines">🏢 Aerolíneas</button>
                    </div>
                </section>

                <section class="card">
                    <div class="card-header">
                        <h2 class="card-title">Próximos Vuelos y Ocupación en Vivo</h2>
                        <span class="badge badge-sky">${flights.length} vuelo(s)</span>
                    </div>
                    <div class="table-wrap">
                        ${flights.length === 0 ? `<div class="empty-state">No hay vuelos registrados todavía.</div>` : `
                            <table>
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Código</th>
                                        <th>Aerolínea</th>
                                        <th>Origen</th>
                                        <th>Destino</th>
                                        <th>Salida</th>
                                        <th>Llegada</th>
                                        <th>Ocupación</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${flights.map(f => {
                                        const occ = Number(f.occupiedSeatsCount ?? 0);
                                        const badgeCls = occ > 0 ? 'badge-success' : 'badge-sky';
                                        return `
                                            <tr>
                                                <td>#${f.flightId}</td>
                                                <td><strong>${escapeHtml(f.flightName || '-')}</strong></td>
                                                <td>${escapeHtml(f.airlineName || 'Sin aerolínea')}</td>
                                                <td>${formatAirport(f.departure)}</td>
                                                <td>${formatAirport(f.arrival)}</td>
                                                <td>${escapeHtml(f.dateDeparture || '')} ${escapeHtml(f.hourDeparture || '')}</td>
                                                <td>${escapeHtml(f.dateArrival || '')} ${escapeHtml(f.hourArrival || '')}</td>
                                                <td><span class="badge ${badgeCls}">${occ} pasajero(s)</span></td>
                                            </tr>
                                        `;
                                    }).join('')}
                                </tbody>
                            </table>
                        `}
                    </div>
                </section>
            `;

            container.querySelectorAll('[data-kpi]').forEach(el => {
                animateCounter(el, el.getAttribute('data-kpi'));
            });

            container.querySelectorAll('[data-goto]').forEach(btn => {
                btn.addEventListener('click', () => {
                    window.navigateTo(btn.getAttribute('data-goto'));
                });
            });
        }
    };
})();
