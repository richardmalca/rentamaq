const statusText = document.getElementById('status');
const cajamarcaCenter = [-7.157, -78.5175];
const map = L.map('map', {
  zoomControl: false,
  dragging: false,
  scrollWheelZoom: false,
  minZoom: 12,
  maxZoom: 15
}).setView(cajamarcaCenter, 13);

new ResizeObserver(() => map.invalidateSize()).observe(document.getElementById('map'));

map.attributionControl.setPrefix(false);
L.control.zoom({ position: 'topright', zoomInTitle: 'Acercar', zoomOutTitle: 'Alejar' }).addTo(map);
L.circle(cajamarcaCenter, {
  radius: 1250,
  color: '#5d662d',
  weight: 2,
  fillColor: '#d9bd4a',
  fillOpacity: 0.23,
  interactive: false
}).addTo(map);

const loadingTimeout = window.setTimeout(() => {
  statusText.textContent = 'Mapa no disponible. Revisa tu conexión.';
}, 8000);

L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
  attribution: '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
}).on('tileload', () => {
  window.clearTimeout(loadingTimeout);
  statusText.hidden = true;
}).addTo(map);
